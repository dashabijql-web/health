#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import shutil
import socket
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TESTS_DIR = ROOT / "tests"
SHOW = ROOT / "HealthShow"
DATA = ROOT / "HealthData"
TOOLS = ROOT / "tools"
FULL_STACK_RUNNER = SHOW / "tests/run-full-stack-local.py"
WSL_STACK = TOOLS / "health-wsl-stack.sh"
ENV_SCRIPT = TOOLS / "health-wsl-env.sh"


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def iso_now() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--profile", choices=["preflight", "smoke", "standard", "full", "dual-db", "ui-product", "nightly"], default="standard")
    parser.add_argument("--data-source", choices=["old", "new", "both", "auto"], default="auto")
    parser.add_argument("--allow-fix", action="store_true")
    parser.add_argument("--allow-competitor-research", action="store_true")
    parser.add_argument("--allow-openclaw-notify", action="store_true")
    return parser.parse_args()


def socket_open(port: int, timeout: float = 2.0) -> bool:
    sock = socket.socket()
    sock.settimeout(timeout)
    try:
        sock.connect(("127.0.0.1", port))
        return True
    except OSError:
        return False
    finally:
        sock.close()


def http_ok(url: str) -> bool:
    import urllib.request

    try:
        with urllib.request.urlopen(url, timeout=5) as response:
            return 200 <= response.status < 400
    except Exception:  # noqa: BLE001
        return False


def relative_to_run(path: Path, run_dir: Path) -> str:
    try:
        return str(path.resolve().relative_to(run_dir.resolve())).replace("\\", "/")
    except Exception:  # noqa: BLE001
        return str(path)


class HealthLoopRunner:
    def __init__(self, args: argparse.Namespace) -> None:
        self.args = args
        self.run_id = f"{datetime.now().strftime('%Y%m%d-%H%M%S')}-{args.profile}"
        self.run_dir = TESTS_DIR / "runs" / self.run_id
        self.logs_dir = self.run_dir / "logs"
        self.issues_dir = self.run_dir / "issues"
        self.artifacts_dir = self.run_dir / "artifacts"
        self.screenshots_dir = self.run_dir / "screenshots"
        self.videos_dir = self.run_dir / "videos"
        self.competitor_dir = self.run_dir / "competitor"
        self.patches_dir = self.run_dir / "patches"
        self.started_at = iso_now()
        self.command_index = 0
        self.commands: list[dict[str, Any]] = []
        self.issues: list[dict[str, Any]] = []
        for directory in (
            self.run_dir,
            self.logs_dir,
            self.issues_dir,
            self.artifacts_dir,
            self.screenshots_dir,
            self.videos_dir,
            self.competitor_dir,
            self.patches_dir,
        ):
            directory.mkdir(parents=True, exist_ok=True)

    def run_bash(self, command: str, cwd: Path, env: dict[str, str] | None = None) -> subprocess.CompletedProcess[str]:
        merged_env = os.environ.copy()
        if env:
            merged_env.update(env)
        return subprocess.run(
            ["/bin/bash", "-lc", command],
            cwd=cwd,
            env=merged_env,
            text=True,
            capture_output=True,
            check=False,
        )

    def write_git_status_log(self, label: str) -> Path:
        output = self.logs_dir / label
        lines: list[str] = []
        for name, repo in (("HealthShow", SHOW), ("HealthData", DATA)):
            lines.append(f"## {name}")
            lines.append(f"cwd: {repo}")
            completed = subprocess.run(["git", "status", "--short"], cwd=repo, text=True, capture_output=True, check=False)
            status = (completed.stdout or completed.stderr or "").strip()
            lines.extend(status.splitlines() if status else ["(clean)"])
            lines.append("")
        output.write_text("\n".join(lines) + "\n", encoding="utf-8")
        return output

    def copy_new_artifacts(self, name: str, since: datetime) -> list[str]:
        source_roots: list[Path] = []
        if name in {"audit-api", "audit-data-old", "audit-data-new", "audit-write"}:
            source_roots.append(SHOW / "tests/api/artifacts")
        if name in {"audit-auth", "audit-e2e"}:
            source_roots.append(SHOW / "tests/e2e/artifacts")
        if name in {"audit-pipeline", "audit-pipeline-warning"}:
            source_roots.append(SHOW / "tests/pipeline/artifacts")

        copied: list[str] = []
        for source_root in source_roots:
            if not source_root.exists():
                continue
            for directory in sorted((item for item in source_root.iterdir() if item.is_dir()), key=lambda item: item.stat().st_mtime):
                created = datetime.fromtimestamp(directory.stat().st_mtime, tz=timezone.utc)
                if created < since:
                    continue
                dest = self.artifacts_dir / f"{name}-{directory.name}"
                if dest.exists():
                    shutil.rmtree(dest)
                shutil.copytree(directory, dest)
                summary = None
                for candidate in (
                    "summary.json",
                    "auth-summary.json",
                    "data-density.json",
                    "warning-summary.json",
                    "summary.md",
                    "auth-summary.md",
                    "data-density.md",
                    "warning-summary.md",
                ):
                    candidate_path = dest / candidate
                    if candidate_path.exists():
                        summary = candidate_path
                        break
                copied.append(relative_to_run(summary or dest, self.run_dir))
        return copied

    def artifact_result(self, artifact_paths: list[str]) -> str:
        for relative in artifact_paths:
            path = self.run_dir / relative
            if path.suffix.lower() != ".json" or not path.exists():
                continue
            try:
                payload = json.loads(path.read_text(encoding="utf-8"))
            except Exception:  # noqa: BLE001
                continue
            if isinstance(payload, dict) and "checks" in payload:
                checks = payload.get("checks") or []
                warnings = payload.get("warnings") or []
                failed = [item for item in checks if item.get("status") != "passed"]
                return f"{len(checks)} checks / {len(failed)} failed / {len(warnings)} warning"
            if isinstance(payload, dict) and "routeResults" in payload:
                routes = payload.get("routeResults") or []
                issues = payload.get("issues") or []
                return f"{len(routes)} routes / {len(issues)} issues"
            if isinstance(payload, dict) and "stages" in payload:
                stages = payload.get("stages") or []
                warnings = payload.get("warnings") or []
                failed = [item for item in stages if item.get("status") != "passed"]
                return f"{len(stages)} stages / {len(failed)} failed / {len(warnings)} warning"
        return ""

    def run_logged_command(
        self,
        name: str,
        command: str,
        working_directory: Path,
        timeout_seconds: int = 600,
        env: dict[str, str] | None = None,
        collect_artifacts: bool = False,
    ) -> dict[str, Any]:
        self.command_index += 1
        safe_name = "".join(ch if ch.isalnum() or ch in "._-" else "-" for ch in name).strip("-")
        prefix = f"{self.command_index:02d}-{safe_name}"
        stdout_path = self.logs_dir / f"{prefix}.stdout.log"
        stderr_path = self.logs_dir / f"{prefix}.stderr.log"
        combined_path = self.logs_dir / f"{prefix}.combined.log"
        started = utc_now()

        merged_env = os.environ.copy()
        if env:
            merged_env.update(env)
        proc = subprocess.Popen(
            ["/bin/bash", "-lc", command],
            cwd=working_directory,
            env=merged_env,
            text=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        timed_out = False
        try:
            stdout_text, stderr_text = proc.communicate(timeout=timeout_seconds)
        except subprocess.TimeoutExpired:
            timed_out = True
            proc.kill()
            stdout_text, stderr_text = proc.communicate()
        exit_code = 124 if timed_out else proc.returncode
        finished = utc_now()

        stdout_path.write_text(stdout_text or "", encoding="utf-8")
        stderr_path.write_text(stderr_text or "", encoding="utf-8")
        combined_lines = [
            f"name: {name}",
            f"command: {command}",
            f"cwd: {working_directory}",
            f"started_at: {started.astimezone().isoformat()}",
            f"finished_at: {finished.astimezone().isoformat()}",
            f"duration_seconds: {round((finished - started).total_seconds(), 3)}",
            f"exit_code: {exit_code}",
        ]
        if timed_out:
            combined_lines.append("timed_out: true")
        combined_lines.extend(["", "## stdout", stdout_text or "", "", "## stderr", stderr_text or ""])
        combined_path.write_text("\n".join(combined_lines), encoding="utf-8")

        artifact_paths = self.copy_new_artifacts(name, started) if collect_artifacts else []
        result = self.artifact_result(artifact_paths)
        if not result:
            result = "passed" if exit_code == 0 else f"failed with exit code {exit_code}"
            if timed_out:
                result = f"timed out after {timeout_seconds} seconds"
        entry = {
            "name": name,
            "command": command,
            "status": "passed" if exit_code == 0 else "failed",
            "exit_code": exit_code,
            "started_at": started.astimezone().isoformat(),
            "finished_at": finished.astimezone().isoformat(),
            "seconds": round((finished - started).total_seconds(), 3),
            "log": relative_to_run(combined_path, self.run_dir),
            "result": result,
            "artifacts": artifact_paths,
        }
        self.commands.append(entry)
        return entry

    def new_preflight_checks(self) -> list[dict[str, Any]]:
        checks = [
            {"name": "python3", "ok": shutil.which("python3") is not None, "required": True, "detail": shutil.which("python3") or "missing", "hint": "install Python 3"},
            {"name": "node", "ok": shutil.which("node") is not None, "required": True, "detail": shutil.which("node") or "missing", "hint": "install Node.js"},
            {"name": "npm", "ok": shutil.which("npm") is not None, "required": True, "detail": shutil.which("npm") or "missing", "hint": "install npm"},
            {"name": "sqlcmd", "ok": shutil.which("sqlcmd") is not None, "required": False, "detail": shutil.which("sqlcmd") or "missing", "hint": "install go-sqlcmd in $HOME/.local/bin"},
        ]
        maven_check = self.run_bash(f"source {ENV_SCRIPT} && \"$HEALTH_MAVEN_CMD\" -v | head -n 1", ROOT)
        checks.append(
            {
                "name": "maven",
                "ok": maven_check.returncode == 0,
                "required": True,
                "detail": (maven_check.stdout or maven_check.stderr or "").strip(),
                "hint": "fix tools/health-wsl-env.sh or local JDK/Maven install",
            }
        )

        requested_source = "old" if self.args.data_source == "auto" else self.args.data_source
        if self.args.profile in {"full", "dual-db"}:
            checks.extend(
                [
                    {"name": "sql-server", "ok": socket_open(1433), "required": True, "detail": "127.0.0.1:1433", "hint": "start SQL Server"},
                    {"name": "redis", "ok": socket_open(6379), "required": True, "detail": "127.0.0.1:6379", "hint": "start Redis"},
                    {"name": "data-source", "ok": True, "required": True, "detail": requested_source, "hint": ""},
                ]
            )
            return checks

        if self.args.profile != "preflight":
            checks.extend(
                [
                    {"name": "backend-http", "ok": http_ok("http://127.0.0.1:8080/health"), "required": True, "detail": "http://127.0.0.1:8080/health", "hint": "bash tools/health-wsl-stack.sh backend start"},
                    {"name": "frontend-http", "ok": http_ok("http://127.0.0.1:9528/"), "required": True, "detail": "http://127.0.0.1:9528/", "hint": "bash tools/health-wsl-stack.sh frontend start"},
                ]
            )
        if self.args.profile in {"standard", "nightly"}:
            checks.extend(
                [
                    {"name": "sql-server", "ok": socket_open(1433), "required": True, "detail": "127.0.0.1:1433", "hint": "start SQL Server"},
                    {"name": "redis", "ok": socket_open(6379), "required": True, "detail": "127.0.0.1:6379", "hint": "start Redis"},
                    {"name": "tcp-9000", "ok": socket_open(9000), "required": True, "detail": "127.0.0.1:9000", "hint": "start backend TCP listener"},
                ]
            )
        return checks

    def profile_commands(self) -> list[dict[str, Any]]:
        requested_source = "old" if self.args.data_source == "auto" else self.args.data_source
        command_source = requested_source if requested_source in {"old", "new"} else "old"
        sql_db = "health_new" if requested_source == "new" else "health"
        frontend_env = {
            "API_ARTIFACT_RETENTION": "50",
            "API_WRITE_ARTIFACT_RETENTION": "50",
            "E2E_ARTIFACT_RETENTION": "50",
            "PIPELINE_ARTIFACT_RETENTION": "50",
            "PIPELINE_DATA_SOURCE": command_source,
            "SQL_DB": sql_db,
            "API_DATA_SOURCE": command_source,
            "API_EXPECT_NON_EMPTY": "0" if command_source == "new" else "1",
        }
        if self.args.profile == "preflight":
            return []
        if self.args.profile == "smoke":
            return [
                {"name": "audit-api", "command": "npm run audit:api", "cwd": SHOW, "timeout": 360, "env": frontend_env, "artifacts": True},
            ]
        if self.args.profile == "standard":
            return [
                {"name": "backend-regression", "command": f"python3 {BACKEND_REGRESSION_SCRIPT}", "cwd": DATA, "timeout": 300, "env": {}, "artifacts": False},
                {"name": "test-integration", "command": f"npm run test:integration:{command_source}", "cwd": SHOW, "timeout": 600, "env": frontend_env, "artifacts": True},
                {"name": "audit-e2e", "command": "npm run audit:e2e", "cwd": SHOW, "timeout": 900, "env": frontend_env, "artifacts": True},
                {"name": "audit-pipeline", "command": "npm run audit:pipeline", "cwd": SHOW, "timeout": 420, "env": frontend_env, "artifacts": True},
            ]
        if self.args.profile == "full":
            return [
                {
                    "name": f"full-stack-{requested_source}",
                    "command": f"python3 {FULL_STACK_RUNNER} --data-source {requested_source}",
                    "cwd": SHOW,
                    "timeout": 7200,
                    "env": {},
                    "artifacts": False,
                }
            ]
        if self.args.profile == "dual-db":
            return [
                {
                    "name": "full-stack-both",
                    "command": f"python3 {FULL_STACK_RUNNER} --data-source both",
                    "cwd": SHOW,
                    "timeout": 10800,
                    "env": {},
                    "artifacts": False,
                }
            ]
        if self.args.profile == "ui-product":
            return [
                {"name": "audit-e2e", "command": "npm run audit:e2e", "cwd": SHOW, "timeout": 900, "env": frontend_env, "artifacts": True},
                {"name": "npm-build", "command": "npm run build", "cwd": SHOW, "timeout": 900, "env": frontend_env, "artifacts": False},
            ]
        if self.args.profile == "nightly":
            return [
                {"name": "audit-nightly", "command": "npm run audit:nightly", "cwd": SHOW, "timeout": 2400, "env": frontend_env, "artifacts": True}
            ]
        raise RuntimeError(f"unsupported profile: {self.args.profile}")

    def failure_meta(self, name: str) -> dict[str, Any]:
        if name == "preflight":
            return {"category": "environment", "severity": "blocker", "project": "Health", "priority": 1}
        if name == "audit-auth":
            return {"category": "auth", "severity": "high", "project": "HealthShow", "priority": 2}
        if "pipeline" in name or name == "audit-write":
            return {"category": "write-pipeline", "severity": "high", "project": "HealthShow", "priority": 3}
        if "backend" in name or "maven" in name:
            return {"category": "backend-regression", "severity": "high", "project": "HealthData", "priority": 4}
        if name == "audit-api":
            return {"category": "api", "severity": "high", "project": "HealthShow", "priority": 5}
        if name == "audit-e2e":
            return {"category": "e2e", "severity": "medium", "project": "HealthShow", "priority": 6}
        if name == "npm-build":
            return {"category": "build", "severity": "medium", "project": "HealthShow", "priority": 7}
        if "audit-data" in name:
            return {"category": "data-density", "severity": "medium", "project": "HealthShow", "priority": 8}
        return {"category": "regression", "severity": "medium", "project": "Health", "priority": 9}

    def new_issue_for_failure(self, failed_commands: list[dict[str, Any]]) -> None:
        if not failed_commands:
            return
        ranked = sorted(
            ((self.failure_meta(item["name"]) | {"command": item}) for item in failed_commands),
            key=lambda item: item["priority"],
        )
        selected = ranked[0]
        requested_source = "old" if self.args.data_source == "auto" else self.args.data_source
        issue_id = f"health-{selected['category']}-{datetime.now().strftime('%Y%m%d%H%M%S')}-001"
        issue = {
            "issue_id": issue_id,
            "project": selected["project"],
            "category": selected["category"],
            "severity": selected["severity"],
            "data_source": requested_source,
            "title": f"{selected['command']['name']} failed in {self.run_id}",
            "run_id": self.run_id,
            "failing_command": selected["command"]["command"],
            "exit_code": selected["command"]["exit_code"],
            "expected": "Runbook command passes with its existing assertions unchanged.",
            "actual": selected["command"]["result"],
            "failure_signature": f"{selected['command']['name']}:exit-{selected['command']['exit_code']}",
            "evidence": {
                "log_files": [selected["command"]["log"]],
                "artifacts": selected["command"]["artifacts"],
                "screenshots": [],
            },
            "suspected_files": [],
            "constraints": [
                "Only fix this issue",
                "Do not perform opportunistic refactors",
                "Do not delete or weaken test assertions",
                "Do not modify generated artifacts",
                "Check HealthShow and HealthData git status separately",
            ],
            "verification_commands": [selected["command"]["command"]],
            "status": "open",
        }
        (self.issues_dir / f"{issue_id}.json").write_text(json.dumps(issue, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        self.issues.append(issue)

    def environment_summary(self) -> dict[str, Any]:
        maven = self.run_bash(f"source {ENV_SCRIPT} && \"$HEALTH_MAVEN_CMD\" -v | head -n 1", ROOT)
        return {
            "frontend_url": "http://localhost:9528/",
            "backend_url": "http://localhost:8080/health",
            "runner": f"WSL Python {sys.version.split()[0]}",
            "maven": (maven.stdout or maven.stderr or "").strip(),
            "node": subprocess.run(["node", "-v"], text=True, capture_output=True, check=False).stdout.strip(),
            "npm": subprocess.run(["npm", "-v"], text=True, capture_output=True, check=False).stdout.strip(),
            "python": sys.version.split()[0],
        }

    def data_source_summary(self) -> dict[str, Any]:
        requested = "old" if self.args.data_source == "auto" else self.args.data_source
        sql_db = "health_new" if requested == "new" else "health"
        return {
            "requested": requested,
            "cookie": requested if requested in {"old", "new"} else "old",
            "request_header": requested if requested in {"old", "new"} else "old",
            "response_header": requested if requested in {"old", "new"} else "old",
            "redis_key": f"health:buffer:{requested if requested in {'old', 'new'} else 'old'}",
            "sql_db": sql_db,
            "pipeline_table": "",
        }

    def write_summaries(self, status: str, git_before_log: Path, git_after_log: Path) -> None:
        finished_at = iso_now()
        summary = {
            "run_id": self.run_id,
            "profile": self.args.profile,
            "status": status,
            "started_at": self.started_at,
            "finished_at": finished_at,
            "environment": self.environment_summary(),
            "data_source": self.data_source_summary(),
            "git_status": {
                "HealthShow_before": relative_to_run(git_before_log, self.run_dir),
                "HealthData_before": relative_to_run(git_before_log, self.run_dir),
                "HealthShow_after": relative_to_run(git_after_log, self.run_dir),
                "HealthData_after": relative_to_run(git_after_log, self.run_dir),
            },
            "commands": self.commands,
            "issues": self.issues,
            "competitor_findings": [],
            "fixes": [],
            "next_actions": [],
        }
        if status == "blocked":
            summary["next_actions"] = ["Restore missing local dependencies or services, then rerun the same profile."]
        elif status == "failed":
            summary["next_actions"] = ["Handle the generated highest-priority issue, then rerun its verification command."]
        else:
            summary["next_actions"] = ["No open issue was generated for this run."]

        summary_json_path = self.run_dir / "summary.json"
        summary_json_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        latest_path = TESTS_DIR / "latest-run.json"
        latest_path.write_text(
            json.dumps(
                {"run_id": self.run_id, "summary": str(summary_json_path), "status": status, "updated_at": finished_at},
                ensure_ascii=False,
                indent=2,
            )
            + "\n",
            encoding="utf-8",
        )

        lines = [
            f"# Health autonomous run: {self.run_id}",
            "",
            f"status: {status}",
            f"profile: {self.args.profile}",
            "",
            "## Data source",
        ]
        ds = summary["data_source"]
        lines.extend(
            [
                f"- requested: {ds['requested']}",
                f"- cookie/request/response: {ds['cookie']} / {ds['request_header']} / {ds['response_header']}",
                f"- redis/sql/table: {ds['redis_key']} / {ds['sql_db']} / {ds['pipeline_table']}",
                "",
                "## Commands",
                "| name | status | exit | seconds | result | log |",
                "| --- | --- | ---: | ---: | --- | --- |",
            ]
        )
        for cmd in self.commands:
            lines.append(
                f"| {cmd['name']} | {cmd['status']} | {cmd['exit_code']} | {cmd['seconds']} | "
                f"{str(cmd['result']).replace('|', '/')} | `{cmd['log']}` |"
            )
        lines.extend(
            [
                "",
                "## Git status logs",
                f"- before: `{relative_to_run(git_before_log, self.run_dir)}`",
                f"- after: `{relative_to_run(git_after_log, self.run_dir)}`",
                "",
                "## Issues",
            ]
        )
        if not self.issues:
            lines.append("No issue generated.")
        else:
            lines.extend(f"- {issue['issue_id']}: {issue['title']} [{issue['severity']}]" for issue in self.issues)
        lines.extend(["", "## Fixes", "No code fix was attempted by this runner.", "", "## Next actions"])
        lines.extend(f"- {item}" for item in summary["next_actions"])
        (self.run_dir / "summary.md").write_text("\n".join(lines) + "\n", encoding="utf-8")

        open_issues_path = TESTS_DIR / "open-issues.json"
        existing_open: list[dict[str, Any]] = []
        if open_issues_path.exists():
            try:
                parsed = json.loads(open_issues_path.read_text(encoding="utf-8"))
                if isinstance(parsed, dict) and "issues" in parsed:
                    existing_open = list(parsed["issues"])
                elif isinstance(parsed, list):
                    existing_open = parsed
            except Exception:  # noqa: BLE001
                existing_open = []
        new_open = [issue for issue in self.issues if issue.get("status") == "open"]
        merged = [issue for issue in existing_open if issue.get("run_id") != self.run_id] + new_open
        open_issues_path.write_text(
            json.dumps({"updated_at": finished_at, "issues": merged}, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )

    def execute(self) -> int:
        git_before = self.write_git_status_log("00-git-status-before.combined.log")
        checks = self.new_preflight_checks()
        blocked = [item for item in checks if item["required"] and not item["ok"]]
        preflight_log = self.logs_dir / "00-preflight.combined.log"
        preflight_lines = []
        for item in checks:
            status = "OK" if item["ok"] else "BLOCKED" if item["required"] else "WARN"
            preflight_lines.append(f"{status} {item['name']}: {item['detail']}")
            if not item["ok"] and item["hint"]:
                preflight_lines.append(f"hint: {item['hint']}")
        preflight_log.write_text("\n".join(preflight_lines) + "\n", encoding="utf-8")
        self.commands.append(
            {
                "name": "preflight",
                "command": "local environment checks",
                "status": "passed" if not blocked else "failed",
                "exit_code": 0 if not blocked else 2,
                "started_at": self.started_at,
                "finished_at": iso_now(),
                "seconds": 0,
                "log": relative_to_run(preflight_log, self.run_dir),
                "result": "passed" if not blocked else f"blocked: {', '.join(item['name'] for item in blocked)}",
                "artifacts": [],
            }
        )
        if blocked:
            self.new_issue_for_failure([self.commands[-1]])
            git_after = self.write_git_status_log("99-git-status-after.combined.log")
            self.write_summaries("blocked", git_before, git_after)
            print(f"run_id={self.run_id}")
            print("status=blocked")
            print(f"summary={self.run_dir / 'summary.md'}")
            return 2

        for spec in self.profile_commands():
            entry = self.run_logged_command(
                spec["name"],
                spec["command"],
                spec["cwd"],
                timeout_seconds=spec["timeout"],
                env=spec["env"],
                collect_artifacts=spec["artifacts"],
            )
            if entry["exit_code"] != 0:
                break

        failed_commands = [item for item in self.commands if item["status"] != "passed"]
        status = "failed" if failed_commands else "passed"
        if failed_commands:
            self.new_issue_for_failure(failed_commands)

        git_after = self.write_git_status_log("99-git-status-after.combined.log")
        self.write_summaries(status, git_before, git_after)
        print(f"run_id={self.run_id}")
        print(f"status={status}")
        print(f"summary={self.run_dir / 'summary.md'}")
        if self.issues:
            print(f"issue_count={len(self.issues)}")
        return 0 if status == "passed" else 1


def main() -> int:
    return HealthLoopRunner(parse_args()).execute()


if __name__ == "__main__":
    raise SystemExit(main())
