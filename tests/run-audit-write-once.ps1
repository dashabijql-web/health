$ErrorActionPreference='Stop'
cd 'D:\Health\HealthShow'
if (-not $env:SQL_PASSWORD) {
  $text = Get-Content 'D:\Health\HealthData\src\main\resources\application.yml' -Raw
  $m = [regex]::Match($text, 'DB_PASSWORD:([^}]+)')
  if ($m.Success) { $env:SQL_PASSWORD = $m.Groups[1].Value }
}
npm run audit:write
exit $LASTEXITCODE
