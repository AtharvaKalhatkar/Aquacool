
[Setup]
AppName=Bhairavnath Aqua Management System
AppVersion=1.0.0
DefaultDirName={autopf}\Bhairavnath-Aqua
DefaultGroupName=Bhairavnath Aqua
OutputDir=.\
OutputBaseFilename=Bhairavnath-Aqua-Setup
Compression=lzma2/ultra64
SolidCompression=yes
SetupIconFile=app_icon.ico
UninstallDisplayIcon={app}\Bhairavnath-Aqua.exe
PrivilegesRequired=lowest

[Files]
Source: "AquaManagement_Desktop_App\Bhairavnath-Aqua\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
Source: "app_icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autodesktop}\Bhairavnath Aqua"; Filename: "{app}\Bhairavnath-Aqua.exe"; IconFilename: "{app}\app_icon.ico"
Name: "{group}\Bhairavnath Aqua"; Filename: "{app}\Bhairavnath-Aqua.exe"; IconFilename: "{app}\app_icon.ico"
Name: "{group}\Uninstall Bhairavnath Aqua"; Filename: "{uninstallexe}"

[Run]
Filename: "{app}\Bhairavnath-Aqua.exe"; Description: "Launch Bhairavnath Aqua Management System"; Flags: nowait postinstall skipifsilent
