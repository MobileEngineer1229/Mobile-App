@echo off
REM ═══════════════════════════════════════════════════════════════
REM  Agar.io Server — Windows 10 Dependency Installer
REM  Requires: Visual Studio 2017, Git, CMake 3.14+
REM ═══════════════════════════════════════════════════════════════

setlocal EnableDelayedExpansion

echo.
echo ===  Agar.io Server — Windows Dependency Setup  ===
echo.

REM ── Check prerequisites ──────────────────────────────────────
where git >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Git not found. Install from https://git-scm.com
    exit /b 1
)

where cmake >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] CMake not found. Install from https://cmake.org
    exit /b 1
)

REM ── Step 1: Install vcpkg ─────────────────────────────────────
if not defined VCPKG_ROOT (
    set VCPKG_ROOT=C:\vcpkg
)

if not exist "%VCPKG_ROOT%\vcpkg.exe" (
    echo [1/5] Cloning vcpkg to %VCPKG_ROOT% ...
    git clone https://github.com/microsoft/vcpkg.git "%VCPKG_ROOT%"
    call "%VCPKG_ROOT%\bootstrap-vcpkg.bat" -disableMetrics
) else (
    echo [1/5] vcpkg already installed at %VCPKG_ROOT%
)

REM ── Step 2: Integrate vcpkg with Visual Studio ────────────────
echo [2/5] Integrating vcpkg with Visual Studio...
"%VCPKG_ROOT%\vcpkg.exe" integrate install

REM ── Step 3: Install packages (x64-windows) ───────────────────
echo [3/5] Installing packages for x64-windows (this takes 10-30 min)...

set TRIPLET=x64-windows

"%VCPKG_ROOT%\vcpkg.exe" install boost-asio:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install boost-beast:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install boost-system:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install protobuf:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install mongo-cxx-driver:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install hiredis:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install libdatachannel:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

"%VCPKG_ROOT%\vcpkg.exe" install openssl:%TRIPLET%
if %ERRORLEVEL% NEQ 0 goto :error

REM ── Step 4: Download EnTT (header-only) ──────────────────────
echo [4/5] Downloading EnTT header-only library...
if not exist "..\vendor\entt" (
    mkdir "..\vendor" 2>nul
    git clone --depth=1 https://github.com/skypjack/entt.git ..\vendor\entt
) else (
    echo EnTT already present.
)

REM ── Step 5: Install MongoDB Community Server ─────────────────
echo [5/5] Checking MongoDB...
where mongod >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [INFO] MongoDB not found. Download and install from:
    echo        https://www.mongodb.com/try/download/community
    echo        Default install path: C:\Program Files\MongoDB\Server\X.X\bin
    echo        Add to PATH then run: mongod --dbpath C:\data\db
) else (
    echo MongoDB found.
)

REM ── Redis for Windows ─────────────────────────────────────────
where redis-server >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [INFO] Redis not found. Install Memurai (Redis-compatible for Windows):
    echo        https://www.memurai.com/get-memurai
    echo        Or use WSL2: wsl --install ubuntu then apt install redis-server
)

echo.
echo ═══════════════════════════════════════════════════════════
echo  Setup complete!
echo.
echo  Next steps:
echo  1. Set VCPKG_ROOT=%VCPKG_ROOT% as a system environment variable
echo  2. Start MongoDB:  mongod --dbpath C:\data\db
echo  3. Start Redis:    redis-server (or Memurai)
echo  4. Open AgarServer.sln in Visual Studio 2017
echo  5. Build ^> Release ^> x64
echo  6. Run bin\Release\AgarServer.exe
echo ═══════════════════════════════════════════════════════════
goto :end

:error
echo [ERROR] vcpkg install failed. Check output above.
exit /b 1

:end
endlocal
