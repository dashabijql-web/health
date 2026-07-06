# Health Windows Codex Handoff Package With Database Backups

Generated: 2026-06-21 Asia/Shanghai

This package is intended for moving the Health monorepo to another Windows machine where Codex will continue development.

## Contents

- `source/health-source-cbf79974cb07.tar.gz`: source snapshot from git HEAD.
- `source/health-repo-cbf79974cb07.bundle`: full git bundle with repository history.
- `database/backups/health_full.bak`: SQL Server backup for old data source `health`.
- `database/backups/health_new_full.bak`: SQL Server backup for new data source `health_new`.
- `database/repo-sql/`: SQL files tracked in the repository.
- `database/external/scripthealth.original-utf16.sql`: external SQL Server script found at `/mnt/c/Users/j/Documents/scripthealth.sql`.
- `database/external/scripthealth.utf8.sql`: UTF-8 copy of the same external SQL script.
- `database/scripts/Backup-HealthDatabases.ps1`: PowerShell helper to regenerate database backups.
- `database/scripts/Restore-HealthDatabases-Windows.ps1`: PowerShell helper to restore both `.bak` files on Windows SQL Server 2022.
- `checksums/SHA256SUMS`: SHA-256 hashes for files in this package.
- `MANIFEST.md`: exact package facts.

## Runtime Facts

- Frontend: `http://localhost:9528/`
- Backend: `http://localhost:8080/health`
- Health check: `http://localhost:8080/health/actuator/health`
- Watch TCP: `9000`
- Admin login: `admin / admin123`
- Simulator: `HealthShow\watch_tcp_simulator_1000.py`
- Simulator default target: `127.0.0.1:9000`
- Simulator data source: old DB `health`

## Restore Source

```powershell
git clone .\source\health-repo-cbf79974cb07.bundle D:\Health
cd D:\Health
git checkout codex-frontend-beautification-20260522
```

Detailed guide inside source tree:

```text
docs\WINDOWS_CODEX_HANDOFF.md
```

## Restore Database

Copy the included backups to a target SQL Server-readable directory, for example:

```powershell
New-Item -ItemType Directory -Path C:\Users\Public\HealthHandoffDb -Force
Copy-Item .\database\backups\*.bak C:\Users\Public\HealthHandoffDb\
```

Then restore with:

```powershell
.\database\scripts\Restore-HealthDatabases-Windows.ps1 -SqlPassword '<target-sa-password>' -SqlServer 'localhost,11433'
```

If target SQL Server listens on `1433`, use `-SqlServer 'localhost,1433'` and set project `DB_PORT=1433`.
