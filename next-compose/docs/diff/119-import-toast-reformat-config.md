# DIFF-119：格式化/配置转换导入 toast

对照 Electron 全局 `ToastProvider`：文件/文本导入成功除状态栏文案外弹出应用内 toast。

## 范围

- **F03 格式化**：文件 Tab 选择文件或 `FileDropRow` 拖入后 `toastSuccess(json.notice.imported)`。
- **F06 配置转换**：Properties/YAML 导入按钮同样 toast。

## 文件

- `ReformatScreen.kt`、`ConfigConvertScreen.kt`
