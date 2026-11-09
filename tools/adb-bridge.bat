@echo off
rem Bridge the USB phone to WSL: Windows adb listens on the LAN, WSL uses ADB_SERVER_SOCKET.
rem Usage: double-click, or run  tools\adb-bridge.bat  from cmd.

set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%ADB%" set "ADB=adb"

echo [1/4] stopping any old adb server
"%ADB%" kill-server >nul 2>&1
timeout /t 3 >nul

echo [2/4] devices seen over USB
"%ADB%" devices

echo [3/4] starting adb server on 0.0.0.0:5037
start "" /b "%ADB%" -a -P 5037 nodaemon server
timeout /t 4 >nul

echo [4/4] devices through the LAN listener
"%ADB%" devices

echo.
echo In WSL run:  export ADB_SERVER_SOCKET=tcp:192.168.3.3:5037
echo Keep this window open while you work.
pause
