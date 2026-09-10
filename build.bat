@echo off
setlocal
cd /d "%~dp0"
echo ========================================================
echo Building Cassette Player Plus v3.4 (Minecraft 1.21.1)
echo ========================================================
call gradlew.bat clean build %*
if %ERRORLEVEL% equ 0 (
    echo.
    echo ========================================================
    echo Build completed successfully!
    echo Output JAR: build\libs\cassette_player_plus-mc1.21.1-3.4.jar
    echo ========================================================
) else (
    echo.
    echo Build failed with error code %ERRORLEVEL%
)
endlocal
