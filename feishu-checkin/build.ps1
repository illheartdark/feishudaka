<#
.SYNOPSIS
    Offline Android APK build for the check-in assistant (no Gradle required).
.DESCRIPTION
    Pipeline: aapt2 compile -> aapt2 link -> javac -> d8 -> jar (add classes.dex)
              -> zipalign -> apksigner.
    NOTE: kept ASCII-only on purpose; Windows PowerShell 5.1 reads BOM-less UTF-8
          files as GBK and would break on non-ASCII literals.
#>
param(
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$sdk = if ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$buildTools = Join-Path $sdk 'build-tools\35.0.0'
$androidJar = Join-Path $sdk 'platforms\android-35\android.jar'

if (-not (Test-Path $androidJar)) { throw "android.jar not found: $androidJar" }
if (-not (Test-Path $buildTools)) { throw "build-tools not found: $buildTools" }

$aapt2 = Join-Path $buildTools 'aapt2.exe'
$d8 = Join-Path $buildTools 'd8.bat'
$zipalign = Join-Path $buildTools 'zipalign.exe'
$apksigner = Join-Path $buildTools 'apksigner.bat'

$srcDir = Join-Path $root 'app\src\main\java'
$resDir = Join-Path $root 'app\src\main\res'
$manifest = Join-Path $root 'app\src\main\AndroidManifest.xml'
$buildDir = Join-Path $root 'build'
$realiseDir = Join-Path (Split-Path $root -Parent) 'realise'
$classesDir = Join-Path $buildDir 'classes'
$dexDir = Join-Path $buildDir 'dex'
$flatDir = Join-Path $buildDir 'flat'

if ($Clean -and (Test-Path $buildDir)) { Remove-Item $buildDir -Recurse -Force }
foreach ($d in @($buildDir, $classesDir, $dexDir, $flatDir)) {
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}

Write-Host '[1/7] aapt2 compile resources'
& $aapt2 compile --dir $resDir -o $flatDir
if ($LASTEXITCODE -ne 0) { throw 'aapt2 compile failed' }

$flatFiles = @(Get-ChildItem $flatDir -Filter *.flat | ForEach-Object { $_.FullName })
if ($flatFiles.Count -eq 0) { throw 'no compiled resources' }

Write-Host '[2/7] aapt2 link'
$unsignedApk = Join-Path $buildDir 'app.unsigned.apk'
& $aapt2 link -o $unsignedApk -I $androidJar --manifest $manifest --min-sdk-version 26 --target-sdk-version 35 --auto-add-overlay @flatFiles
if ($LASTEXITCODE -ne 0) { throw 'aapt2 link failed' }

Write-Host '[3/7] javac'
$sources = @(Get-ChildItem $srcDir -Recurse -Filter *.java | ForEach-Object { $_.FullName })
& javac -source 8 -target 8 -encoding UTF-8 -nowarn -classpath $androidJar -d $classesDir @sources
if ($LASTEXITCODE -ne 0) { throw 'javac failed' }

Write-Host '[4/7] d8'
$classFiles = @(Get-ChildItem $classesDir -Recurse -Filter *.class | ForEach-Object { $_.FullName })
& $d8 --lib $androidJar --min-api 26 --output $dexDir @classFiles
if ($LASTEXITCODE -ne 0) { throw 'd8 failed' }

Write-Host '[5/7] add classes.dex'
Copy-Item (Join-Path $dexDir 'classes.dex') (Join-Path $buildDir 'classes.dex') -Force
Push-Location $buildDir
try {
    & jar uf $unsignedApk 'classes.dex'
    if ($LASTEXITCODE -ne 0) { throw 'jar failed' }
} finally {
    Pop-Location
}

Write-Host '[6/7] zipalign'
$alignedApk = Join-Path $buildDir 'app.aligned.apk'
& $zipalign -p -f 4 $unsignedApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw 'zipalign failed' }

Write-Host '[7/7] apksigner'
$keystore = Join-Path $root 'debug.keystore'
if (-not (Test-Path $keystore)) {
    & keytool -genkeypair -keystore $keystore -storepass android -keypass android -alias daka -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Daka Assistant, OU=Dev, O=Personal, L=Beijing, ST=Beijing, C=CN'
    if ($LASTEXITCODE -ne 0) { throw 'keytool failed' }
}
# Version comes from AndroidManifest.xml so the file name always matches the build.
$manifestText = Get-Content $manifest -Raw -Encoding UTF8
$verMatch = [regex]::Match($manifestText, 'versionName="([^"]+)"')
$version = if ($verMatch.Success) { $verMatch.Groups[1].Value } else { 'dev' }
# ASCII-only source: build the Chinese product name from code points (avoid PS 5.1 GBK parsing issue).
$baseName = [string]([char]0x98DE) + [char]0x4E66 + [char]0x6253 + [char]0x5361
$finalApk = Join-Path $realiseDir ("$baseName-v$version.apk")
& $apksigner sign --ks $keystore --ks-pass pass:android --key-pass pass:android --out $finalApk $alignedApk
if ($LASTEXITCODE -ne 0) { throw 'apksigner failed' }

# 规则 15：APK 只能存放在 realise；清理 build 下的中间 APK 与 v4 签名副产物
# apksigner.bat 退出后句柄可能尚未释放，直接删除会静默失败，因此重试若干次
for ($i = 0; $i -lt 6; $i++) {
    Get-ChildItem -Path $buildDir -Filter *.apk -ErrorAction SilentlyContinue |
        ForEach-Object { try { Remove-Item $_.FullName -Force -ErrorAction Stop } catch { } }
    Remove-Item ($finalApk + '.idsig') -Force -ErrorAction SilentlyContinue
    $left = @(Get-ChildItem -Path $buildDir -Filter *.apk -ErrorAction SilentlyContinue)
    if ($left.Count -eq 0) { break }
    Start-Sleep -Milliseconds 400
}
Remove-Item $unsignedApk, $alignedApk -Force -ErrorAction SilentlyContinue

# 规则 15 自检：realise 之外不得存在任何 APK
$stray = @(Get-ChildItem -Path (Split-Path $root -Parent) -Recurse -Filter *.apk -ErrorAction SilentlyContinue |
    Where-Object { $_.DirectoryName -ne $realiseDir })
if ($stray.Count -gt 0) {
    $stray | ForEach-Object { Write-Host "STRAY APK: $($_.FullName)" }
    throw 'rule 15 violated: APK found outside realise'
}

& $apksigner verify --verbose $finalApk | Select-Object -First 4

$size = [math]::Round((Get-Item $finalApk).Length / 1KB, 1)
Write-Host ''
Write-Host "BUILD OK: $finalApk ($size KB)"
