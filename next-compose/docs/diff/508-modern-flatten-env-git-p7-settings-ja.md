# DIFF-508：非 p5 工具壳压平、设置分类 ja、Env/Git 引擎、P7 元数据校验

## 背景

DIFF-507「未做」仍列：非 p5 工具页 modern 压平走查、设置分类文案（含 ja）、F-tool 引擎（非纯 normalize）、Git/Vault 产品流、P7 Gradle 打包路径。避开 DIFF-501～507 控件半径/设置 chrome 链。

## 行为

### P1 非 p5 工具壳

- `mooToolShell`：`flatten` 默认跟 `MooColors.flattenWorkspaceToolPanels()`（modern/quiet 压平；hero/claude/smartisan/miui 保留壳；`p5=true` 仍走 raised shell）。
- `ThemeContrastTest` 补充 quiet 压平策略。

### A01 设置分类 i18n

- `Translator` ja 增补全套 `settings.category.*`（对齐 Electron `messages.ts`）。
- `SettingsCategoryI18nTest` 锁定 ja 分类标题。

### F08 环境变量 / F11 延续

- `EnvEngineTest.updatesEnvironmentFileLikeElectronSystemServiceFixture`：对齐 Electron `systemService.test.ts` `JAVA_HOME` 引号更新/删除。

### A03 Vault Git 加载

- `SettingsVaultGitNormalize.sanitizeGitRemote`：加载/Electron 迁入时丢弃非法 remote（与 `GitEngine.normalizeGitRemote` 前缀/换行规则一致）。
- `GitEngineTest.settingsVaultGitSanitizeAcceptsSameRemotesAsNormalizeGitRemote`。
- `scripts/prepare-vault-git-settings-evidence.sh`：隔离目录 + file:// bare remote，供设置 Vault/Git 与 JSON Vault Git 产品窗走查说明。

### P7

- `:composeApp:verifyNativePackageMetadata`：锁定 DMG/MSI/DEB/RPM、bundleID、UpgradeCode、Linux 包名等（不构建安装包）。
- `prepare-p7-package-smoke.sh` 调用上述任务。

## 验证

- `ThemeContrastTest.heroClaudeRestoreWorkspaceEditorShell`
- `SettingsCategoryI18nTest` / `SettingsVaultGitNormalizeTest` / `EnvEngineTest` / `GitEngineTest`
- `./gradlew :composeApp:verifyNativePackageMetadata :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具 Tab 走查、命令盘 Compose 新帧、P7 三平台真实安装/公证、其余 F-tool 引擎大切片、设置分类面板与 Electron 逐组 UI 差、非法 remote 保存时 UI 即时校验（仍依赖 Git 面板 configure）。
