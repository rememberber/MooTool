# DIFF-509：设置 Vault Git 失焦提交、非法 remote 提示、P7 可选本机构建

## 背景

DIFF-508「未做」仍列：非法 remote 保存时 UI 即时校验、Vault/Git 产品走查、P7 `packageDistributionForCurrentOS` 烟雾路径。本条优先 **A03 设置 Git 字段交互**（非纯 load normalize）并补 P7 脚本接线；避开设置分类逐组 UI 大改与其它 F-tool 引擎切片。

## 行为

### A03 设置 Vault / Git

- `SettingCommitTextField`：草稿编辑、失焦 `trim` 后提交（对齐 Electron `TextSetting` / `TextInput.onBlur`）。
- `gitUsername` / `gitToken`：失焦提交并长度上限（128 / 2048）。
- `gitRemote`：`SettingsVaultGitNormalize.commitGitRemote`；非法非空 remote 拒绝写入、`toastError` + 恢复已保存值；合法前缀与空串仍走 `SettingsRepository.save` 规范化链。
- i18n：`settings.vault.gitRemoteInvalid`（zh/en）。

### P7

- `prepare-p7-package-smoke.sh`：`MOOTOOL_P7_BUILD_DIST=1` 时执行 `:composeApp:packageDistributionForCurrentOS --offline`；默认仍只跑元数据 + 单测。文档标明 Windows/Linux 包须在本机 OS 构建，macOS 烟雾不代替三平台安装验收。

### 产品证据

- `docs/evidence/2026-09-17-vault-git-settings/results.md`：配合既有 `prepare-vault-git-settings-evidence.sh` 的手工走查说明（不提交 PNG）。

## 验证

- `SettingsVaultGitNormalizeTest.commitGitRemoteMatchesSettingsBlurSemantics`
- `./gradlew :composeApp:verifyNativePackageMetadata :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具 Tab 走查、命令盘 Compose 新帧、P7 三平台真实安装/公证、设置分类面板与 Electron 逐组 UI 差、JSON Vault Git 面板 remote 行即时校验（仍由 `GitEngine.setRemote` 处理）、`MOOTOOL_P7_BUILD_DIST=1` 在本会话未执行（耗时）。
