@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM  ExtendedHotbar project setup (Windows)
REM  Run this ONCE after cloning the repository.
REM ============================================================

set "ROOT=%~dp0"
cd /d "%ROOT%"

echo [setup] 1/4 Initializing submodules...
git submodule update --init --recursive
if errorlevel 1 goto :error

REM Ignore IntelliJ module files locally (sparse-checkout hides the repo .gitignore).
findstr /X /C:"*.iml" "%ROOT%.git\modules\web\info\exclude" >nul 2>&1 || echo *.iml>>"%ROOT%.git\modules\web\info\exclude"

echo [setup] 2/4 Applying sparse-checkout (src/ExtendedHotbar + src/style.css + src/i18n.js)...
git -C web sparse-checkout set --no-cone /src/ExtendedHotbar/ /src/style.css /src/i18n.js
if errorlevel 1 goto :error

echo [setup] 3/4 Enabling git hooks...
git config core.hooksPath githooks
if errorlevel 1 goto :error

echo [setup] 4/4 Creating icon symlink...
if exist "src\main\resources\assets\extendedhotbar\icon.png" del "src\main\resources\assets\extendedhotbar\icon.png"
mklink "src\main\resources\assets\extendedhotbar\icon.png" "..\..\..\..\..\icon.png"
if errorlevel 1 goto :error

echo.
echo Setup complete. You can now build the project.
exit /b 0

:error
echo.
echo Setup FAILED. Please check the error above.
exit /b 1
