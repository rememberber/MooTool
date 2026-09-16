# DIFF-403：设置展示配置根与迁移预览重置

## 背景

`data-platform-release.md` 规定配置目录与数据根分离；自定义 `data.directory` 不迁移 `configRoot`。DIFF-402 仅展示有效 `dataRoot`，用户仍难定位 `mootool-compose.settings.json` 与引导文件。备份恢复会替换 SQLite/指纹，迁移扫描预览若保留会误导。

## 行为

- 数据分类增加 `settings.configPath.effectiveHint` 与「打开配置目录」按钮（`configRoot`）。
- 迁移面板在 `sessionGeneration` 递增时清空预览/确认（备份恢复后需重新扫描）；导入成功提示保留见 [DIFF-404](404-cross-product-import-reload-sessions.md)。

## 验收

- A01/A03；`desktopTest --offline`。
