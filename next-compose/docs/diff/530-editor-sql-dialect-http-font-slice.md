# DIFF-530：编辑器 SQL 方言规范化 + JSON 系字号即时生效 + HTTP 响应区字号

## 背景

DIFF-529 已按工具区分软换行；仍缺：**设置/Electron 迁入** 对非法 `editor.sqlDialect` 的回退（Electron 下拉仅 10 项）、**F09 HTTP 响应** `EditorHost` 硬编码 `fontSize = 11` 与请求 Body 不一致、命令盘难搜到 SQL 方言设置。

## 行为

### A01 设置 · 编辑器

- `EditorFontSettings.normalizeSqlDialect`：与 Electron `SettingsWindow` / `SqlFormatEngine.dialects` 同一预设列表；`mysql` 别名→`MySQL`；未知/空→默认 `Standard SQL`。
- 加载、`SettingsRepository.sanitize`、`ElectronNextSettingsImport` 合并经 `normalizeEditorSettings` 写回。
- 命令盘 `editor` 分类增 `sql`/`dialect`/`方言` 关键词。

### A01 / F04 / F05 / F09 编辑器即时设置

- `EditorSettingsLiveApply.jsonEditorFontSize`：JSON / HTTP 请求·响应 / 代码运行源码 `EditorHost` 统一读 `settings.editor.jsonFontSize`（设置页失焦提交后重组即生效）。
- **修复** HTTP 响应区此前固定 11sp，现与请求 Body 一致。

## 验证

- `EditorFontSettingsTest` / `EditorSettingsLiveApplyTest` / `CommandSearchCatalogTest.sqlDialectQueryOpensEditorSettings`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、Host 软换行、随手记已开笔记随全局 softWrap 切换（Electron 保持 frontmatter）、multipart 文件 Tab、TCC/P7、目标未达成。
