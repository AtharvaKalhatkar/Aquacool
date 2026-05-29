@echo off
title Create Desktop Shortcut
echo Creating desktop shortcut for Bhairavnath Aqua...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$WshShell = New-Object -ComObject WScript.Shell; $Shortcut = $WshShell.CreateShortcut('%USERPROFILE%\Desktop\Bhairavnath Aqua.lnk'); $Shortcut.TargetPath = '%~dp0Bhairavnath-Aqua.exe'; $Shortcut.WorkingDirectory = '%~dp0'; $Shortcut.IconLocation = '%~dp0Bhairavnath-Aqua.exe,0'; $Shortcut.Save()"
echo.
echo Desktop shortcut created successfully!
echo You can now close this window and launch the app from your desktop.
timeout /t 3
