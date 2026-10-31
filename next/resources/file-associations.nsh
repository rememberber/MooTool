; Advertise MooTool in Open With / Default Apps without replacing the user's
; current default. SHCTX follows NSIS's per-user or per-machine install scope.
!macro MooToolFileType EXT
  WriteRegStr SHCTX "Software\Classes\MooTool.NextElectron.${EXT}" "" "MooTool ${EXT} document"
  WriteRegStr SHCTX "Software\Classes\MooTool.NextElectron.${EXT}\DefaultIcon" "" '$\"$INSTDIR\${APP_EXECUTABLE_FILENAME}$\",0'
  WriteRegStr SHCTX "Software\Classes\MooTool.NextElectron.${EXT}\shell\open\command" "" '$\"$INSTDIR\${APP_EXECUTABLE_FILENAME}$\" $\"%1$\"'
  WriteRegStr SHCTX "Software\Classes\.${EXT}\OpenWithProgids" "MooTool.NextElectron.${EXT}" ""
  WriteRegStr SHCTX "Software\MooToolNextElectron\Capabilities\FileAssociations" ".${EXT}" "MooTool.NextElectron.${EXT}"
!macroend

!macro MooToolRemoveFileType EXT
  DeleteRegValue SHCTX "Software\Classes\.${EXT}\OpenWithProgids" "MooTool.NextElectron.${EXT}"
  DeleteRegKey /ifempty SHCTX "Software\Classes\.${EXT}\OpenWithProgids"
  DeleteRegKey /ifempty SHCTX "Software\Classes\.${EXT}"
  DeleteRegKey SHCTX "Software\Classes\MooTool.NextElectron.${EXT}"
!macroend

!macro MooToolTextTypes ACTION
  !insertmacro ${ACTION} "json"
  !insertmacro ${ACTION} "jsonc"
  !insertmacro ${ACTION} "txt"
  !insertmacro ${ACTION} "log"
  !insertmacro ${ACTION} "md"
  !insertmacro ${ACTION} "markdown"
  !insertmacro ${ACTION} "xml"
  !insertmacro ${ACTION} "yaml"
  !insertmacro ${ACTION} "yml"
  !insertmacro ${ACTION} "html"
  !insertmacro ${ACTION} "htm"
  !insertmacro ${ACTION} "js"
  !insertmacro ${ACTION} "ts"
  !insertmacro ${ACTION} "java"
  !insertmacro ${ACTION} "py"
  !insertmacro ${ACTION} "sql"
!macroend

!macro customInstall
  WriteRegStr SHCTX "Software\MooToolNextElectron\Capabilities" "ApplicationName" "MooTool Next Electron"
  WriteRegStr SHCTX "Software\MooToolNextElectron\Capabilities" "ApplicationDescription" "MooTool text and JSON workspaces"
  WriteRegStr SHCTX "Software\RegisteredApplications" "MooTool Next Electron" "Software\MooToolNextElectron\Capabilities"
  !insertmacro MooToolTextTypes MooToolFileType
  System::Call 'shell32::SHChangeNotify(i 0x08000000, i 0, p 0, p 0)'
!macroend

!macro customUnInstall
  !insertmacro MooToolTextTypes MooToolRemoveFileType
  DeleteRegValue SHCTX "Software\RegisteredApplications" "MooTool Next Electron"
  DeleteRegKey SHCTX "Software\MooToolNextElectron\Capabilities"
  DeleteRegKey /ifempty SHCTX "Software\MooToolNextElectron"
  System::Call 'shell32::SHChangeNotify(i 0x08000000, i 0, p 0, p 0)'
!macroend
