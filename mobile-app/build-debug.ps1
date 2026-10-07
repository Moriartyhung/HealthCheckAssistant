# HealthCheckAndroid - build both Debug APKs (Windows PowerShell)
# Usage (in this folder):  powershell -ExecutionPolicy Bypass -File .\build-debug.ps1
# Logs are written to .\build-logs\ so the build output can be reviewed afterwards.

$ErrorActionPreference = "Continue"
$root = $PSScriptRoot
$logDir = Join-Path $root "build-logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$envLog = Join-Path $logDir "env.txt"
"=== $(Get-Date -Format s) ===" | Out-File $envLog -Encoding utf8

function Log($m) { Write-Host $m; $m | Out-File $envLog -Append -Encoding utf8 }

# ---- Environment inventory ----
$os = Get-CimInstance Win32_OperatingSystem
Log "Windows: $($os.Caption) $($os.Version) $($os.OSArchitecture)"
Log "CPU: $((Get-CimInstance Win32_Processor | Select-Object -First 1).Name)"
Log "RAM GB: $([math]::Round($os.TotalVisibleMemorySize/1MB,1))"
Log "Free disk C: GB: $([math]::Round((Get-PSDrive C).Free/1GB,1))"
try { Log "Virtualization (firmware): $((Get-CimInstance Win32_Processor | Select-Object -First 1).VirtualizationFirmwareEnabled)" } catch {}
try { Log "Hypervisor present: $((Get-CimInstance Win32_ComputerSystem).HypervisorPresent)" } catch {}
$git = Get-Command git -ErrorAction SilentlyContinue
if ($git) { Log "Git: $(& git --version)" } else { Log "Git: NOT FOUND" }

# ---- Diagnostics: Android Studio install folder ----
$asDir = "$env:ProgramFiles\Android\Android Studio"
if (Test-Path $asDir) { Log "Android Studio dir: $((Get-ChildItem $asDir | ForEach-Object Name) -join ', ')" } else { Log "Android Studio dir: NOT FOUND" }
if (Test-Path "$asDir\product-info.json") { try { $pi = Get-Content "$asDir\product-info.json" -Raw | ConvertFrom-Json; Log "Android Studio: $($pi.name) $($pi.version) build $($pi.buildNumber)" } catch {} }

$ProgressPreference = "SilentlyContinue"
$toolsDir = Join-Path $env:LOCALAPPDATA "HealthCheckTools"

function Get-JdkMajor($home_) {
  $rel = Join-Path $home_ "release"
  if (Test-Path $rel) {
    $line = Select-String -Path $rel -Pattern '^JAVA_VERSION="?([0-9]+)' | Select-Object -First 1
    if ($line) { return [int]$line.Matches[0].Groups[1].Value }
  }
  return 0
}

# ---- JDK 17+ (prefer Android Studio bundled JBR, then any existing JDK, else portable Temurin 21) ----
$jdkCandidates = @("$asDir\jbr", "$asDir\jre", "$env:LOCALAPPDATA\Programs\Android Studio\jbr", $env:JAVA_HOME)
foreach ($pat in @("$env:ProgramFiles\Java\*", "$env:ProgramFiles\Eclipse Adoptium\*", "$env:ProgramFiles\Microsoft\jdk-*",
                   "$env:ProgramFiles\Zulu\*", "$env:ProgramFiles\Amazon Corretto\*", "$env:USERPROFILE\.jdks\*",
                   "$env:USERPROFILE\.gradle\jdks\*", "$toolsDir\jdk*")) {
  $jdkCandidates += @(Get-ChildItem $pat -Directory -ErrorAction SilentlyContinue | ForEach-Object FullName)
}
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if ($javaCmd) { $jdkCandidates += (Split-Path (Split-Path $javaCmd.Source)) }
$jdk = $null
foreach ($c in $jdkCandidates) {
  if ($c -and (Test-Path (Join-Path $c "bin\javac.exe"))) {
    $m = Get-JdkMajor $c
    Log "JDK candidate: $c (major=$m)"
    if ($m -ge 17 -and -not $jdk) { $jdk = $c }
  }
}
if (-not $jdk) {
  Log "No JDK 17+ found. Downloading portable Eclipse Temurin JDK 21 (per-user, no admin) to $toolsDir ..."
  New-Item -ItemType Directory -Force -Path $toolsDir | Out-Null
  $zip = Join-Path $toolsDir "temurin21.zip"
  try {
    Invoke-WebRequest -UseBasicParsing -Uri "https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse" -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $toolsDir -Force
    Remove-Item $zip -Force
    $jdk = (Get-ChildItem "$toolsDir\jdk*" -Directory | Select-Object -First 1).FullName
  } catch { Log "ERROR: JDK download failed: $($_.Exception.Message)" }
}
if (-not $jdk) { Log "ERROR: No usable JDK."; exit 1 }
$env:JAVA_HOME = $jdk
$env:Path = "$jdk\bin;$env:Path"
Log "JAVA_HOME: $env:JAVA_HOME"
Log "Java: $((& "$jdk\bin\java.exe" -version 2>&1) -join ' | ')"

# ---- Android SDK (existing, else install cmdline-tools + required packages per-user) ----
$sdkCandidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, "$env:LOCALAPPDATA\Android\Sdk") |
  Where-Object { $_ -and (Test-Path $_) }
$sdk = if ($sdkCandidates) { @($sdkCandidates)[0] } else { "$env:LOCALAPPDATA\Android\Sdk" }
New-Item -ItemType Directory -Force -Path $sdk | Out-Null
$env:ANDROID_HOME = $sdk
Log "ANDROID_HOME: $sdk"

$sdkmanager = @(Get-ChildItem "$sdk\cmdline-tools\*\bin\sdkmanager.bat" -ErrorAction SilentlyContinue | ForEach-Object FullName) | Select-Object -First 1
if (-not $sdkmanager) {
  Log "cmdline-tools not found. Installing Android SDK Command-line Tools ..."
  try {
    $repo = (Invoke-WebRequest -UseBasicParsing -Uri "https://dl.google.com/android/repository/repository2-3.xml").Content
    $name = ([regex]::Matches($repo, 'commandlinetools-win-(\d+)_latest\.zip') | Sort-Object { [long]$_.Groups[1].Value } | Select-Object -Last 1).Value
    Log "cmdline-tools package: $name"
    $zip = Join-Path $env:TEMP $name
    Invoke-WebRequest -UseBasicParsing -Uri "https://dl.google.com/android/repository/$name" -OutFile $zip
    $tmp = Join-Path $env:TEMP "cmdline-tools-extract"
    if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
    Expand-Archive -Path $zip -DestinationPath $tmp -Force
    New-Item -ItemType Directory -Force -Path "$sdk\cmdline-tools" | Out-Null
    if (Test-Path "$sdk\cmdline-tools\latest") { Remove-Item "$sdk\cmdline-tools\latest" -Recurse -Force }
    Move-Item "$tmp\cmdline-tools" "$sdk\cmdline-tools\latest"
    Remove-Item $zip -Force
    $sdkmanager = "$sdk\cmdline-tools\latest\bin\sdkmanager.bat"
  } catch { Log "ERROR: cmdline-tools install failed: $($_.Exception.Message)" }
}
if ($sdkmanager) {
  Log "sdkmanager: $sdkmanager"
  Write-Host ""
  Write-Host ">>> Android SDK licenses: if asked, read and type y to accept. <<<" -ForegroundColor Yellow
  & $sdkmanager --sdk_root="$sdk" --licenses
  Log "Installing/verifying SDK packages: platform-tools, platforms;android-36, build-tools;36.0.0, emulator ..."
  & $sdkmanager --sdk_root="$sdk" "platform-tools" "platforms;android-36" "build-tools;36.0.0" "emulator" 2>&1 | Select-Object -Last 5 | ForEach-Object { Log "  $_" }
}
Log "SDK platforms: $((Get-ChildItem "$sdk\platforms" -ErrorAction SilentlyContinue | ForEach-Object Name) -join ', ')"
Log "SDK build-tools: $((Get-ChildItem "$sdk\build-tools" -ErrorAction SilentlyContinue | ForEach-Object Name) -join ', ')"
Log "SDK system-images: $((Get-ChildItem "$sdk\system-images" -Recurse -Depth 2 -Directory -ErrorAction SilentlyContinue | ForEach-Object FullName) -join ' ; ')"
Log "SDK licenses: $((Get-ChildItem "$sdk\licenses" -ErrorAction SilentlyContinue | ForEach-Object Name) -join ', ')"
$adb = Join-Path $sdk "platform-tools\adb.exe"
if (Test-Path $adb) { Log "adb: $((& $adb version | Select-Object -First 1))"; Log "adb devices: $((& $adb devices) -join ' | ')" } else { Log "adb: NOT FOUND" }
$emu = Join-Path $sdk "emulator\emulator.exe"
if (Test-Path $emu) { Log "AVDs: $((& $emu -list-avds) -join ', ')" } else { Log "emulator: NOT FOUND" }

# ---- Map project to an ASCII-only drive letter (AGP dislikes non-ASCII paths on Windows) ----
$drive = $null
foreach ($d in @("X:", "Y:", "W:", "V:")) {
  if (-not (Test-Path "$d\")) { subst $d "$root" 2>$null; if (Test-Path "$d\") { $drive = $d; break } }
}
$base = if ($drive) { "$drive\" } else { "$root\" }
Log "Build base path: $base"

$results = @()
foreach ($app in @("HealthCheckPatientAndroid", "HealthCheckStaffAndroid")) {
  $proj = Join-Path $base $app
  # local.properties (machine-specific, git-ignored)
  "sdk.dir=$($sdk -replace '\\','\\')" | Out-File (Join-Path $proj "local.properties") -Encoding ascii
  $log = Join-Path $logDir "$app.log"
  Log "---- Building $app (log: $log) ----"
  Push-Location $proj
  & .\gradlew.bat assembleDebug --no-daemon --stacktrace --console=plain *> $log
  $code = $LASTEXITCODE
  Pop-Location
  $apk = Join-Path $root "$app\app\build\outputs\apk\debug\app-debug.apk"
  $ok = ($code -eq 0) -and (Test-Path $apk)
  Log "$app exit=$code apk_exists=$(Test-Path $apk)"
  $results += "$app : $(if ($ok) {'SUCCESS'} else {'FAILED'}) -> $apk"
}

if ($drive) { subst $drive /D 2>$null }
Log "==== RESULT ===="
$results | ForEach-Object { Log $_ }
"DONE $(Get-Date -Format s)" | Out-File (Join-Path $logDir "done.txt") -Encoding utf8
