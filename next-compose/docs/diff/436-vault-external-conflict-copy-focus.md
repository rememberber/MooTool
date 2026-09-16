# DIFF-436：Vault 外部冲突「保存副本」闭环 + 对话框 Tab 焦点证据

## 背景

JSON/随手记在外部修改导致 `VaultConflictState` 时，用户可选「重新加载 / 保存副本 / 保留编辑」。`JsonScreen`/`QuickNoteScreen` 已调用 `VaultConflictEngine.conflictCopyName` 与 `write`，但缺存储层单测与冲突对话框键盘焦点回归帧（产品主窗仍待拍）。

## 行为

- `JsonVaultConflictCopyTest`：磁盘内容与编辑器草稿不一致时，`conflictCopyName` 旁路写入不覆盖原文件。
- `VaultConflictDialog`：三按钮补充 `contentDescription`；测试可注入 `reloadButtonModifier` 等。
- `VaultConflictCaptureTest`：`141-compose-vault-conflict-reload-tab-focus.png`（Compose 场景，非产品主窗）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（549/549）
