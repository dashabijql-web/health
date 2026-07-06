param(
  [string]$LocalAddress = "10.8.138.214",
  [int]$Port = 9000,
  [int]$DurationSec = 360,
  [int]$MeasurePeriodMinutes = 1,
  [int]$CommandIntervalSec = 45
)

$ErrorActionPreference = "Stop"

$logDir = "\\wsl.localhost\Ubuntu\home\j\code\health\runtime-logs\watch-probe"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$logFile = Join-Path $logDir ("windows-watch-health-probe-{0}.log" -f (Get-Date -Format "yyyyMMdd-HHmmss"))

function Write-Log([string]$Message) {
  $line = "{0} {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"), $Message
  Write-Output $line
  Add-Content -Path $logFile -Value $line -Encoding UTF8
}

function Send-Ascii($Stream, [string]$Payload) {
  $bytes = [System.Text.Encoding]::ASCII.GetBytes($Payload)
  $Stream.Write($bytes, 0, $bytes.Length)
  $Stream.Flush()
  Write-Log ("TX {0}" -f $Payload)
}

function New-Serial() {
  return "{0:D6}" -f ([int]((Get-Date).Ticks % 1000000))
}

function Reply-ForFrame([string]$Frame, [string]$Imei) {
  if ($Frame -match "^IWAP00(?<imei>\d{15})#$") {
    $script:CurrentImei = $Matches.imei
    return "IWBP00,{0},8#" -f (Get-Date).ToUniversalTime().ToString("yyyyMMddHHmmss")
  }

  if ($Frame -match "^IW(?<code>AP[A-Z0-9]{2})") {
    $code = $Matches.code
    switch ($code) {
      "AP01" { return "IWBP01#" }
      "AP91" { return "IWBP91#" }
      "AP02" { return "IWBP02#" }
      "AP92" { return "IWBP92#" }
      "AP03" { return "IWBP03#" }
      "AP05" { return "IWBP05,0#" }
      "AP07" { return "IWBP07#" }
      "AP10" { return "IWBP10#" }
      "AP49" { return "IWBP49#" }
      "AP50" { return "IWBP50#" }
      "AP51" { return "IWBP51#" }
      "APHT" { return "IWBPHT#" }
      "APHP" { return "IWBPHP#" }
      "APTM" { return ("IWBPTM,{0},0,0#" -f (Get-Date).ToUniversalTime().ToString("yyyyMMddHHmmss")) }
      "APWT" { return $null }
      "AP94" { return "IWBP94#" }
      "AP97" { return "IWBP97#" }
      "APRR" { return "IWBPRR#" }
      default { return $null }
    }
  }

  return $null
}

function Send-ProbeCommands($Stream, [string]$Imei) {
  if ([string]::IsNullOrWhiteSpace($Imei)) {
    return
  }

  $serial = New-Serial
  $commands = @(
    ("IWBP33,{0},{1},1#" -f $Imei, $serial),
    ("IWBP86,{0},{1},1,{2}#" -f $Imei, (New-Serial), $MeasurePeriodMinutes),
    ("IWBP87,{0},{1},1,{2}#" -f $Imei, (New-Serial), $MeasurePeriodMinutes),
    ("IWBPXL,{0},{1}#" -f $Imei, (New-Serial)),
    ("IWBPXY,{0},{1}#" -f $Imei, (New-Serial)),
    ("IWBPXZ,{0},{1}#" -f $Imei, (New-Serial)),
    ("IWBPXT,{0},{1}#" -f $Imei, (New-Serial)),
    ("IWBP16,{0},{1}#" -f $Imei, (New-Serial))
  )

  foreach ($cmd in $commands) {
    Send-Ascii $Stream $cmd
    Start-Sleep -Milliseconds 800
  }
}

$script:CurrentImei = $null
$ip = [System.Net.IPAddress]::Parse($LocalAddress)
$listener = [System.Net.Sockets.TcpListener]::new($ip, $Port)
$listener.Server.SetSocketOption(
  [System.Net.Sockets.SocketOptionLevel]::Socket,
  [System.Net.Sockets.SocketOptionName]::ReuseAddress,
  $true
)

try {
  $listener.Start()
  Write-Log ("LISTEN {0}:{1} duration={2}s log={3}" -f $LocalAddress, $Port, $DurationSec, $logFile)
  $endAt = (Get-Date).AddSeconds($DurationSec)
  $lastCommandAt = (Get-Date).AddYears(-1)

  while ((Get-Date) -lt $endAt) {
    if (-not $listener.Pending()) {
      Start-Sleep -Milliseconds 100
      continue
    }

    $client = $listener.AcceptTcpClient()
    $remote = $client.Client.RemoteEndPoint.ToString()
    Write-Log ("ACCEPT {0}" -f $remote)
    $client.ReceiveTimeout = 1000
    $client.SendTimeout = 1000
    $stream = $client.GetStream()
    $buffer = New-Object byte[] 4096
    $textBuffer = ""
    $connectionEndAt = (Get-Date).AddSeconds(90)

    try {
      while ($client.Connected -and (Get-Date) -lt $connectionEndAt -and (Get-Date) -lt $endAt) {
        if ($stream.DataAvailable) {
          $n = $stream.Read($buffer, 0, $buffer.Length)
          if ($n -le 0) {
            break
          }

          $chunk = [System.Text.Encoding]::ASCII.GetString($buffer, 0, $n)
          $textBuffer += $chunk

          while ($textBuffer.Contains("#")) {
            $idx = $textBuffer.IndexOf("#")
            $frame = $textBuffer.Substring(0, $idx + 1)
            $textBuffer = $textBuffer.Substring($idx + 1)
            Write-Log ("RX {0} {1}" -f $remote, $frame)

            $reply = Reply-ForFrame $frame $script:CurrentImei
            if ($reply) {
              Send-Ascii $stream $reply
            }

            if ($script:CurrentImei -and ((Get-Date) - $lastCommandAt).TotalSeconds -ge $CommandIntervalSec) {
              Send-ProbeCommands $stream $script:CurrentImei
              $lastCommandAt = Get-Date
            }
          }
        } else {
          Start-Sleep -Milliseconds 100
        }
      }
    } catch {
      Write-Log ("CONNECTION_ERROR {0} {1}" -f $remote, $_.Exception.Message)
    } finally {
      $client.Close()
      Write-Log ("CLOSE {0}" -f $remote)
    }
  }
} finally {
  $listener.Stop()
  Write-Log "STOP"
}
