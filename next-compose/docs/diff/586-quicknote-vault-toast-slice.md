# DIFF-586：F01 Vault/树/附件失败 toast 统一 + 帧 181

基线：DIFF-585（工作区）。

## 范围

- **F01**：`QuickNoteWiringPresentation.operationFailureMessage` + `notifyQuickNoteOperationFailure`；Vault 移动/删除/对话框 CRUD、重复、拖入导入、清理孤儿、图片/格式化失败 error toast；`QuickNoteVaultMutationAborted` 避免保存守卫后重复 toast。
- **证据**：`QuickNoteVaultFooterCaptureTest` → `181-compose-quicknote-vault-footer-rename-tab-focus.png`。

**不重复** 585：F18/F10 时间 Host toast / 帧 180。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline`
