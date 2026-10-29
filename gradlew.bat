@echo off
setlocal
set GRADLE_VERSION=8.13
where gradle >nul 2>nul
if %ERRORLEVEL% EQU 0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
set GRADLE_USER_HOME=%GRADLE_USER_HOME%
if "%GRADLE_USER_HOME%"=="" set GRADLE_USER_HOME=%USERPROFILE%\.gradle
set GRADLE_HOME_LOCAL=%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin\manual\gradle-%GRADLE_VERSION%
if not exist "%GRADLE_HOME_LOCAL%\bin\gradle.bat" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base='%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin\manual'; New-Item -ItemType Directory -Force -Path $base | Out-Null; $zip=Join-Path $base 'gradle-%GRADLE_VERSION%-bin.zip'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip; Expand-Archive -Force $zip $base"
  if errorlevel 1 exit /b %ERRORLEVEL%
)
call "%GRADLE_HOME_LOCAL%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
