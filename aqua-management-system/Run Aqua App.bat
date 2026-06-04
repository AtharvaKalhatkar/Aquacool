@echo off
title Aqua Management System
echo Starting Aqua Management System...
echo Please wait while the system resolves dependencies and launches...

call "%~dp0mvnw.cmd" javafx:run

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with an error.
    pause
)
