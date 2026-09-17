# DIFF-517：命令盘 12 类设置深链 + Git push 冲突守卫 + Host 列表单测 + JSON listPaths 预览

## 背景

DIFF-516 已为命令盘追加部分设置分类（AI/Vault/网络等），但 `SettingsNavCategory` 共 12 类仍缺 **general / editor / tools** 深链；Vault Git **push** 仅在 UI 禁用冲突期，引擎层与 **commit** 不一致。F10 `HostProfileStore.list` 已接 `HostEngine.matchesProfileSearch`，缺存储层单测。F04 `listPaths` 登记仅断言 path，未锁 label/preview。

## 行为

### A01 / A02 命令盘 · MCP 设置可达

- `CommandSearchCatalog` 补全 12 类（与 `SettingsNavCategory.storageId()` 一一对应）：general、editor、tools；AI 增 `integration`；Vault 增 `username`/`askpass`/`credential`/`checkpoint`/`autopull` 等 Git·凭据关键词。
- `CommandSearchCatalogTest.catalogCoversEverySettingsNavCategory` 防止后续新增设置分类漏登记。

### A03 Vault Git

- `GitEngine.push`：未解决冲突（`conflicts > 0`）时拒绝，文案 `Resolve all conflicts before pushing`（UI 仍保持 DIFF-516 禁用；merge/rebase 进行中仍优先 merge 守卫）。
- `GitPushGuardTest.pushBlockedWhileMergeConflictsRemain`。

### F10 Host

- `HostProfileStoreTest`：profile id ROOT 搜索、`includeContent` 开关与列表过滤一致。

### F04 JSON

- `JsonEngineTest.listPathsMatchesElectronJsonToolsEnumerate` 增补 `label`/`preview`（`"One"` 字面量预览）。

## 验证

- `CommandSearchCatalogTest` / `GitPushGuardTest` / `HostProfileStoreTest` / `JsonEngineTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗冲突/Git merge/IME 截图、六套 CSS 皮肤、P7 Win/Linux 安装、`runDistributable` 烟雾、设置 TextField 链、命令盘 Compose 新帧、其余 F-tool 大切片。
