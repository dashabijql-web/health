# Health WSL Handoff with Windows SQL Server

This package is for a target machine where:
- the source code lives in WSL Linux filesystem
- SQL Server runs on Windows
- Redis can run on Windows or Docker, depending on local setup

## Recommended layout
- Unpack the repo inside WSL, for example: `/home/<user>/code/health`
- Keep SQL Server on Windows and point `DB_HOST` / `DB_PORT` at the Windows SQL Server listener
- Use `HealthShow/tests/run-full-stack-local.py` or `tools/health-wsl-stack.sh` from WSL

## Connecting from WSL to Windows SQL Server
- Prefer the Windows host IP from WSL, not `localhost`, unless you have verified localhost forwarding works on that machine.
- If the Windows SQL Server listens on `1433`, set `DB_PORT=1433`.
- If the project expects `11433`, either change SQL Server to `11433` or export `DB_PORT=1433` before starting.

## Useful checks
```bash
bash /home/j/code/health/tools/health-wsl-stack.sh status
python3 /home/j/code/health/HealthShow/tests/run-full-stack-local.py --data-source both
```

## Database
This archive does not include `.bak` files. Restore `health` and `health_new` on the Windows SQL Server host before running the app.
