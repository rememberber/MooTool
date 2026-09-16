# DIFF-196：JSON Vault 底栏选中路径与未保存标记

## 背景

Electron `JsonVaultPanel` 在文档库底部展示 `vault-panel__selection`：10sp 省略路径，当前文件未保存时在路径前加 `• `（对照 `JsonVaultPanel.tsx` 与 `.vault-panel__selection`）。

Compose F04 此前仅有树与快捷按钮，缺少该底栏。

## 行为

- 新增 `VaultSelectionFooter`（31dp 高、顶边 `borderSoft`、10sp muted 省略）。
- JSON Vault 在选中 `currentFile` 时展示；`editor.text != savedText` 时前缀 `• `。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
- 对照 Electron JSON 文档库底栏与 `global.css` `.vault-panel__selection`
