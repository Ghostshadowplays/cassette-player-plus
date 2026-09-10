@echo off
setlocal
cd /d "%~dp0"
echo Starting Minecraft NeoForge Client...
call gradlew.bat runClient %*
endlocal
