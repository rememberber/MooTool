#define MyAppName "MooTool Next Flutter"
#ifndef MyAppVersion
#define MyAppVersion "0.1.0"
#endif
#define MyAppPublisher "rememberber"
#define MyAppId "com.rememberber.mootool.next.flutter"

[Setup]
AppId={{A7C3E9F1-4B2D-4E11-9C8A-00NEXTFLUTTER}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={autopf}\MooTool Next Flutter
DefaultGroupName=MooTool Next Flutter
OutputBaseFilename=MooTool-Next-Flutter-{#MyAppVersion}-win-x64-setup
Compression=lzma
SolidCompression=yes
PrivilegesRequired=lowest
UninstallDisplayIcon={app}\MooToolNextFlutter.exe
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
; Unsigned on purpose.
SignTool=

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "..\..\build\windows\x64\runner\Release\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\MooTool Next Flutter"; Filename: "{app}\MooToolNextFlutter.exe"
Name: "{autodesktop}\MooTool Next Flutter"; Filename: "{app}\MooToolNextFlutter.exe"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Create a desktop shortcut"; GroupDescription: "Additional icons:"; Flags: unchecked

[UninstallDelete]
Type: filesandordirs; Name: "{app}"
