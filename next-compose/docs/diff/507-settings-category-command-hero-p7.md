# DIFF-507：设置分类 chrome、命令盘搜索、hero/claude 壳、Vault Git、F11 端口、P7 脚本

## 背景

DIFF-506 之后 parity-gap 仍列：设置未实现分类面板（相对 Electron `SettingsWindow` 侧栏图标 + `settings.category.*` 标题）、命令盘与 Electron 搜索语义、hero/claude 工作区 editor-shell 策略、Vault Git 字段卫生、F-tool 引擎（非纯 normalize）、P7 三平台安装验收脚本。

避开 DIFF-501～506 控件半径链。

## 行为

### A01 设置分类 UI

- `SettingsNavCategory`：`categoryLabelKey()`（`settings.category.*`）、`navIcon()` 装饰 glyph。
- `SettingsNavItem` 可选图标列；内容区顶栏显示分类图标 + `MooPageTitle`（对齐 Electron `settings-content__header`）。
- i18n 增补 zh/en `settings.category.*` 文案（与 Electron `messages.ts` 一致）。

### A02 命令盘

- `ToolRegistry.search` / `matchesSearch`：工具 id 与 keywords 用 `Locale.ROOT` 规范化；本地化标题用默认 locale（对齐 Electron `toLocaleLowerCase`）。

### hero/claude 内容区 shell

- `MooColors.flattenWorkspaceToolPanels()` 明确 modern/quiet 压平与 hero/claude/smartisan 保留壳（`mooEditorFrame(flatten = true)` 既有逻辑不变）。
- `ThemeContrastTest.heroClaudeRestoreWorkspaceEditorShell` 锁定策略。

### A03 Vault Git

- `SettingsVaultGitNormalize`：加载/Electron 迁入链修剪 `gitUsername`/`gitRemote`/`gitToken` 并长度上限（与 `GitEngine.normalizeGitRemote` 一致）。

### F11 网络

- `NetEngine.parsePortSpec("3306,80-82,22")` 单测对齐 Electron `systemService.test.ts` 排序去重结果。

### P7

- `scripts/prepare-p7-package-smoke.sh`：toolchain + `printTooling` + `desktopTest --offline`；可选打包命令注释；更新 `scripts/README-product-evidence.md`。

## 验证

- `SettingsNavCategoryTest` / `SettingsCategoryI18nTest`
- `SettingsVaultGitNormalizeTest` / `SettingsVaultPathSanitizeTest.loadTrimsVaultGitCredentialFields`
- `ToolRegistryTest.searchUsesRootLocaleForToolIdsAndKeywords`
- `NetEngineTest.parsesIpv4RangeAndPortSpec`（Electron 端口 fixture）
- `ThemeContrastTest.heroClaudeRestoreWorkspaceEditorShell`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具 Tab 走查、命令盘 Compose 新帧、P7 三平台真实安装/公证、非 p5 工具页 modern 压平走查、其余 F-tool 引擎大切片。
