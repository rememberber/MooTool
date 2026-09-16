# DIFF-208：随手记 Vault 右键「文档信息」

## 背景

Electron `QuickNoteTree` 对文件提供 `onInfoRequest` → `DocumentInfoDialog`，展示路径、创建/修改时间与行/词/字符统计。

Compose 随手记 Vault 右键菜单缺少该入口。

## 行为

- `VaultContextId.Info`（仅文件）与随手记上下文菜单项。
- 打开前先 `saveIfNeeded` 并选中该笔记（对齐 `selectNode`）。
- 应用内 overlay 展示路径、`metadata.createdAt`/`modifiedAt`（本地格式化）与 `QuickReplaceEngine.stats` 统计。
- i18n：`quickNote.info`、path/created/modified/lines/words/characters（中/英/日）。

## 验证

- `QuickNoteDocumentInfoTest`
- `./gradlew :composeApp:desktopTest --offline`
