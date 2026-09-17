# DIFF-518：设置翻译语言失焦提交 + 命令盘工具默认深链 + F04 listPaths 补全

## 背景

DIFF-517 后 parity-gap 仍列：设置 **工具默认值** 中翻译源/目标语言逐字写入（非 Electron `TextSetting` 失焦提交）；命令盘 `tools` 分类缺翻译/Google/Bing 关键词；F04 `listPaths` 登记仅断言 `books[0]` 的 label/preview。Vault Git push 冲突守卫单测未写入 fixture 登记。

## 行为

### A01 / F20 设置 · 翻译语言

- `SettingsToolsTranslationNormalize.commitLanguagePair`：失焦时经 `TranslationEngine.normalizeLanguagePair` 写回（含旧版本地化名、源目标相同→`auto`）。
- 设置页「源语言 / 目标语言」改 `SettingCommitTextField`；提交一侧时同步规范化语言对，F20 读取 `settings.tools.*` 立即生效。

### A02 命令盘

- `CommandSearchCatalog` `tools` 分类增 `translation`/`google`/`bing`/`translate`/`翻译` 关键词。

### F04 JSON

- `JsonEngineTest.listPathsMatchesElectronJsonToolsEnumerate` 增补 `$.store.books[1].title` 的 label/preview。

### A03 Git fixture

- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 `GitPushGuardTest.pushBlockedWhileMergeConflictsRemain`。

## 验证

- `SettingsToolsTranslationNormalizeTest` / `CommandSearchCatalogTest.translateQueryOpensToolsDefaults` / `JsonEngineTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗冲突/Git merge/IME 截图、六套 CSS 皮肤、P7 Win/Linux 安装、Cron/HTTP/PDF 大切片、设置其它 TextField 链、命令盘 Compose 新帧。
