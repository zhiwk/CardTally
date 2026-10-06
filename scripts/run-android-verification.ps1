param(
    [int]$BuildTimeoutMinutes = 10,
    [int]$UnitTestTimeoutMinutes = 5,
    [int]$DeviceTestTimeoutMinutes = 15,
    [ValidateRange(1, 60)][int]$DeviceGroupTimeoutSeconds = 60,
    [switch]$SkipDebugBuild,
    [switch]$SkipUnitTests,
    [switch]$SkipDeviceTests,
    [string[]]$DeviceGroups
)

$ErrorActionPreference = 'Stop'

$root = Split-Path $PSScriptRoot -Parent
$gradle = Join-Path $root 'gradlew.bat'
$logRoot = Join-Path $root 'app/build/reports/verification'
$runId = Get-Date -Format 'yyyyMMdd-HHmmss'
$runLogRoot = Join-Path $logRoot $runId
$script:originalScreenOffTimeout = $null
$script:adbInvocation = 0
$resultsRoot = Join-Path $root 'app/build/outputs/androidTest-results/connected/debug/flavors/verification'

if (-not (Test-Path -LiteralPath $gradle)) {
    throw "Gradle wrapper not found: $gradle"
}
if ($DeviceGroupTimeoutSeconds -lt 1 -or $DeviceTestTimeoutMinutes -lt 1) {
    throw 'Device group timeout and total device timeout must both be positive.'
}
New-Item -ItemType Directory -Path $runLogRoot -Force | Out-Null
. (Join-Path $PSScriptRoot 'verification-device-groups.ps1')

$selectedDeviceGroups = @()
if ($SkipDeviceTests -and $DeviceGroups.Count -gt 0) {
    throw 'Use either -SkipDeviceTests or -DeviceGroups, not both.'
}
if (-not $SkipDeviceTests) {
    if ($DeviceGroups.Count -eq 0) {
        $selectedDeviceGroups = @($deviceTestGroups)
    } else {
        $unknownGroups = @($DeviceGroups | Where-Object { $_ -notin @($deviceTestGroups.Name) })
        if ($unknownGroups.Count -gt 0) {
            throw "Unknown device group(s): $($unknownGroups -join ', '). Available groups: $($deviceTestGroups.Name -join ', ')"
        }
        $duplicateGroups = @($DeviceGroups | Group-Object | Where-Object { $_.Count -gt 1 })
        if ($duplicateGroups.Count -gt 0) {
            throw "Duplicate device group(s): $($duplicateGroups.Name -join ', ')"
        }
        $selectedDeviceGroups = @($deviceTestGroups | Where-Object { $_.Name -in $DeviceGroups })
    }
}
$expectedSelectedDeviceTests = [int](($selectedDeviceGroups | Measure-Object -Property Expected -Sum).Sum)
$runDeviceTests = $selectedDeviceGroups.Count -gt 0
$script:deviceTouched = $false
if ($SkipDebugBuild -and $SkipUnitTests -and -not $runDeviceTests) {
    throw 'No verification stages selected.'
}
Write-Host "Plan: debug-build=$(-not $SkipDebugBuild); unit-tests=$(-not $SkipUnitTests); device-groups=$($selectedDeviceGroups.Name -join ',')"

function Assert-DeviceGroupCoverage {
    $testRoot = Join-Path $root 'app/src/androidTest/java'
    $discovered = @(Get-ChildItem -LiteralPath $testRoot -Recurse -Filter '*Test.kt' | ForEach-Object {
        $relative = $_.FullName.Substring($testRoot.Length + 1)
        $relative.Substring(0, $relative.Length - 3).Replace('\', '.')
    })
    $configured = @($deviceTestGroups | ForEach-Object { $_.Classes })
    $duplicate = @($configured | Group-Object | Where-Object { $_.Count -ne 1 })
    $difference = @(Compare-Object -ReferenceObject $discovered -DifferenceObject $configured)
    if ($duplicate.Count -gt 0 -or $difference.Count -gt 0) {
        throw "Device test groups do not cover each androidTest class exactly once. Duplicates: $($duplicate.Name -join ', '); differences: $($difference.InputObject -join ', ')"
    }
    if (($deviceTestGroups | ForEach-Object { $_.Expected } | Measure-Object -Sum).Sum -ne 149) {
        throw 'Expected device test counts must total 149; review the group manifest.'
    }
}

function Stop-ProcessTree([int]$ProcessId) {
    if (Get-Process -Id $ProcessId -ErrorAction SilentlyContinue) {
        & taskkill.exe /PID $ProcessId /T /F 2>$null | Out-Null
    }
}

function Invoke-BoundedAdb([string[]]$Arguments, [int]$TimeoutSeconds = 5) {
    $script:adbInvocation++
    $stdout = Join-Path $runLogRoot "adb-$($script:adbInvocation).stdout.log"
    $stderr = Join-Path $runLogRoot "adb-$($script:adbInvocation).stderr.log"
    $process = Start-Process -FilePath 'adb.exe' -ArgumentList $Arguments -WorkingDirectory $root -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
        Stop-ProcessTree $process.Id
        return $null
    }
    $process.Refresh()
    if ($process.ExitCode -ne 0) { return $null }
    return [System.IO.File]::ReadAllText($stdout).Trim()
}

function Stop-VerificationProcesses {
    # Do not touch the daily or release application sandboxes.
    Invoke-BoundedAdb @('shell', 'am', 'force-stop', 'com.example.cardtally.verification') | Out-Null
    Invoke-BoundedAdb @('shell', 'am', 'force-stop', 'com.example.cardtally.verification.test') | Out-Null
}

function Prepare-VerificationDevice {
    $screenTimeout = Invoke-BoundedAdb @('shell', 'settings', 'get', 'system', 'screen_off_timeout')
    if ($screenTimeout -match '^\d+$') {
        $script:originalScreenOffTimeout = $screenTimeout
        # The complete instrumentation suite can run longer than common phone
        # display timeouts. Keep the screen awake so ActivityScenario tests do
        # not resume after Android has saved the host activity state.
        Invoke-BoundedAdb @('shell', 'settings', 'put', 'system', 'screen_off_timeout', '1800000') | Out-Null
    }
    Invoke-BoundedAdb @('shell', 'input', 'keyevent', '224') | Out-Null
    Start-Sleep -Seconds 1
}

function Restore-VerificationDevice {
    if ($script:originalScreenOffTimeout -match '^\d+$') {
        Invoke-BoundedAdb @('shell', 'settings', 'put', 'system', 'screen_off_timeout', $script:originalScreenOffTimeout) | Out-Null
        $script:originalScreenOffTimeout = $null
    }
}

function Invoke-Stage([string]$Name, [string[]]$Arguments, [int]$TimeoutSeconds) {
    $stdout = Join-Path $runLogRoot "$Name.stdout.log"
    $stderr = Join-Path $runLogRoot "$Name.stderr.log"
    Write-Host "START: $Name (timeout ${TimeoutSeconds}s)"

    # Start through cmd.exe because PowerShell cannot reliably expose the exit
    # code of a .bat file directly.
    $cmdArguments = @('/d', '/c', 'call', ('"' + $gradle + '"')) + $Arguments
    $process = Start-Process -FilePath $env:ComSpec -ArgumentList $cmdArguments -WorkingDirectory $root -RedirectStandardOutput $stdout -RedirectStandardError $stderr -PassThru
    try {
        $finished = $process.WaitForExit($TimeoutSeconds * 1000)
        if (-not $finished) {
            if ($Name -like 'device-*') { Save-DeviceTimeoutDiagnostics $Name $stdout }
            Stop-ProcessTree $process.Id
            if ($script:deviceTouched) { Stop-VerificationProcesses }
            Write-Host "TIMEOUT: $Name after ${TimeoutSeconds}s"
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

function Save-DeviceTimeoutDiagnostics([string]$Name, [string]$Stdout) {
    $output = Join-Path $runLogRoot "$Name.diagnostics.log"
    $lines = @(Get-Content -LiteralPath $Stdout -ErrorAction SilentlyContinue)
    $completed = @($lines | ForEach-Object { [regex]::Replace($_, '\x1B\[[0-9;]*m', '') } |
        Where-Object { $_ -match '^com\.example\.cardtally\..* > .* (SUCCESS|FAILED)' })
    $last = if ($completed.Count -gt 0) { $completed[-1] } else { '(none reported by UTP)' }
    $power = Invoke-BoundedAdb @('shell', 'dumpsys', 'power')
    $wakefulness = @(($power -split '\r?\n') | Where-Object { $_ -match 'mWakefulness=' }) -join '; '
    if (-not $wakefulness) { $wakefulness = '(unavailable within ADB timeout)' }
    @(
        "Group: $Name",
        "Last completed: $last",
        "Verification app PID: $(Invoke-BoundedAdb @('shell', 'pidof', 'com.example.cardtally.verification'))",
        "Test PID: $(Invoke-BoundedAdb @('shell', 'pidof', 'com.example.cardtally.verification.test'))",
        "Screen: $wakefulness"
    ) | Set-Content -LiteralPath $output -Encoding UTF8
    Write-Host "Diagnostics: $output"
}

function Assert-DeviceGroupResult($Group, [datetime]$StartedAt) {
    $result = @(Get-ChildItem -LiteralPath $resultsRoot -Filter 'TEST-*.xml' -ErrorAction SilentlyContinue |
        Where-Object { $_.LastWriteTimeUtc -ge $StartedAt.ToUniversalTime().AddSeconds(-2) })
    if ($result.Count -ne 1) { throw "Expected one fresh XML report for $($Group.Name), found $($result.Count)." }
    [xml]$report = [System.IO.File]::ReadAllText($result[0].FullName)
    $suite = $report.testsuite
    $count = [int]$suite.tests
    $classNames = @($suite.testcase | ForEach-Object { [string]$_.classname } | Sort-Object -Unique)
    $unexpected = @($classNames | Where-Object { $_ -notin $Group.Classes })
    if ($count -ne $Group.Expected -or [int]$suite.failures -ne 0 -or
        [int]$suite.errors -ne 0 -or [int]$suite.skipped -ne 0 -or $unexpected.Count -gt 0) {
        throw "Incomplete $($Group.Name): tests=$count/$($Group.Expected), failures=$($suite.failures), errors=$($suite.errors), skipped=$($suite.skipped), unexpected=$($unexpected -join ', ')."
    }
    return $count
}

function Test-TransientDeviceConnectionFailure([string]$Name) {
    $stdout = [System.IO.File]::ReadAllText((Join-Path $runLogRoot "$Name.stdout.log"))
    $stderr = [System.IO.File]::ReadAllText((Join-Path $runLogRoot "$Name.stderr.log"))
    $zeroTests = $stdout -match 'Starting\s+0\s+tests' -or $stdout -notmatch 'Starting\s+\d+\s+tests'
    return ($zeroTests -and
        ($stdout + "`n" + $stderr) -match 'Connection timed out: connect|device offline|device disconnected|Connection reset|Instrumentation run failed due to Process crashed')
}

if ($runDeviceTests) {
    Assert-DeviceGroupCoverage
}
$stages = @()
if (-not $SkipDebugBuild) {
    $stages += @{ Name = 'assemble-debug'; Args = @(':app:assembleEverydayDebug', '--no-daemon', '--console=plain'); Timeout = $BuildTimeoutMinutes * 60 }
}
if (-not $SkipUnitTests) {
    $stages += @{ Name = 'unit-test'; Args = @(':app:testEverydayUnitTest', '--no-daemon', '--console=plain'); Timeout = $UnitTestTimeoutMinutes * 60 }
}
if ($runDeviceTests) {
    $stages += @{ Name = 'assemble-verification-tests'; Args = @(':app:assembleVerificationDebugAndroidTest', '--no-daemon', '--console=plain'); Timeout = $BuildTimeoutMinutes * 60 }
}

$exitCode = 0
try {
    foreach ($stage in $stages) {
        $exitCode = Invoke-Stage $stage.Name $stage.Args $stage.Timeout
        if ($exitCode -ne 0) { break }
    }
    if ($exitCode -eq 0 -and $runDeviceTests) {
        $script:deviceTouched = $true
        Prepare-VerificationDevice
        $deadline = (Get-Date).AddMinutes($DeviceTestTimeoutMinutes)
        $completedCount = 0
        foreach ($group in $selectedDeviceGroups) {
            $started = Get-Date
            for ($attempt = 1; $attempt -le 2; $attempt++) {
                $remaining = [int][Math]::Ceiling(($deadline - (Get-Date)).TotalSeconds)
                if ($remaining -le 0) { $exitCode = 124; break }
                $groupTimeout = if ($group.ContainsKey('TimeoutSeconds')) {
                    [int]$group.TimeoutSeconds
                } else {
                    $DeviceGroupTimeoutSeconds
                }
                if ($groupTimeout -lt 1 -or $groupTimeout -gt 600) {
                    throw "Invalid timeout for device group $($group.Name): $groupTimeout seconds."
                }
                $timeout = [Math]::Min($groupTimeout, $remaining)
                $name = "device-$($group.Name)" + $(if ($attempt -gt 1) { '-retry' } else { '' })
                $started = Get-Date
                $exitCode = Invoke-Stage $name @(
                    ':app:connectedVerificationDebugAndroidTest',
                    "-Pandroid.testInstrumentationRunnerArguments.class=$($group.Classes -join ',')",
                    '--console=plain', '--info'
                ) $timeout
                if ($exitCode -eq 0) { break }
                if ($attempt -eq 2 -or $exitCode -eq 124 -or -not (Test-TransientDeviceConnectionFailure $name)) { break }
                Write-Host "RETRY: $($group.Name), device/instrumentation failed before any test started"
                Stop-VerificationProcesses
                Start-Sleep -Seconds 3
                Invoke-BoundedAdb @('devices') | Out-Null
            }
            if ($exitCode -ne 0) { break }
            try {
                $completedCount += Assert-DeviceGroupResult $group $started
            } catch {
                Write-Host "FAIL: device-$($group.Name): $($_.Exception.Message)"
                $exitCode = 1
                break
            }
            Write-Host "PASS: device-$($group.Name) ($($group.Expected) tests)"
        }
        if ($exitCode -eq 0 -and $completedCount -ne $expectedSelectedDeviceTests) {
            Write-Host "FAIL: selected device tests completed $completedCount/$expectedSelectedDeviceTests"
            $exitCode = 1
        } elseif ($exitCode -eq 0) {
            Write-Host "PASS: selected device suite $completedCount/$expectedSelectedDeviceTests"
        }
    }
} finally {
    if ($script:deviceTouched) {
        Stop-VerificationProcesses
        Restore-VerificationDevice
    }
}

Write-Host "Verification result: $(if ($exitCode -eq 0) { 'PASS' } elseif ($exitCode -eq 124) { 'TIMEOUT' } else { 'FAIL' })"
Write-Host "Run logs: $runLogRoot"
exit $exitCode
