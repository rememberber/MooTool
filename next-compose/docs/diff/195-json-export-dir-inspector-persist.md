# DIFF-195：JSON 检查器会话持久化 + Vault 导入导出目录

## 背景

- F04 检查器 **类名**、**JSONPath 结果** 在重启后丢失：`JsonSessionSnapshot` 未包含 `className` / `pathResult`。
- F04/F01 工具栏与 Vault 右键 **导入/导出** 的 AWT `FileDialog` 未使用设置中的 `tools.exportDirectory`，与 [DIFF-189](189-legacy-qr-save-export-directory.md) 二维码保存行为不一致。

## 行为

- `JsonSessionSnapshot` 增加 `className`、`pathResult`（默认兼容旧会话 JSON）。
- 新增 `ToolsExportDirectory.kt`：`chooseFileWithExportDirectory`、`persistToolsExportDirectory`；JSON/随手记导入导出与 Vault 导出使用导出目录初始路径，成功后写回 `tools.exportDirectory`。
- 二维码保存复用 `persistToolsExportDirectory`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（含 `JsonSessionSnapshotTest`）
- 对照 Electron JSON/随手记文件选择与 `exportDirectory` 设置项
