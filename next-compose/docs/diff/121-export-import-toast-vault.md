# DIFF-121：Vault/编辑器导出与随手记导入 toast

对照 Electron `JsonTool.tsx` 的 `toast.success(json.notice.exported)` 与全局导入反馈。

## 范围

- **F04 JSON**：工具栏导出、Vault 右键/菜单导出 → `toastSuccess(json.notice.exported)`。
- **F01 随手记**：工具栏/溢出/右键导出、工具栏/溢出导入 → 导入/导出 toast。
- **F06 配置转换**：Properties/YAML 导出 → `toastSuccess(json.notice.exported)`。
- **F03 格式化**：另存成功 → `toastSuccess(reformat.saved)`。

## 文件

- `JsonScreen.kt`、`QuickNoteScreen.kt`、`ConfigConvertScreen.kt`、`ReformatScreen.kt`
