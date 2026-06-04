<# :
@echo off
title Bhairavnath Aqua Installer
color 0b
echo ===================================================
echo   Installing Bhairavnath Aqua Management System...
echo ===================================================
echo.
echo Please wait while we set up the software on your computer...

taskkill /f /im "Bhairavnath-Aqua.exe" >nul 2>&1
taskkill /f /im "java.exe" >nul 2>&1
timeout /t 2 >nul

powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-Expression (Get-Content '%~f0' -Raw)"
exit /b
#>

$installDir = "$env:LOCALAPPDATA\Bhairavnath-Aqua"
$sourceDir = Join-Path $PSScriptRoot "AquaManagement_Desktop_App\Bhairavnath-Aqua"

if (Test-Path $installDir) {
    Remove-Item -Path $installDir -Recurse -Force -ErrorAction SilentlyContinue
}
New-Item -ItemType Directory -Force -Path $installDir | Out-Null

if (Test-Path $sourceDir) {
    Copy-Item -Path "$sourceDir\*" -Destination $installDir -Recurse -Force
} else {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show("Installation source files are missing. Please extract the ZIP completely before running.", "Installation Error", 0, 16) | Out-Null
    exit
}

$WshShell = New-Object -ComObject WScript.Shell
$Shortcut = $WshShell.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('Desktop'), 'Bhairavnath Aqua.lnk'))
$Shortcut.TargetPath = "$installDir\Bhairavnath-Aqua.exe"
$Shortcut.WorkingDirectory = "$installDir"
$Shortcut.IconLocation = "$installDir\app_icon.ico"
$Shortcut.Description = 'Bhairavnath Aqua Management System'
$Shortcut.Save()

$Shortcut2 = $WshShell.CreateShortcut([System.IO.Path]::Combine([Environment]::GetFolderPath('StartMenu'), 'Programs', 'Bhairavnath Aqua.lnk'))
$Shortcut2.TargetPath = "$installDir\Bhairavnath-Aqua.exe"
$Shortcut2.WorkingDirectory = "$installDir"
$Shortcut2.IconLocation = "$installDir\app_icon.ico"
$Shortcut2.Description = 'Bhairavnath Aqua Management System'
$Shortcut2.Save()

$uninstallScript = @"
@echo off
echo Uninstalling Bhairavnath Aqua...
timeout /t 1 >nul
taskkill /f /im "Bhairavnath-Aqua.exe" >nul 2>&1
taskkill /f /im java.exe >nul 2>&1
rmdir /s /q "$installDir"
del "%userprofile%\Desktop\Bhairavnath Aqua.lnk"
del "%appdata%\Microsoft\Windows\Start Menu\Programs\Bhairavnath Aqua.lnk"
echo.
echo Uninstallation completed successfully!
pause
"@
$uninstallScript | Out-File -FilePath "$installDir\Uninstall-Aqua.bat" -Encoding Ascii

Add-Type -AssemblyName PresentationFramework
[System.Windows.MessageBox]::Show("Bhairavnath Aqua Management System has been installed successfully!`n`nA Desktop and Start Menu shortcut have been created. enjoy! ✅", "Installation Successful", 0, 64) | Out-Null

Start-Process -FilePath "$installDir\Bhairavnath-Aqua.exe" -WorkingDirectory "$installDir"
