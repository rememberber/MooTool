# DIFF-146：Java SQLite 正则/Cron 收藏迁移 + F04 3 MiB 单测

## 背景

Electron `legacyMigrationService` 会从 Java `MooTool.db` 的 `t_favorite_regex_*` / `t_favorite_cron_*` 迁入 `t_next_favorite`。next-compose 的 `CrossProductImporter` 此前只迁笔记、JSON 与 Electron 自定义分组。

`feature-parity.md` 要求 F04 验收含 **3 MiB** 输入；单测此前仅约 1.5 MB 字符串。

## 行为

- `CrossProductImporter.inspect`：在 SQLite 中读取正则/Cron 收藏（含 list 联表标题，缺 list 表时回退单行查询；空分组名显示为「默认收藏夹」）。
- `ImportPreview.totalItems()` 计入收藏条数；指纹包含收藏内容，避免重复导入误判。
- `CrossProductImporter.apply`：可选传入 `RegexFavoriteStore` / `CronFavoriteStore`，经 `mergeImport` 按「名称+模式/表达式」去重写入 `data/favorites/*.json`。
- 设置迁移页展示 SQLite 收藏计数；完成 toast 增加 regex/cron 计数。
- `JsonEngineTest.formatsThreeMiBStringPayloadWithoutLoss`：构造 ≥3 MiB 的 JSON 文档并验证 `format` 不丢 payload。

## 证据

- `./gradlew :composeApp:desktopTest --offline`（含 `CrossProductImporterTest` 与 3 MiB 用例）。

## 未覆盖

- Electron 全量 legacy（HTTP 历史、Host、翻译、QR、`t_func_content` 等）仍不在 `CrossProductImporter` 范围。
- 收藏迁移无逐行 `t_next_migration` 幂等表，仅依赖导入指纹 + 名称/表达式去重。
