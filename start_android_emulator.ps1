# start_android_emulator.ps1 - clean stop/unlock/boot/install/launch for Video Poker Android.
# Usage: .\start_android_emulator.ps1 [-NoPauseOnError]
param(
    [switch]$NoPauseOnError
)

$ErrorActionPreference = 'Stop'

$RepoDir  = $PSScriptRoot
$AvdName  = 'Medium_Phone_API_37.0'
$AvdDir   = Join-Path $env:USERPROFILE '.android\avd\Medium_Phone.avd'
$SdkDir   = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
$Adb      = Join-Path $SdkDir 'platform-tools\adb.exe'
$Emulator = Join-Path $SdkDir 'emulator\emulator.exe'
$ApkPath  = Join-Path $RepoDir 'app\build\outputs\apk\debug\app-debug.apk'
$GradleFile = Join-Path $RepoDir 'app\build.gradle.kts'
$PkgName  = 'com.videopoker.android'
$OutLog   = Join-Path $env:TEMP 'vp_android_emulator_stdout.log'
$ErrLog   = Join-Path $env:TEMP 'vp_android_emulator_stderr.log'

function Fail($msg) {
    Write-Host "ERROR: $msg" -ForegroundColor Red
    if (-not $NoPauseOnError) { Read-Host "Press Enter to close" | Out-Null }
    exit 1
}

function Invoke-AdbQuiet {
    param([string[]]$AdbArgs)
    $prev = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $out = & $Adb @AdbArgs 2>&1
    $ErrorActionPreference = $prev
    return $out
}

if (-not (Test-Path $Adb))      { Fail "adb.exe not found at $Adb" }
if (-not (Test-Path $Emulator)) { Fail "emulator.exe not found at $Emulator" }

# 1. Stop any running/hung emulator or QEMU instances cleanly.
$existing = @(Get-Process -Name 'emulator', 'qemu-system-x86_64' -ErrorAction SilentlyContinue)
if ($existing.Count -gt 0) {
    Write-Host "Stopping existing Android emulator..."
    $null = Invoke-AdbQuiet @('-s', 'emulator-5554', 'emu', 'kill')
    for ($i = 0; $i -lt 10; $i++) {
        Start-Sleep -Milliseconds 500
        if (@(Get-Process -Name 'emulator', 'qemu-system-x86_64' -ErrorAction SilentlyContinue).Count -eq 0) { break }
    }
    Get-Process -Name 'qemu-system-x86_64', 'emulator' -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    Start-Sleep -Seconds 1
}

# 2. Reset ADB server so no stale daemon state or hung session remains.
Write-Host "Resetting ADB server..."
$null = Invoke-AdbQuiet @('kill-server')
Start-Sleep -Milliseconds 500
$null = Invoke-AdbQuiet @('start-server')

# 3. Clean up any stale AVD lock files/folders left behind after crashes/reboots.
if (Test-Path $AvdDir) {
    $locks = @(Get-ChildItem -Path $AvdDir -Filter '*.lock' -Force -ErrorAction SilentlyContinue)
    foreach ($l in $locks) {
        Write-Host "Removing stale lock: $($l.Name)"
        Remove-Item -Path $l.FullName -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# 4. Cold-boot the visible desktop emulator window (-no-snapshot-load prevents snapshot hangs & stale package sessions).
#    Redirect stdout/stderr to %TEMP% logs so emulator.exe does not keep the launching PowerShell window open.
Remove-Item $OutLog, $ErrLog -Force -ErrorAction SilentlyContinue
Write-Host "Launching Android emulator ($AvdName)..."
$emuProc = Start-Process -FilePath $Emulator -ArgumentList @('-avd', $AvdName, '-no-snapshot-load') -RedirectStandardOutput $OutLog -RedirectStandardError $ErrLog -WindowStyle Normal -PassThru

# 5. Wait for Android OS boot to complete cleanly.
Write-Host "Waiting for Android phone to finish booting (usually 15-30 seconds)..."
$booted = $false
$deadline = (Get-Date).AddSeconds(120)
while ((Get-Date) -lt $deadline) {
    if ($emuProc.HasExited) {
        Fail "Android emulator exited unexpectedly (code $($emuProc.ExitCode))."
    }
    $devs = (& $Adb devices 2>$null) -join "`n"
    if ($devs -match 'emulator-5554\s+device') {
        $prop = (& $Adb -s emulator-5554 shell getprop sys.boot_completed 2>$null)
        if ($prop -and $prop.Trim() -eq '1') {
            $booted = $true
            break
        }
    }
    Start-Sleep -Seconds 2
}
if (-not $booted) { Fail "Android emulator did not finish booting within 120 seconds." }

Start-Sleep -Seconds 2
& $Adb -s emulator-5554 shell wm dismiss-keyguard 2>$null | Out-Null

# 6. Install built APK if missing or older than local build.gradle.kts versionCode.
if (Test-Path $ApkPath) {
    $targetVc = $null
    if (Test-Path $GradleFile) {
        $m = Select-String -Path $GradleFile -Pattern 'versionCode\s*=\s*(\d+)' | Select-Object -First 1
        if ($m) { $targetVc = $m.Matches[0].Groups[1].Value }
    }
    $pkgDump = (& $Adb -s emulator-5554 shell dumpsys package $PkgName 2>$null) -join "`n"
    $installedVc = $null
    if ($pkgDump -match 'versionCode=(\d+)') { $installedVc = $Matches[1] }

    if (-not $installedVc -or ($targetVc -and $installedVc -ne $targetVc)) {
        Write-Host "Installing Video Poker Android APK (versionCode=$targetVc)..."
        & $Adb -s emulator-5554 install -r --no-streaming $ApkPath
        if ($LASTEXITCODE -ne 0) { Fail "Failed to install APK onto emulator-5554." }
    } else {
        Write-Host "Video Poker Android (versionCode=$installedVc) is already up to date."
    }
} else {
    Write-Host "Note: built APK not found at $ApkPath; launching installed app." -ForegroundColor Yellow
}

# 7. Bring Video Poker Android straight to the foreground.
Write-Host "Launching Video Poker Android..."
& $Adb -s emulator-5554 shell am start -n "$PkgName/.MainActivity" | Out-Null
Write-Host "Video Poker Android is running on $AvdName." -ForegroundColor Green
Start-Sleep -Seconds 2
exit 0
