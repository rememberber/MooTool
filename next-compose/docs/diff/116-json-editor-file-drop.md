# DIFF-116：JSON 编辑器拖入文件

对照随手记 `handleDroppedFiles` 与 F04「拖入编辑器」要求。

## 行为

- `EditorHost` 启用 `onFilesDropped`：
  - `.json`：复制进 Vault（`jsonVault.importFile`）、打开为当前文件并记监视器指纹。
  - 其它文本：读入编辑器（可撤销），与「导入」按钮一致。
- 失败保留 `json.notice.failed` 提示。

## 文件

- `JsonScreen.kt`：`handleJsonEditorFileDrop`
