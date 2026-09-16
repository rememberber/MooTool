# DIFF-147：颜色收藏 + 工具历史/草稿/QR SQLite 迁移

## 背景

Electron `legacyMigrationService` 除正则/Cron 外，还会迁移颜色收藏、`t_func_history`、`t_func_content`（草稿）、`t_qr_code`（写入历史）。next-compose 在 DIFF-146 后仍缺这些表。

## 行为

- `CrossProductImporter.inspect` 读取 `t_favorite_color_*`、`t_func_history`、`t_func_content`、`t_qr_code`。
- `LegacyToolIdMapper` 将 Java `func_type`/`func` 映射为 Compose `ToolId`（如 `json`、`qrCode`、`regex`）。
- `apply` 写入 `ColorFavoriteStore.mergeImportAll` 与 `HistoryRepository.mergeImport`（保留 `create_time`，按工具 200 条上限裁剪）。
- 设置迁移预览/完成文案增加颜色与历史/草稿计数。

## 证据

- `./gradlew :composeApp:desktopTest --offline`（`CrossProductImporterTest`、`LegacyToolIdMapperTest`）。

## 未覆盖

- `t_msg_http`、`t_http_request_history`、`t_host`、`t_translation_*` 等仍待后续批次。
- 无 Electron 式 `t_next_migration_row` 逐行幂等，依赖导入指纹与内容去重。
