# GitHub 이관 가이드 (Windows)

## 1) 현재 PC: 원격 연결 + push
1. 원격 저장소 URL 준비  
   예: `https://github.com/<org-or-id>/<repo>.git`
2. 아래 실행:
   ```powershell
   cd C:\game\appdev\teamtodo
   .\scripts\setup_github_remote.ps1 -RepoUrl "https://github.com/<org-or-id>/<repo>.git"
   ```

## 2) 새 PC: clone + 빌드
1. 아래 실행:
   ```powershell
   .\scripts\bootstrap_new_pc.ps1 -RepoUrl "https://github.com/<org-or-id>/<repo>.git" -ProjectDir "C:\game\appdev\teamtodo"
   ```
2. 필요 조건
   - Android SDK 설치 및 `local.properties`의 `sdk.dir` 설정
   - JDK 17 설치 (기본 경로: `C:\Users\<user>\.jdks\corretto-17.0.3\bin\java.exe`)

## 3) APK 경로
- `C:\game\appdev\teamtodo\app\build\outputs\apk\debug\app-debug.apk`

## 4) 주의사항
- `google-services.json`, `local.properties`, `.xdg-config` 등 로컬/민감 파일은 Git에 포함하지 않도록 `.gitignore` 처리되어 있음.
- 본 프로젝트는 현재 JDK 21 + AGP 조합에서 `jlink` 오류가 발생할 수 있어 JDK 17 빌드를 기준으로 사용.
