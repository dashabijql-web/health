# Manifest

## Source Snapshot

- Repository: `/home/j/code/health`
- Branch: `codex-frontend-beautification-20260522`
- Commit: `cbf79974cb075e8d42acdfc2686b73a8e8763804`
- Git status at package time: see `source/git-status.txt`

## Database Backups Included

- Source SQL Server instance: `localhost,58135` / `R9000K3080\MSSQLSERVER2019`
- Authentication used to create backups: Windows integrated auth (`-E`)
- Backup staging path: `C:\Users\Public\HealthHandoffDb`
- Included files:
  - `database/backups/health_full.bak`
  - `database/backups/health_new_full.bak`

Backup command result summary:

- `health`: BACKUP DATABASE succeeded, 356698 pages.
- `health_new`: BACKUP DATABASE succeeded, 1858 pages.

## Runtime Facts Included In Docs

- Frontend: `http://localhost:9528/`
- Backend: `http://localhost:8080/health`
- Admin: `admin / admin123`
- Simulator script: `HealthShow\watch_tcp_simulator_1000.py`
- Simulator default data route: old DB `health`

## Sensitive Data Handling

- No GitHub token file was copied.
- No DeepSeek API key file was copied.
- No database password was written to helper scripts.
