param(
  [Parameter(Mandatory = $true)]
  [string]$RepoUrl,
  [string]$ProjectDir = "C:\game\appdev\teamtodo"
)

$ErrorActionPreference = "Stop"

Write-Host "[1/6] Clone repository..."
if (-not (Test-Path (Split-Path -Parent $ProjectDir))) {
  New-Item -ItemType Directory -Path (Split-Path -Parent $ProjectDir) | Out-Null
}
if (Test-Path $ProjectDir) {
  Write-Host "Project directory already exists: $ProjectDir"
} else {
  git clone $RepoUrl $ProjectDir
}

Set-Location $ProjectDir

Write-Host "[2/6] Local properties check..."
if (-not (Test-Path ".\local.properties")) {
  Write-Host "local.properties not found. Create it with sdk.dir=... before Android build."
}

Write-Host "[3/6] JDK check..."
$jdk17 = "C:\Users\$env:USERNAME\.jdks\corretto-17.0.3\bin\java.exe"
if (-not (Test-Path $jdk17)) {
  Write-Host "JDK17 path not found: $jdk17"
  Write-Host "Install JDK17 and rerun build step."
  exit 1
}

Write-Host "[4/6] Build debug APK..."
$env:GRADLE_USER_HOME = "$ProjectDir\.gradle-home"
$env:ANDROID_USER_HOME = "$ProjectDir\.android-home"
Remove-Item Env:ANDROID_SDK_HOME -ErrorAction SilentlyContinue

& $jdk17 '-Dorg.gradle.appname=gradlew' -classpath gradle\wrapper\gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:testDebugUnitTest :app:assembleDebug

Write-Host "[5/6] APK info..."
Get-Item ".\app\build\outputs\apk\debug\app-debug.apk" | Select-Object FullName, Length, LastWriteTime

Write-Host "[6/6] Done."
