# ============================================================
# ETERNA PoC — Unity UaaL + Compose 통합 자동화
# ============================================================
# 사용법: ComposePoC 폴더에서 실행
#   .\Build.ps1
# ============================================================

$ErrorActionPreference = "Stop"

# ===== 경로 설정 (하드코딩) =====
$ExportPath   = "C:\Users\user\PoC\UnityPoC\Builds"
$ApkPath      = "C:\Users\user\PoC\UnityPoC\Builds\PoC.apk"
$ComposeRoot  = "C:\Users\user\PoC\ComposePoC"
$env:JAVA_HOME = "C:\Users\user\AppData\Local\Programs\Android Studio\jbr"

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  ETERNA PoC Build Script" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan

# ============================================================
# Step 0: Unity batchmode — APK + Export
# ============================================================
Write-Host "`n[0/6] Unity 빌드..." -ForegroundColor Green

$UnityPath = "C:\Program Files\Unity\Hub\Editor\6000.0.63f1\Editor\Unity.exe"
$ProjectPath = "C:\Users\user\PoC\UnityPoC"

# Unity 에디터 열려있으면 경고
$unityProc = Get-Process Unity -ErrorAction SilentlyContinue
if ($unityProc) {
    Write-Host "  Unity 에디터가 실행 중입니다. 닫은 뒤 아무 키나 누르세요." -ForegroundColor Yellow
    pause
}

# APK 빌드
Write-Host "  APK 빌드 중..." -ForegroundColor Cyan
& $UnityPath -quit -batchmode -buildTarget Android -projectPath $ProjectPath -executeMethod Editor.BuildScript.BuildAPK -logFile "$ProjectPath\Logs\build_apk.log"

if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: APK 빌드 실패! 로그: $ProjectPath\Logs\build_apk.log" -ForegroundColor Red
    exit 1
}
Write-Host "  APK OK" -ForegroundColor Green

# Export
Write-Host "  Export 중..." -ForegroundColor Cyan
& $UnityPath -quit -batchmode -buildTarget Android -projectPath $ProjectPath -executeMethod Editor.BuildScript.ExportAndroidProject -logFile "$ProjectPath\Logs\build_export.log"
if ($LASTEXITCODE -ne 0) {
    Write-Host "  ERROR: Export 실패! 로그: $ProjectPath\Logs\build_export.log" -ForegroundColor Red
    exit 1
}
Write-Host "  Export OK" -ForegroundColor Green

# ============================================================
# Step 1: unityLibrary 복사
# ============================================================
Write-Host "`n[1/6] unityLibrary 복사..." -ForegroundColor Green

$srcLibrary = "$ExportPath\unityLibrary"
$dstLibrary = "$ComposeRoot\unityLibrary"

if (-not (Test-Path $srcLibrary)) {
    Write-Host "ERROR: $srcLibrary 없음!" -ForegroundColor Red
    exit 1
}

if (Test-Path $dstLibrary) { Remove-Item $dstLibrary -Recurse -Force }
Copy-Item -Recurse $srcLibrary $dstLibrary
Write-Host "  OK" -ForegroundColor Green

# ============================================================
# Step 2: shared 폴더 복사
# ============================================================
Write-Host "`n[2/6] shared 폴더 복사..." -ForegroundColor Green

$srcShared = "$ExportPath\shared"
$dstShared = "$ComposeRoot\shared"

if (Test-Path $srcShared) {
    if (Test-Path $dstShared) { Remove-Item $dstShared -Recurse -Force }
    Copy-Item -Recurse $srcShared $dstShared
    Write-Host "  OK" -ForegroundColor Green
} else {
    Write-Host "  스킵 (없음)" -ForegroundColor Yellow
}

# ============================================================
# Step 2.5: unityLibrary AndroidManifest에서 LAUNCHER 제거
# ============================================================
Write-Host "`n[2.5/6] AndroidManifest LAUNCHER 제거..." -ForegroundColor Green

$manifestPath = "$dstLibrary\src\main\AndroidManifest.xml"
if (Test-Path $manifestPath) {
    $xml = Get-Content $manifestPath -Raw
    $patched = $xml -replace '(?s)<intent-filter>\s*<action android:name="android.intent.action.MAIN"\s*/>\s*<category android:name="android.intent.category.LAUNCHER"\s*/>\s*</intent-filter>', ''
    Set-Content $manifestPath $patched -Encoding UTF8
    Write-Host "  OK (LAUNCHER intent-filter 제거됨)" -ForegroundColor Green
} else {
    Write-Host "  WARNING: AndroidManifest.xml 없음" -ForegroundColor Yellow
}

# ============================================================
# Step 3: build.gradle 수정
# ============================================================
Write-Host "`n[3/6] build.gradle 수정..." -ForegroundColor Green

$buildGradle = "$dstLibrary\build.gradle"
$originalContent = Get-Content $buildGradle -Raw

# 원본에서 값 추출
$ndkPath = if ($originalContent -match 'ndkPath\s+"([^"]+)"') { $matches[1] } else { "" }
$ndkVersion = if ($originalContent -match 'ndkVersion\s+"([^"]+)"') { $matches[1] } else { "27.2.12479018" }
$compileSdk = if ($originalContent -match 'compileSdk\s+(\d+)') { $matches[1] } else { "36" }
$minSdk = if ($originalContent -match 'minSdk\s+(\d+)') { $matches[1] } else { "23" }
$targetSdk = if ($originalContent -match 'targetSdk\s+(\d+)') { $matches[1] } else { "36" }
$buildTools = if ($originalContent -match 'buildToolsVersion\s*=\s*"([^"]+)"') { $matches[1] } else { "36.0.0" }

$newContent = @"
apply plugin: 'com.android.library'
apply from: '../shared/keepUnitySymbols.gradle'

dependencies {
    api fileTree(dir: 'libs', include: ['*.jar'])
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core:1.9.0'
    implementation 'androidx.games:games-activity:3.0.5'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
}

android {
    namespace "com.unity3d.player"
    ndkPath "$ndkPath"
    ndkVersion "$ndkVersion"
    compileSdk $compileSdk
    buildToolsVersion = "$buildTools"

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    defaultConfig {
        consumerProguardFiles "proguard-unity.txt"
        versionName "0.1.0"
        minSdk $minSdk
        targetSdk $targetSdk
        versionCode 1

        ndk {
            abiFilters "arm64-v8a"
            debugSymbolLevel "none"
        }
    }

    lint {
        abortOnError false
    }

    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:!CVS:!thumbs.db:!picasa.ini:!*~"
        noCompress = ['.unity3d', '.ress', '.resource', '.obb', '.bundle', '.unityexp'] + unityStreamingAssets.tokenize(', ')
    }

    packaging {
        jniLibs {
            useLegacyPackaging true
        }
    }
}
"@

Set-Content $buildGradle $newContent -Encoding UTF8
Write-Host "  OK (IL2CPP 태스크 제거, api fileTree 적용)" -ForegroundColor Green

# ============================================================
# Step 4: APK에서 libil2cpp.so + global-metadata.dat 추출
# ============================================================
Write-Host "`n[4/6] APK에서 파일 추출..." -ForegroundColor Green

if (-not (Test-Path $ApkPath)) {
    Write-Host "  ERROR: APK 없음: $ApkPath" -ForegroundColor Red
    Write-Host "  Unity에서 일반 Build (APK)를 먼저 해주세요!" -ForegroundColor Yellow
    exit 1
}

$tempZip = "$env:TEMP\unity_apk_temp.zip"
$extractDir = "$env:TEMP\unity_apk_extract"

if (Test-Path $tempZip) { Remove-Item $tempZip -Force }
if (Test-Path $extractDir) { Remove-Item $extractDir -Recurse -Force }

Copy-Item $ApkPath $tempZip
Expand-Archive $tempZip -DestinationPath $extractDir -Force

# jniLibs 폴더 준비
$jniLibsDir = "$dstLibrary\src\main\jniLibs\arm64-v8a"
New-Item -ItemType Directory -Force $jniLibsDir | Out-Null

# APK에서 모든 arm64 .so 파일 복사
$apkLibDir = "$extractDir\lib\arm64-v8a"
if (Test-Path $apkLibDir) {
    Get-ChildItem $apkLibDir -Filter "*.so" | ForEach-Object {
        Copy-Item $_.FullName "$jniLibsDir\$($_.Name)" -Force
        Write-Host "  $($_.Name) ($([math]::Round($_.Length/1MB, 1))MB)" -ForegroundColor Cyan
    }
} else {
    Write-Host "  WARNING: APK에 lib/arm64-v8a 없음" -ForegroundColor Yellow
}

# global-metadata.dat 복사
$metadataFiles = Get-ChildItem $extractDir -Recurse -Filter "global-metadata.dat"
if ($metadataFiles.Count -gt 0) {
    # APK에서의 상대 경로 유지
    $metaSrc = $metadataFiles[0].FullName
    $metaRelative = $metaSrc.Replace("$extractDir\assets\", "")
    $metaDstDir = "$dstLibrary\src\main\assets\$(Split-Path $metaRelative -Parent)"
    New-Item -ItemType Directory -Force $metaDstDir | Out-Null
    Copy-Item $metaSrc "$metaDstDir\global-metadata.dat" -Force
    Write-Host "  global-metadata.dat OK" -ForegroundColor Cyan
} else {
    Write-Host "  WARNING: global-metadata.dat 없음" -ForegroundColor Yellow
}

# 기타 assets 복사 (Unity data 파일들)
$apkAssets = "$extractDir\assets"
$dstAssets = "$dstLibrary\src\main\assets"
if (Test-Path $apkAssets) {
    # bin 폴더 (Unity 데이터)
    $binDir = "$apkAssets\bin"
    if (Test-Path $binDir) {
        Copy-Item -Recurse $binDir "$dstAssets\bin" -Force
        Write-Host "  assets/bin/ 복사 OK" -ForegroundColor Cyan
    }
}

# 클린업
Remove-Item $tempZip -Force -ErrorAction SilentlyContinue
Remove-Item $extractDir -Recurse -Force -ErrorAction SilentlyContinue

Write-Host "  APK 추출 완료!" -ForegroundColor Green

# ============================================================
# Step 5: 확인
# ============================================================
Write-Host "`n[5/6] 파일 확인..." -ForegroundColor Green

$soFiles = Get-ChildItem $jniLibsDir -Filter "*.so" -ErrorAction SilentlyContinue
if ($soFiles) {
    $soFiles | ForEach-Object { Write-Host "  $($_.Name) ($([math]::Round($_.Length/1KB))KB)" }
} else {
    Write-Host "  ERROR: .so 파일 없음!" -ForegroundColor Red
    exit 1
}

$required = @("libmain.so", "libunity.so", "libil2cpp.so")
$missing = $required | Where-Object { -not (Test-Path "$jniLibsDir\$_") }
if ($missing.Count -gt 0) {
    Write-Host "  WARNING: 누락: $($missing -join ', ')" -ForegroundColor Red
} else {
    Write-Host "  필수 .so 전부 존재!" -ForegroundColor Green
}

# ============================================================
# Step 6: 클린 빌드
# ============================================================
Write-Host "`n[6/6] 클린 빌드..." -ForegroundColor Green

Push-Location $ComposeRoot
& "$ComposeRoot\gradlew.bat" clean
& "$ComposeRoot\gradlew.bat" :composeApp:assembleDebug
$buildResult = $LASTEXITCODE
Pop-Location

if ($buildResult -eq 0) {
    Write-Host "`n============================================" -ForegroundColor Green
    Write-Host "  BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host "============================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  설치: cd $ComposeRoot" -ForegroundColor Cyan
    Write-Host '  $env:JAVA_HOME = "C:\Users\user\AppData\Local\Programs\Android Studio\jbr"' -ForegroundColor Cyan
    Write-Host "  .\gradlew.bat :composeApp:installDebug" -ForegroundColor Cyan
} else {
    Write-Host "`n============================================" -ForegroundColor Red
    Write-Host "  BUILD FAILED" -ForegroundColor Red
    Write-Host "============================================" -ForegroundColor Red
}