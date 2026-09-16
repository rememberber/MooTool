# DIFF-427：JSON Vault 搜索框 Tab 焦点环证据

## 背景

parity-gap 列「JSON Vault 搜索」Tab 走查；`120-json-vault-search-focus.png` 为产品窗帧。需 Compose 场景回归 modern 焦点环 token（对齐 [DIFF-097](097-evidence-tab-focus-qrcode-filedrop.md) / `127`–`130`）。

## 行为

- `MooCompactSearch` 增加可选 `fieldModifier`（供测试 `focusRequester`）。
- `ToolbarFocusCaptureTest.captureJsonVaultSearchFocusRing` 生成 `131-compose-json-vault-search-tab-focus.png` 并断言 accent 环像素。

## 验证

- `./gradlew :composeApp:desktopTest --offline --tests '*ToolbarFocusCaptureTest*'`
