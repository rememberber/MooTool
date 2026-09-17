# DIFF-516：命令盘设置深链 + JSON listPaths + Host 搜索 + Vault Git push UI

## 背景

DIFF-515 之后 parity-gap 仍列：MCP/AI 与 Vault Git 设置需从命令盘可达（相对侧栏仅工具列表）、Electron `jsonTools.test.ts` 路径枚举未单独登记、Host 方案搜索语义未与 `ToolRegistry` 对齐、merge/rebase 冲突期 push 按钮应显式禁用。

避开设置 `TextField`/normalize-only 链。

## 行为

### A02 命令盘 / 导航

- `CommandSearchCatalog`：非空查询时追加设置分类命中（`mcp`→AI、`git`→Vault、`proxy`→网络等），Enter 调用 `openSettings(categoryId)`。
- 结果行右侧分组文案 `app.search.settingsGroup`；工具结果仍走 `ToolRegistry.search`。

### F04 JSON

- `JsonEngine.listPaths` 单测 `listPathsMatchesElectronJsonToolsEnumerate`（`$.store.books[0].title`）。
- `docs/fixtures/electron-next-jsonTools-vitest.md` 登记 `list-paths-enumerate`。

### F10 Host

- `HostEngine.matchesProfileSearch`：profile id 用 `Locale.ROOT`，名称/正文用默认 locale；`HostProfileStore.list` 复用。

### A03 Vault Git UI

- Git 面板 **推送** 在 `conflicts > 0` 时禁用（与 commit/pull 一致，引擎层 merge/rebase 仍守卫 push）。

## 验证

- `CommandSearchCatalogTest` / `JsonEngineTest.listPathsMatchesElectronJsonToolsEnumerate` / `HostEngineTest.matchesProfileSearchUsesRootLocaleForProfileId`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗冲突/Git merge/IME 截图、六套 CSS 皮肤、P7 Win/Linux 安装、`runDistributable` 烟雾、设置 TextField 链、命令盘 Compose 新帧、其余 F-tool 大切片。
