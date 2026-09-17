# DIFF-566：六套 CSS 批次 + JSON/Vault/Git 产品流 + HTTP/翻译边界 UI + 更新安装应用 + Vault MCP + 帧 160/161

## 背景

DIFF-565 已做 F15 Worker↔`runMatch`、`mooRegexResultsPane`/`mooRegexMatchCard`/`mooMessageBoardFormatRow`、Vault MCP notes read 分页与 list/read 联调、Compose 帧 `159`、P7 Win/Linux 说明脚本；本条**不重复** 565 F15 regex worker / messageboard CSS / notes read offset stdio / 帧 159 / P7 脚本链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### 样式（CSS 组件批次，非 565）

- `mooDiffNavCluster`（F02 对比模式/高亮导航簇）。
- `mooHttpResponseHead` / `mooHttpPreviousResponseHead`（F09 响应区标题行与「上次响应」态）。
- `mooTranslationResultPane` / `mooTranslationResultFooter`（F20 译文侧栏与底栏 provider 行）。
- `mooSettingsOpenInstallerButton`（A03 关于页打开安装包）。
- `mooQuickNoteVaultFooterActions`（F01 随手记 Vault 底栏操作行）。
- `mooCryptoRandomRow`（F14 随机串 Tab 单行）。

### JSON / Vault / Git 产品流

- `JsonVaultFooterPresentation.gitFlushSkipsWhenClean` / `gitUntitledBlockKey`；`jsonGitFlushBeforeAction` 改经 Presentation 守卫。
- `GitMergeProductFlowPresentation.mergeContinueActionEnabled`；Vault Git 对话框 continue 钮接线。

### HTTP / 翻译边界 UI

- `TranslationResponsePresentation.showResultFooter` / `resultFooterText`；F20 底栏 provider/fallback 显隐。
- F09 响应头行按 `HttpResponsePresentation.showPreviousLabel` 切换 `mooHttpPreviousResponseHead`。

### 更新安装应用路径

- `UpdateInstallApplyPresentation`：`canOpenInstaller` / `shouldRecordOpenFailure`。
- `UpdateCoordinator.openInstaller` 失败写 `error`；`UpdateCoordinatorOpenInstallerTest` mock 下载→打开/失败。

### Vault MCP stdio（search/read 余量）

- `subprocessNotesSearchHonorsSearchOffset`：notes search offset/limit（**非** 564 json search offset / 565 notes read offset 链）。
- `subprocessDualVaultReadAfterSearch`：双库 access 同会话 notes+json read（**非** 562 双库 search-only 链）。

### Compose 证据

- `TextDiffNavCaptureTest` → `160-compose-text-diff-nav-tab-focus.png`。
- `SettingsAboutCaptureTest.captureAboutOpenInstallerButtonFocusRing` → `161-compose-settings-about-open-installer-tab-focus.png`（非产品主窗）。

## 验证

- `JsonVaultFooterPresentationTest` / `GitMergeProductFlowPresentationTest` / `TranslationResponsePresentationTest` / `UpdateInstallApplyPresentationTest` / `UpdateCoordinatorOpenInstallerTest` / `AiIntegrationVaultMcpConnectionTest` / `TextDiffNavCaptureTest` / `SettingsAboutCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
