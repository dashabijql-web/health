param(
  [ValidateSet('old', 'new', 'both')]
  [string]$DataSource = 'old',

  [switch]$SkipBackendTests,
  [switch]$NoStartSimulatorForOld,
  [switch]$NoStopSimulatorForNew
)

$ErrorActionPreference = 'Continue'
$ProgressPreference = 'SilentlyContinue'

$root = 'D:\Health'
$show = Join-Path $root 'HealthShow'
$data = Join-Path $root 'HealthData'
$runId = "$(Get-Date -Format 'yyyyMMdd-HHmmss')-full-stack-local-$DataSource"
$outDir = Join-Path $show "tests\runs\$runId"
$summaryFile = Join-Path $outDir 'full-stack-local-summary.json'
$summaryMdFile = Join-Path $outDir 'summary.md'
$hermesArchiveFile = Join-Path $outDir 'hermes-archive.md'
$logFile = Join-Path $outDir 'full-stack-local.log'
$latestFile = Join-Path $show 'tests\runs\latest-full-stack-local.json'

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$startedAt = Get-Date
$startedProcesses = New-Object System.Collections.ArrayList
$stoppedProcesses = New-Object System.Collections.ArrayList
$steps = New-Object System.Collections.ArrayList
$phases = New-Object System.Collections.ArrayList
$dataSourceFacts = [ordered]@{}

function Log([string]$message) {
  $line = "[$(Get-Date -Format o)] $message"
  $line | Tee-Object -FilePath $logFile -Append
}

function Test-Port([int]$port) {
  try {
    $client = New-Object Net.Sockets.TcpClient
    $iar = $client.BeginConnect('127.0.0.1', $port, $null, $null)
    $ok = $iar.AsyncWaitHandle.WaitOne(2000, $false)
    if ($ok) { $client.EndConnect($iar) }
    $client.Close()
    return [bool]$ok
  } catch {
    return $false
  }
}

function Wait-Port([string]$name, [int]$port, [int]$timeoutSeconds = 120) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (Test-Port $port) {
      Log "READY $name 127.0.0.1:$port"
      return $true
    }
    Start-Sleep -Seconds 2
  }
  Log "BLOCKED $name 127.0.0.1:$port not ready after ${timeoutSeconds}s"
  return $false
}

function Wait-PortClosed([string]$name, [int]$port, [int]$timeoutSeconds = 30) {
  $deadline = (Get-Date).AddSeconds($timeoutSeconds)
  while ((Get-Date) -lt $deadline) {
    if (-not (Test-Port $port)) {
      Log "READY $name 127.0.0.1:$port closed"
      return $true
    }
    Start-Sleep -Seconds 1
  }
  Log "WARN $name 127.0.0.1:$port still listening after ${timeoutSeconds}s"
  return $false
}

function Add-StartedProcess([string]$name, $process) {
  if ($null -eq $process) { return }
  [void]$startedProcesses.Add([ordered]@{
    name = $name
    id = $process.Id
    startedAt = (Get-Date -Format o)
  })
}

function Start-NoWindowProcess(
  [string]$filePath,
  [string[]]$arguments,
  [string]$workingDirectory,
  [string]$stdout,
  [string]$stderr
) {
  function Quote-ProcessArgument([string]$value) {
    if ($null -eq $value) { return '""' }
    if ($value -notmatch '[\s"]') { return $value }
    return '"' + ($value -replace '"', '\"') + '"'
  }

  $effectiveFilePath = $filePath
  $effectiveArguments = $arguments
  if ($filePath -match '\.(cmd|bat)$') {
    $commandLine = @((Quote-ProcessArgument $filePath)) + @($arguments | ForEach-Object { Quote-ProcessArgument $_ })
    $effectiveFilePath = 'cmd.exe'
    $effectiveArguments = @('/d', '/s', '/c', ($commandLine -join ' '))
  }

  $startInfo = New-Object System.Diagnostics.ProcessStartInfo
  $startInfo.FileName = $effectiveFilePath
  $startInfo.Arguments = ($effectiveArguments | ForEach-Object { Quote-ProcessArgument $_ }) -join ' '
  $startInfo.WorkingDirectory = $workingDirectory
  $startInfo.UseShellExecute = $false
  $startInfo.CreateNoWindow = $true
  $startInfo.RedirectStandardOutput = $true
  $startInfo.RedirectStandardError = $true

  $process = New-Object System.Diagnostics.Process
  $process.StartInfo = $startInfo

  $outPath = $stdout
  $errPath = $stderr
  Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
    if ($EventArgs.Data) { Add-Content -LiteralPath $Event.MessageData -Encoding UTF8 -Value $EventArgs.Data }
  } -MessageData $outPath | Out-Null
  Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
    if ($EventArgs.Data) { Add-Content -LiteralPath $Event.MessageData -Encoding UTF8 -Value $EventArgs.Data }
  } -MessageData $errPath | Out-Null

  [void]$process.Start()
  $process.BeginOutputReadLine()
  $process.BeginErrorReadLine()

  return $process
}

function Initialize-WindowProbe {
  if ('Win32WindowProbe' -as [type]) { return }

  Add-Type @'
using System;
using System.Runtime.InteropServices;
public static class Win32WindowProbe {
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
}
'@
}

function Test-ProcessHasVisibleWindow([int]$processId) {
  try {
    Initialize-WindowProbe
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    return $process -and $process.MainWindowHandle -ne 0 -and [Win32WindowProbe]::IsWindowVisible($process.MainWindowHandle)
  } catch {
    return $false
  }
}

function Get-BackendProcesses {
  @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object {
      $_.CommandLine -and (
        $_.CommandLine.Contains('D:\Health\HealthData\pom.xml') -or
        $_.CommandLine.Contains('com.xzkj.health.HealthApplication')
      )
    } |
    Select-Object ProcessId, Name, CommandLine)
}

function Stop-VisibleBackendProcesses {
  $stoppedAny = $false
  $backendProcesses = Get-BackendProcesses
  foreach ($process in $backendProcesses) {
    if (-not (Test-ProcessHasVisibleWindow $process.ProcessId)) { continue }

    try {
      Log "STOP visible backend window pid=$($process.ProcessId) before managed no-window startup"
      Stop-Process -Id $process.ProcessId -Force
      $stoppedAny = $true
      [void]$stoppedProcesses.Add([ordered]@{
        name = 'backend'
        id = $process.ProcessId
        stoppedAt = (Get-Date -Format o)
        reason = 'visible-window-restart'
      })
    } catch {
      Log "FAILED to stop visible backend pid=$($process.ProcessId): $($_.Exception.Message)"
    }
  }

  return $stoppedAny
}

function Start-IfPortClosed(
  [string]$name,
  [int]$port,
  [string]$filePath,
  [string[]]$arguments,
  [string]$workingDirectory,
  [string]$stdoutName,
  [string]$stderrName
) {
  if (Test-Port $port) {
    Log "EXISTING $name on port $port"
    return $null
  }

  Log "START $name"
  $stdout = Join-Path $outDir $stdoutName
  $stderr = Join-Path $outDir $stderrName
  try {
    $process = Start-NoWindowProcess $filePath $arguments $workingDirectory $stdout $stderr
    Add-StartedProcess $name $process
    return $process
  } catch {
    Log "FAILED to start ${name}: $($_.Exception.Message)"
    return $null
  }
}

function Get-SimulatorProcesses {
  @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
    Where-Object { $_.CommandLine -match 'watch_tcp_simulator_1000\.py' } |
    Select-Object ProcessId, CommandLine)
}

function Start-SimulatorForOld {
  $running = @(Get-SimulatorProcesses)
  if ($running.Count -gt 0) {
    Log "EXISTING watch-simulator process(es): $($running.ProcessId -join ',')"
    return
  }

  Log 'START watch-simulator for old data source'
  $stdout = Join-Path $outDir 'simulator.out.log'
  $stderr = Join-Path $outDir 'simulator.err.log'
  try {
    $process = Start-NoWindowProcess 'python' @('watch_tcp_simulator_1000.py') $show $stdout $stderr
    Add-StartedProcess 'watch-simulator' $process
    Start-Sleep -Seconds 8
  } catch {
    Log "FAILED to start watch-simulator: $($_.Exception.Message)"
  }
}

function Stop-SimulatorForNew {
  $running = @(Get-SimulatorProcesses)
  if ($running.Count -eq 0) {
    Log 'READY new phase: no watch-simulator process is running'
    return
  }

  foreach ($process in $running) {
    try {
      Log "STOP watch-simulator pid=$($process.ProcessId) before new data source phase"
      Stop-Process -Id $process.ProcessId -Force
      [void]$stoppedProcesses.Add([ordered]@{
        name = 'watch-simulator'
        id = $process.ProcessId
        stoppedAt = (Get-Date -Format o)
        reason = 'new-data-source-phase'
      })
    } catch {
      Log "FAILED to stop watch-simulator pid=$($process.ProcessId): $($_.Exception.Message)"
    }
  }
  Start-Sleep -Seconds 2
}

function Write-CombinedLog([string]$stdout, [string]$stderr, [string]$combined) {
  Set-Content -LiteralPath $combined -Encoding UTF8 -Value "### STDOUT`r`n"
  if (Test-Path -LiteralPath $stdout) {
    Get-Content -LiteralPath $stdout -Raw | Add-Content -LiteralPath $combined -Encoding UTF8
  }
  Add-Content -LiteralPath $combined -Encoding UTF8 -Value "`r`n### STDERR`r`n"
  if (Test-Path -LiteralPath $stderr) {
    Get-Content -LiteralPath $stderr -Raw | Add-Content -LiteralPath $combined -Encoding UTF8
  }
}

function Invoke-Step([string]$name, [string]$command, [string]$workingDirectory = $show) {
  $started = Get-Date
  Log "RUN $name :: $command"
  $stdout = Join-Path $outDir "$name.out.log"
  $stderr = Join-Path $outDir "$name.err.log"
  $combined = Join-Path $outDir "$name.combined.log"

  Push-Location $workingDirectory
  try {
    & cmd.exe /c $command 1> $stdout 2> $stderr
    $code = $LASTEXITCODE
  } finally {
    Pop-Location
  }

  Write-CombinedLog $stdout $stderr $combined
  $finished = Get-Date
  $containsSkipped = $false
  try {
    $containsSkipped = [bool](Select-String -LiteralPath $combined -Pattern '"status"\s*:\s*"skipped"', '\|\s*[^|]+\s*\|\s*skipped\s*\|' -Quiet)
  } catch {
    $containsSkipped = $false
  }
  $stepStatus = if ($code -ne 0) { 'failed' } elseif ($containsSkipped) { 'skipped' } else { 'passed' }
  $record = [ordered]@{
    name = $name
    command = $command
    workingDirectory = $workingDirectory
    exitCode = $code
    status = $stepStatus
    containsSkipped = $containsSkipped
    startedAt = ($started.ToString('o'))
    finishedAt = ($finished.ToString('o'))
    durationMs = [int](New-TimeSpan -Start $started -End $finished).TotalMilliseconds
    stdout = $stdout
    stderr = $stderr
    combined = $combined
  }
  [void]$steps.Add($record)
  Log "DONE $name exit=$code stdout=$stdout stderr=$stderr combined=$combined"
  return $record
}

function Invoke-Phase([string]$name, [scriptblock]$body) {
  $started = Get-Date
  Log "PHASE $name start"
  & $body
  $finished = Get-Date
  $phaseSteps = @($steps | Where-Object {
    ([datetime]$_.startedAt) -ge $started -and ([datetime]$_.startedAt) -le $finished
  })
  $status = if (@($phaseSteps | Where-Object { $_.exitCode -ne 0 }).Count -gt 0) {
    'failed'
  } elseif (@($phaseSteps | Where-Object { $_.status -eq 'skipped' }).Count -gt 0) {
    'skipped'
  } else {
    'passed'
  }
  [void]$phases.Add([ordered]@{
    name = $name
    status = $status
    startedAt = $started.ToString('o')
    finishedAt = $finished.ToString('o')
    durationMs = [int](New-TimeSpan -Start $started -End $finished).TotalMilliseconds
    stepCount = $phaseSteps.Count
  })
  Log "PHASE $name $status"
}

function Save-GitStatus([string]$repo, [string]$name) {
  $file = Join-Path $outDir "git-$name-status.log"
  Push-Location $repo
  try {
    & git status --short 1> $file 2>&1
  } finally {
    Pop-Location
  }
  return $file
}

function Get-Readiness {
  [ordered]@{
    sql58135 = Test-Port 58135
    redis6379 = Test-Port 6379
    backend8080 = Test-Port 8080
    backendTcp9000 = Test-Port 9000
    frontend9528 = Test-Port 9528
    sqlPasswordSet = [bool]$env:SQL_PASSWORD
    simulatorRunning = @((Get-SimulatorProcesses)).Count -gt 0
  }
}

function Invoke-SqlScalar([string]$database, [string]$query) {
  $sqlcmd = if ($env:SQLCMD_BIN) { $env:SQLCMD_BIN } else { 'sqlcmd' }
  $output = & $sqlcmd -S 'localhost,58135' -U 'sa' -P $env:SQL_PASSWORD -d $database -h -1 -W -Q "SET NOCOUNT ON; $query" 2>&1
  if ($LASTEXITCODE -ne 0) {
    throw "sqlcmd exit=$LASTEXITCODE database=$database query=$query output=$($output -join ' ')"
  }

  $line = @($output | ForEach-Object { "$_".Trim() } | Where-Object { $_ }) | Select-Object -First 1
  if (-not $line) { return 0 }
  return [int]$line
}

function Get-DataSourceFacts {
  $sources = if ($DataSource -eq 'both') { @('old', 'new') } else { @($DataSource) }
  $facts = [ordered]@{}

  foreach ($source in $sources) {
    $database = if ($source -eq 'new') { 'health_new' } else { 'health' }
    $record = [ordered]@{
      database = $database
      ok = $true
      error = $null
      departmentRows = 0
      employeeRows = 0
      deviceRows = 0
      deviceUserRows = 0
      realtimeRows = 0
      userOnlineRows = 0
      simulatorDeviceRows = 0
    }

    try {
      $record.departmentRows = Invoke-SqlScalar $database 'SELECT COUNT(*) FROM department'
      $record.employeeRows = Invoke-SqlScalar $database 'SELECT COUNT(*) FROM employee'
      $record.deviceRows = Invoke-SqlScalar $database 'SELECT COUNT(*) FROM device'
      $record.deviceUserRows = Invoke-SqlScalar $database 'SELECT COUNT(*) FROM device_user'
      $record.realtimeRows = Invoke-SqlScalar $database 'SELECT COUNT(*) FROM realtime_data'
      $record.userOnlineRows = Invoke-SqlScalar $database 'SELECT COUNT(*) FROM user_online_status'
      $record.simulatorDeviceRows = Invoke-SqlScalar $database "SELECT COUNT(*) FROM device WHERE imei LIKE '3594567800_____'"
    } catch {
      $record.ok = $false
      $record.error = $_.Exception.Message
    }

    $facts[$source] = $record
  }

  return $facts
}

function Get-RecordValue($record, [string]$key) {
  if ($null -eq $record) { return $null }
  if ($record -is [System.Collections.IDictionary]) { return $record[$key] }
  return $record.$key
}

function Write-HermesArchive($summary, $skippedSteps) {
  $failedSteps = @($summary['steps'] | Where-Object { (Get-RecordValue $_ 'exitCode') -ne 0 })
  $archive = New-Object System.Collections.Generic.List[string]
  $archive.Add('# Hermes Archive')
  $archive.Add('')
  $archive.Add("- run_id: ``$($summary['runId'])``")
  $archive.Add("- status: ``$($summary['status'])``")
  $archive.Add("- status_reason: ``$($summary['statusReason'])``")
  $archive.Add("- data_source: ``$($summary['dataSource'])``")
  $archive.Add("- phase statuses:")
  foreach ($phase in @($summary['phases'])) {
    $archive.Add("  - $(Get-RecordValue $phase 'name'): ``$(Get-RecordValue $phase 'status')``")
  }
  if (@($summary['phases']).Count -eq 0) {
    $archive.Add('  - none')
  }

  $facts = $summary['dataSourceFacts']
  if ($facts -and $facts.Keys.Count -gt 0) {
    foreach ($source in $facts.Keys) {
      $fact = $facts[$source]
      $archive.Add("- $source dataSourceFacts:")
      $archive.Add("  - database: ``$($fact.database)``")
      $archive.Add("  - departmentRows: ``$($fact.departmentRows)``")
      $archive.Add("  - employeeRows: ``$($fact.employeeRows)``")
      $archive.Add("  - deviceRows: ``$($fact.deviceRows)``")
      $archive.Add("  - deviceUserRows: ``$($fact.deviceUserRows)``")
      $archive.Add("  - realtimeRows: ``$($fact.realtimeRows)``")
      $archive.Add("  - userOnlineRows: ``$($fact.userOnlineRows)``")
      $archive.Add("  - simulatorDeviceRows: ``$($fact.simulatorDeviceRows)``")
    }
  } else {
    $archive.Add('- dataSourceFacts: `none`')
  }

  $archive.Add('- simulator evidence:')
  $simulatorStarted = @($summary['startedProcesses'] | Where-Object { (Get-RecordValue $_ 'name') -eq 'watch-simulator' })
  $simulatorStopped = @($summary['stoppedProcesses'] | Where-Object { (Get-RecordValue $_ 'name') -eq 'watch-simulator' })
  foreach ($process in $simulatorStarted) {
    $archive.Add("  - started: ``watch-simulator``, pid ``$(Get-RecordValue $process 'id')``, ``$(Get-RecordValue $process 'startedAt')``")
  }
  foreach ($process in $simulatorStopped) {
    $archive.Add("  - stopped: ``watch-simulator``, pid ``$(Get-RecordValue $process 'id')``, ``$(Get-RecordValue $process 'stoppedAt')``, reason ``$(Get-RecordValue $process 'reason')``")
  }
  if ($simulatorStarted.Count -eq 0 -and $simulatorStopped.Count -eq 0) {
    $archive.Add('  - none')
  }
  $archive.Add("  - final readiness: ``simulatorRunning=$($summary['readiness'].simulatorRunning)``")

  $skippedNames = @($skippedSteps | ForEach-Object { Get-RecordValue $_ 'name' })
  $archive.Add("- skipped_steps: ``$(if ($skippedNames.Count -gt 0) { $skippedNames -join ', ' } else { 'none' })``")
  $archive.Add('- issue triage:')
  $archive.Add("  - failed steps: ``$($failedSteps.Count)``")
  $archive.Add("  - allowed skipped steps: ``$($skippedSteps.Count)``")
  if ($failedSteps.Count -eq 0) {
    $archive.Add('  - action: no Health code repair required from this cycle')
  } else {
    $archive.Add('  - action: hand failed steps to Codex for root-cause analysis')
  }

  $archive -join "`r`n" | Set-Content -LiteralPath $hermesArchiveFile -Encoding UTF8
}

function Write-SummaryFiles([string]$status, [string]$statusReason, $readiness, $gitStatus) {
  $finishedAt = Get-Date
  $skippedSteps = @($steps | Where-Object { $_.status -eq 'skipped' })
  $summary = [ordered]@{
    runId = $runId
    profile = 'full-stack-local'
    dataSource = $DataSource
    status = $status
    statusReason = $statusReason
    startedAt = $startedAt.ToString('o')
    finishedAt = $finishedAt.ToString('o')
    durationMs = [int](New-TimeSpan -Start $startedAt -End $finishedAt).TotalMilliseconds
    outDir = $outDir
    logFile = $logFile
    hermesArchive = $hermesArchiveFile
    parameters = [ordered]@{
      dataSource = $DataSource
      skipBackendTests = [bool]$SkipBackendTests
      noStartSimulatorForOld = [bool]$NoStartSimulatorForOld
      noStopSimulatorForNew = [bool]$NoStopSimulatorForNew
    }
    environment = [ordered]@{
      root = $root
      HealthShow = $show
      HealthData = $data
      frontendUrl = 'http://127.0.0.1:9528/'
      backendUrl = 'http://127.0.0.1:8080/health'
      sqlServer = '127.0.0.1:58135'
      redis = '127.0.0.1:6379'
      tcp = '127.0.0.1:9000'
    }
    readiness = $readiness
    dataSourceFacts = $dataSourceFacts
    gitStatus = $gitStatus
    startedProcesses = @($startedProcesses)
    stoppedProcesses = @($stoppedProcesses)
    phases = @($phases)
    steps = @($steps)
    skippedSteps = @($skippedSteps)
  }

  $summary | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $summaryFile -Encoding UTF8
  [ordered]@{
    runId = $runId
    status = $status
    statusReason = $statusReason
    dataSource = $DataSource
    outDir = $outDir
    summaryJson = $summaryFile
    summaryMd = $summaryMdFile
    hermesArchive = $hermesArchiveFile
    finishedAt = $finishedAt.ToString('o')
  } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $latestFile -Encoding UTF8

  $failedSteps = @($steps | Where-Object { $_.exitCode -ne 0 })
  $lines = New-Object System.Collections.Generic.List[string]
  $lines.Add('# HealthShow full stack local run')
  $lines.Add('')
  $lines.Add("- run_id: $runId")
  $lines.Add("- status: $status")
  $lines.Add("- status_reason: $statusReason")
  $lines.Add("- data_source: $DataSource")
  $lines.Add("- out_dir: $outDir")
  $lines.Add("- summary_json: $summaryFile")
  $lines.Add("- hermes_archive: $hermesArchiveFile")
  $lines.Add("- latest_pointer: $latestFile")
  $lines.Add('')
  $lines.Add('## Readiness')
  $lines.Add('')
  foreach ($key in $readiness.Keys) {
    $lines.Add("- ${key}: $($readiness[$key])")
  }
  $lines.Add('')
  $lines.Add('## Data Source Facts')
  $lines.Add('')
  if ($dataSourceFacts.Keys.Count -eq 0) {
    $lines.Add('- none')
  } else {
    foreach ($source in $dataSourceFacts.Keys) {
      $fact = $dataSourceFacts[$source]
      $lines.Add("- ${source}: database=$($fact.database), ok=$($fact.ok), department=$($fact.departmentRows), employee=$($fact.employeeRows), device=$($fact.deviceRows), device_user=$($fact.deviceUserRows), realtime_data=$($fact.realtimeRows), user_online_status=$($fact.userOnlineRows), simulatorDeviceRows=$($fact.simulatorDeviceRows)")
      if (-not $fact.ok -and $fact.error) {
        $lines.Add("  - error: $($fact.error)")
      }
    }
  }
  $lines.Add('')
  $lines.Add('## Steps')
  $lines.Add('')
  $lines.Add('| step | status | exit | ms | log |')
  $lines.Add('| --- | --- | ---: | ---: | --- |')
  foreach ($step in $steps) {
    $logName = Split-Path -Leaf $step.combined
    $lines.Add("| $($step.name) | $($step.status) | $($step.exitCode) | $($step.durationMs) | $logName |")
  }
  if ($steps.Count -eq 0) {
    $lines.Add('| - | - | - | - | - |')
  }
  $lines.Add('')
  $lines.Add('## Failed Steps')
  $lines.Add('')
  if ($failedSteps.Count -eq 0) {
    $lines.Add('- none')
  } else {
    foreach ($step in $failedSteps) {
      $lines.Add("- $($step.name): exit=$($step.exitCode), log=$($step.combined)")
    }
  }
  $lines.Add('')
  $lines.Add('## Skipped Steps')
  $lines.Add('')
  if ($skippedSteps.Count -eq 0) {
    $lines.Add('- none')
  } else {
    foreach ($step in $skippedSteps) {
      $lines.Add("- $($step.name): exit=$($step.exitCode), log=$($step.combined)")
    }
  }
  $lines.Add('')
  $lines.Add('## Git Status Logs')
  $lines.Add('')
  $lines.Add("- HealthShow: $($gitStatus.HealthShow)")
  $lines.Add("- HealthData: $($gitStatus.HealthData)")
  $lines -join "`r`n" | Set-Content -LiteralPath $summaryMdFile -Encoding UTF8
  Write-HermesArchive $summary $skippedSteps
}

Log "full stack local run start: $runId"
Log "outDir=$outDir"
Log "dataSource=$DataSource skipBackendTests=$([bool]$SkipBackendTests)"

$appYml = Join-Path $data 'src\main\resources\application.yml'
if (-not $env:SQL_PASSWORD -and (Test-Path $appYml)) {
  $text = Get-Content $appYml -Raw
  $m = [regex]::Match($text, 'DB_PASSWORD:([^}]+)')
  if ($m.Success) { $env:SQL_PASSWORD = $m.Groups[1].Value }
}
if (-not $env:DB_PASSWORD -and $env:SQL_PASSWORD) { $env:DB_PASSWORD = $env:SQL_PASSWORD }
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;D:\apache-maven-3.8.1\bin;C:\nvm4w\nodejs;C:\Program Files\Redis;C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn;$env:Path"

$gitStatus = [ordered]@{
  HealthShow = Save-GitStatus $show 'healthshow'
  HealthData = Save-GitStatus $data 'healthdata'
}

if (-not (Test-Port 58135)) {
  Log 'SQL Server port 58135 is not listening; attempting service start MSSQLSERVER2019/MSSQLSERVER'
  Start-Service -Name 'MSSQLSERVER2019' -ErrorAction SilentlyContinue
  Start-Service -Name 'MSSQLSERVER' -ErrorAction SilentlyContinue
}
Wait-Port 'sql-server' 58135 90 | Out-Null

if (-not (Test-Port 6379)) {
  $redis = 'C:\Program Files\Redis\redis-server.exe'
  $redisConf = 'C:\Program Files\Redis\redis.windows.conf'
  if (Test-Path $redis) {
    Start-IfPortClosed 'redis' 6379 $redis @($redisConf) 'C:\Program Files\Redis' 'redis.out.log' 'redis.err.log' | Out-Null
  }
}
Wait-Port 'redis' 6379 60 | Out-Null

if (Stop-VisibleBackendProcesses) {
  Wait-PortClosed 'backend-http' 8080 30 | Out-Null
  Wait-PortClosed 'backend-tcp' 9000 30 | Out-Null
}

Start-IfPortClosed 'backend' 8080 'D:\apache-maven-3.8.1\bin\mvn.cmd' @('spring-boot:run', '-f', 'D:\Health\HealthData\pom.xml') $data 'backend.out.log' 'backend.err.log' | Out-Null
Wait-Port 'backend-http' 8080 180 | Out-Null
Wait-Port 'backend-tcp' 9000 120 | Out-Null

Start-IfPortClosed 'frontend' 9528 'cmd.exe' @('/c', 'npm run dev') $show 'frontend.out.log' 'frontend.err.log' | Out-Null
Wait-Port 'frontend' 9528 120 | Out-Null

$readiness = Get-Readiness
Log "READINESS $(($readiness | ConvertTo-Json -Compress))"

$blockedKeys = @('sql58135', 'redis6379', 'backend8080', 'backendTcp9000', 'frontend9528', 'sqlPasswordSet') |
  Where-Object { -not $readiness[$_] }

if ($blockedKeys.Count -gt 0) {
  $reason = "blocked readiness: $($blockedKeys -join ', ')"
  Log $reason
  Write-SummaryFiles 'blocked' $reason $readiness $gitStatus
  exit 3
}

if (-not $SkipBackendTests) {
  Invoke-Phase 'backend' {
    Invoke-Step 'backend-maven-test' 'D:\apache-maven-3.8.1\bin\mvn.cmd -q test -f D:\Health\HealthData\pom.xml' $data | Out-Null
    Invoke-Step 'backend-regression' 'python D:\Health\HealthData\scripts\run_backend_regression.py' $data | Out-Null
  }
}

Invoke-Phase 'frontend-common' {
  Invoke-Step 'test-fast' 'npm run test:fast' $show | Out-Null
  Invoke-Step 'test-frontend' 'npm run test:frontend' $show | Out-Null
  Invoke-Step 'test-quality' 'npm run test:quality' $show | Out-Null
}

if ($DataSource -eq 'old' -or $DataSource -eq 'both') {
  Invoke-Phase 'old-data-source' {
    if (-not $NoStartSimulatorForOld) {
      Start-SimulatorForOld
    } else {
      Log 'SKIP simulator start for old data source because -NoStartSimulatorForOld was provided'
    }
    Invoke-Step 'test-integration-old' 'npm run test:integration:old' $show | Out-Null
    Invoke-Step 'test-perf-old' 'npm run test:perf:old' $show | Out-Null
    Invoke-Step 'test-full-old' 'npm run test:full:old' $show | Out-Null
  }
}

if ($DataSource -eq 'new' -or $DataSource -eq 'both') {
  Invoke-Phase 'new-data-source' {
    if (-not $NoStopSimulatorForNew) {
      Stop-SimulatorForNew
    } else {
      Log 'SKIP simulator stop for new data source because -NoStopSimulatorForNew was provided'
    }
    Invoke-Step 'test-integration-new' 'npm run test:integration:new' $show | Out-Null
    Invoke-Step 'test-perf-new' 'npm run test:perf:new' $show | Out-Null
    Invoke-Step 'test-full-new' 'npm run test:full:new' $show | Out-Null
  }
}

$readiness = Get-Readiness
$dataSourceFacts = Get-DataSourceFacts
$failed = @($steps | Where-Object { $_.exitCode -ne 0 })
$skipped = @($steps | Where-Object { $_.status -eq 'skipped' })
$status = if ($failed.Count -gt 0) { 'failed' } elseif ($skipped.Count -gt 0) { 'skipped' } else { 'passed' }
$reason = if ($failed.Count -gt 0) {
  "nonzero steps: $($failed.Count)"
} elseif ($skipped.Count -gt 0) {
  "allowed skipped steps: $($skipped.Count)"
} else {
  'all steps passed'
}

Write-SummaryFiles $status $reason $readiness $gitStatus
Log "summary=$summaryFile"
Log "summary_md=$summaryMdFile"
Log "hermes_archive=$hermesArchiveFile"

if ($failed.Count -gt 0) {
  Log "RESULT failed steps: $($failed.Count)"
  exit 1
}

if ($skipped.Count -gt 0) {
  Log "RESULT SKIPPED $reason"
  exit 0
}

Log 'RESULT PASS all steps'
exit 0
