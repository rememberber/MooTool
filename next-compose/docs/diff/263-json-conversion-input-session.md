# DIFF-263：JSON 检查器 XML/Bean 转换输入独立会话字段

## 对照 Electron

`JsonTool.tsx` 使用 `inputConversion` + `conversionInput` 保存检查器 XML/Bean 转换弹层，与 Vault 新建/重命名等对话框状态分离。

## 问题

Compose 曾用 `dialogInputMode`=`xml`/`bean` 与 Vault 共用的 `dialogInput`，打开转换弹层会覆盖 Vault 对话框草稿，切页恢复时也容易串状态。

## 行为

- `JsonSession.conversionMode`（`xml` / `bean` / 空）与 `conversionInput` 专用于检查器转换 overlay。
- Vault 继续只用 `dialogInputMode` 的 `json-*` 前缀与 `dialogInput`。
- 打开 XML/Bean 转换时清空 `conversionInput`（对齐 Electron 打开弹层时重置输入）。
- `jsonEditorAutoFocusEnabled` 在 `conversionMode` 非空时不抢焦点。

## 验证

- `JsonEditorFocusPolicyTest.autoFocus_blocked_by_inspector_conversion_overlay`
- `ToolModalOverlaysTest`（切页保留 `conversionMode` / `conversionInput`）
- `./gradlew :composeApp:desktopTest --offline`
