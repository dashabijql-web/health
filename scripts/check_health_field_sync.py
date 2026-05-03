#!/usr/bin/env python3
"""Check that HealthRecord metric columns stay wired through key backend layers.

This is a guardrail, not a full migration verifier. It catches the common case where
someone adds or renames a metric field in HealthRecord but forgets to thread the same
column through mapper INSERT SQL or the direct partition-table source builders.
"""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[1]

HEALTH_RECORD = ROOT / "src/main/java/com/xzkj/health/model/HealthRecord.java"
CHECK_TARGETS = {
    "mapper_insert": ROOT / "src/main/java/com/xzkj/health/mapper/HealthRecordMapper.java",
    "dashboard_health_source": ROOT / "src/main/java/com/xzkj/health/service/impl/DashboardServiceImpl.java",
    "heart_rate_table_source": ROOT / "src/main/java/com/xzkj/health/service/impl/HeartRateServiceImpl.java",
}
IGNORE_COLUMNS = {"id", "user_code", "record_time"}


@dataclass(frozen=True)
class MetricField:
    name: str
    column: str


def camel_to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def extract_metric_fields() -> list[MetricField]:
    text = HEALTH_RECORD.read_text(encoding="utf-8")
    pattern = re.compile(
        r'(?:@TableField\("(?P<column>[^"]+)"\)\s*)?private\s+(?:final\s+)?[\w<>]+\s+(?P<name>\w+)\s*;',
        re.MULTILINE,
    )

    fields: list[MetricField] = []
    seen: set[str] = set()

    for match in pattern.finditer(text):
        name = match.group("name")
        column = match.group("column") or camel_to_snake(name)
        if column in IGNORE_COLUMNS or name in seen:
            continue
        seen.add(name)
        fields.append(MetricField(name=name, column=column))

    return fields


def check_targets(fields: list[MetricField]) -> dict[str, list[str]]:
    missing_by_target: dict[str, list[str]] = {}
    for label, file_path in CHECK_TARGETS.items():
        text = file_path.read_text(encoding="utf-8")
        missing = [field.column for field in fields if field.column not in text]
        if missing:
            missing_by_target[label] = missing
    return missing_by_target


def build_report(fields: list[MetricField], missing_by_target: dict[str, list[str]]) -> str:
    lines = [
        "Health field sync report",
        "",
        "Tracked HealthRecord metric columns:",
        "  " + ", ".join(field.column for field in fields),
        "",
        "Auto-checked files:",
    ]

    for label, file_path in CHECK_TARGETS.items():
        if label in missing_by_target:
            lines.append(f"  - {label}: missing {', '.join(missing_by_target[label])}")
        else:
            lines.append(f"  - {label}: OK")

    lines.extend(
        [
            "",
            "Manual checks still required before merging a new health field:",
            "  - 月分表结构",
            "  - v_health_record / v_warning_record 视图定义",
            "  - sp_update_monthly_views",
            "  - Service 返回值与前端 API / 页面绑定",
            "  - 设备/模拟器 -> 日志 -> 数据库 -> API -> 页面 的端到端验证",
        ]
    )

    return "\n".join(lines)


def main() -> int:
    fields = extract_metric_fields()
    if not fields:
        print("No metric fields parsed from HealthRecord.java", file=sys.stderr)
        return 2

    missing_by_target = check_targets(fields)
    print(build_report(fields, missing_by_target))
    return 1 if missing_by_target else 0


if __name__ == "__main__":
    raise SystemExit(main())
