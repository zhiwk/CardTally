param(
    [int]$BuildTimeoutMinutes = 10,
    [int]$UnitTestTimeoutMinutes = 5,
    [int]$DeviceTestTimeoutMinutes = 8,
    [switch]$SkipDeviceTests
)

$ErrorActionPreference = 'Stop'

$root = Split-Path $PSScriptRoot -Parent
$gradle = Join-Path $root 'gradlew.bat'
$logRoot = Join-Path $root 'app/build/reports/verification'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$runLogRoot = Join-Path $logRoot $runId
$script:originalScreenOffTimeout = $null

if (-not (Test-Path -LiteralPath $gradle)) {
    throw "Gradle wrapper not found: $gradle"
}
New-Item -ItemType Directory -Path $runLogRoot -Force | Out-Null

function Stop-ProcessTree([int]$ProcessId) {
    if (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) {
        & taskkill.exe /PID $ProcessId /T /F 2>$null | Out-Null
    }
}

function Stop-VerificationProcesses {
    # Do not touch the daily or release application sandboxes.
    & adb.exe shell am force-stop com.example.cardtally.verification 2>$null | Out-Null
    & adb.exe shell am force-stop com.example.cardtally.verification.test 2>$null | Out-Null
}

function Prepare-VerificationDevice {
    $screenTimeout = (& adb.exe shell settings get system screen_off_timeout 2>$null | Out-String).Trim()
    if ($screenTimeout -match '^\d+$') {
        $script:originalScreenOffTimeout = $screenTimeout
        # The complete instrumentation suite can run longer than common phone
        # display timeouts. Keep the screen awake so ActivityScenario tests do
        # not resume after Android has saved the host activity state.
        & adb.exe shell settings put system screen_off_timeout 1800000 2>$null | Out-Null
    }
    & adb.exe shell input keyevent 224 2>$null | Out-Null
    Start-Sleep -Seconds 1
}

function Restore-VerificationDevice {
    if ($script:originalScreenOffTimeout -match '^\d+$') {
        & adb.exe shell settings put system screen_off_timeout $script:originalScreenOffTimeout 2>$null | Out-Null
        $script:originalScreenOffTimeout = $null
    }
}

function Invoke-Stage([string]$Name, [string[]]$Arguments, [int]$TimeoutMinutes) {
    $stdout = Join-Path $runLogRoot "$Name.stdout.log"
    $stderr = Join-Path $runLogRoot "$Name.stderr.log"
    Write-Host "START: $Name (timeout ${TimeoutMinutes}m)"

    # Start through cmd.exe because PowerShell cannot reliably expose the exit
    # code of a .bat file directly.
    $cmdArguments = @('/d', '/c', 'call', ('"' + $gradle + '"')) + $Arguments
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList $cmdArguments -WorkingDirectory $root -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    try {
        $finished = $process.WaitForExit($TimeoutMinutes * 60 * 1000)
        if (-not $finished) {
            Stop-ProcessTree $process.Id
            Stop-VerificationProcesses
            Write-Host "TIMEOUT: $Name after ${TimeoutMinutes}m"
            Write-Host "Logs: $stdout and $stderr"
            return 124
        }
        $process.Refresh()
        $stageExitCode = [int]$process.ExitCode
        $stdoutText = [System.IO.File]::ReadAllText($stdout)
        $stderrText = [System.IO.File]::ReadAllText($stderr)
        $reportedFailure = ($stdoutText + "`n" + $stderrText) -match '(?im)^BUILD FAILED\b|^Test run failed to complete\b|^> Task .+ FAILED\s*$'
        $reportedSuccess = $stdoutText -match '(?m)^BUILD SUCCESSFUL\b'
        if ($stageExitCode -ne 0 -or $reportedFailure -or -not $reportedSuccess) {
            $reason = if ($stageExitCode -ne 0) { "exit $stageExitCode" }
            elseif ($reportedFailure) { 'failure marker in Gradle output' }
            else { 'missing BUILD SUCCESSFUL marker' }
            Write-Host "FAIL: $Name ($reason)"
            Write-Host "Logs: $stdout and $stderr"
            return $(if ($stageExitCode -ne 0) { $stageExitCode } else { 1 })
        }
        Write-Host "PASS: $Name"
        return 0
    } finally {
        if (Get-Process -Id $process.Id -ErrorAction SilentlyContinue) {
            Stop-ProcessTree $process.Id
        }
    }
}

$stages = @(
    @{ Name = 'assemble-debug'; Args = @(':app:assembleEverydayDebug', '--no-daemon', '--console=plain'); Timeout = $BuildTimeoutMinutes },
    @{ Name = 'unit-test'; Args = @(':app:testEverydayUnitTest', '--no-daemon', '--console=plain'); Timeout = $UnitTestTimeoutMinutes }
)
if (-not $SkipDeviceTests) {
    $stages += @{ Name = 'connected-verification'; Args = @(':app:connectedVerificationDebugAndroidTest', '--no-daemon', '--console=plain'); Timeout = $DeviceTestTimeoutMinutes }
}

$exitCode = 0
try {
    foreach ($stage in $stages) {
        if ($stage.Name -eq 'connected-verification') {
            Prepare-VerificationDevice
        }
        $exitCode = Invoke-Stage $stage.Name $stage.Args $stage.Timeout
        if ($exitCode -ne 0) { break }
    }
} finally {
    Stop-VerificationProcesses
    Restore-VerificationDevice
}

Write-Host "Verification result: $(if ($exitCode -eq 0) { 'PASS' } elseif ($exitCode -eq 124) { 'TIMEOUT' } else { 'FAIL' })"
Write-Host "Run logs: $runLogRoot"
exit $exitCode
