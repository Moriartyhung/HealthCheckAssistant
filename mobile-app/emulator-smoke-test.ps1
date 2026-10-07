# Optional: boot the existing AVD, install both Debug APKs, launch them and capture screenshots.
# Usage: powershell -ExecutionPolicy Bypass -File .\emulator-smoke-test.ps1
$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
$logDir = Join-Path $root "build-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$log = Join-Path $logDir "emulator.txt"
function Log($m) { Write-Host $m; $m | Out-File $log -Append -Encoding utf8 }
"=== $(Get-Date -Format s) ===" | Out-File $log -Encoding utf8

$sdk = @($env:ANDROID_HOME, "$env:LOCALAPPDATA\Android\Sdk") | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1
$adb = "$sdk\platform-tools\adb.exe"; $emu = "$sdk\emulator\emulator.exe"
$avd = (& $emu -list-avds | Select-Object -First 1)
if (-not $avd) { Log "No AVD found"; exit 1 }
Log "Starting AVD: $avd"
Log "accel-check: $((& $emu -accel-check 2>&1) -join ' | ')"
Start-Process -FilePath $emu -ArgumentList @("-avd", $avd, "-no-snapshot-save", "-no-audio") -RedirectStandardOutput (Join-Path $logDir "emulator-stdout.txt") -RedirectStandardError (Join-Path $logDir "emulator-stderr.txt")
& $adb wait-for-device
$booted = $false
for ($i = 0; $i -lt 90; $i++) {
  if ((& $adb shell getprop sys.boot_completed 2>$null) -match "1") { $booted = $true; break }
  Start-Sleep -Seconds 4
}
Log "boot_completed: $booted"
Log "adb devices: $((& $adb devices) -join ' | ')"
if (-not $booted) { exit 1 }
& $adb shell input keyevent 82 | Out-Null

$apps = @(
  @{ name = "patient"; apk = "$root\HealthCheckPatientAndroid\app\build\outputs\apk\debug\app-debug.apk"; pkg = "com.healthcheck.patient" },
  @{ name = "staff";   apk = "$root\HealthCheckStaffAndroid\app\build\outputs\apk\debug\app-debug.apk";   pkg = "com.healthcheck.staff" }
)
foreach ($a in $apps) {
  Log "install $($a.name): $((& $adb install -r -t $a.apk 2>&1) -join ' | ')"
  & $adb logcat -c
  & $adb shell am start -W -n "$($a.pkg)/.MainActivity" 2>&1 | ForEach-Object { Log "  $_" }
  Start-Sleep -Seconds 6
  $pid_ = (& $adb shell pidof $a.pkg)
  Log "$($a.name) running pid: $pid_"
  & $adb shell screencap -p /sdcard/shot.png
  & $adb pull /sdcard/shot.png (Join-Path $logDir "screen-$($a.name).png") | Out-Null
  & $adb logcat -d -s AndroidRuntime:E 2>&1 | Out-File (Join-Path $logDir "crash-$($a.name).txt") -Encoding utf8
}
Log "DONE"
