export function startupErrorTitle(language = navigator.language): string {
  if (language.toLowerCase().startsWith('zh')) return 'MooTool Next Tauri 启动失败'
  if (language.toLowerCase().startsWith('ja')) return 'MooTool Next Tauri の起動に失敗しました'
  return 'MooTool Next Tauri failed to start'
}
