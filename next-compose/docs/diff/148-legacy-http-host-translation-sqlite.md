# DIFF-148：SQLite P5（HTTP/Host/翻译）迁移

## 背景

Electron `legacyMigrationService` 的 `sharedTableMigrations` 覆盖 `t_msg_http`、`t_http_request_history`、`t_host`、`t_translation_word`、`t_translation_history`。next-compose 在 DIFF-147 后仍缺这些表。

## 行为

- `t_msg_http` → `HttpCollectionStore`（解析 JSON params/headers/cookies，保留响应快照与时间戳）。
- `t_http_request_history` → `HistoryRepository`（`tool_id=http`，URL 经 `HistoryPrivacy.httpUrl` 遮蔽密码）。
- `t_host` → `HostProfileStore`（按方案名去重）。
- `t_translation_word` / `t_translation_history` → `TranslationStore.mergeImport*`（语言对经 `TranslationEngine.normalizeLanguagePair`）。
- 设置迁移页增加 P5 预览行；完成 toast 含 HTTP/Host/翻译计数。
- 单表最多读取 5000 行。

## 证据

- `./gradlew :composeApp:desktopTest --offline`（`CrossProductImporterTest` 含 P5 表）。

## 未覆盖

- Electron 侧 `legacySettingsPatch`、Vault 文件迁移幂等表、`t_next_migration_run` 语义仍不同。
- HTTP 历史不还原为完整 `SavedHttpRequest` 集合项，仅进入通用历史列表（与 Compose HTTP 工具现状一致）。
