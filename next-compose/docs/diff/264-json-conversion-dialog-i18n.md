# DIFF-264：JSON 转换/结果对话框文案对齐 Electron

## 对照 Electron

`JsonToolDialogs.tsx` 与 `messages.ts`：

- XML/Bean 输入弹层：`json.dialog.input` 标签、主按钮 `json.dialog.run`、次按钮 `common.cancel`
- 结果弹层：`json.dialog.output` 标签、主按钮 `json.dialog.useOutput`、复制 `json.action.copy`
- 检查器类名标签 `json.dialog.className`（中文「根类名」）

## 行为（next-compose）

- 补齐上述 i18n 键（中/英）；转换弹层不再误用 `json.path.query` / `common.close`
- 结果弹层「恢复」改为 `json.dialog.useOutput` 并设为主按钮样式

## 验证

- `./gradlew :composeApp:desktopTest --offline`
