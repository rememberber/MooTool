# DIFF-559：CSS 批次 + F 工具引擎接线 + 导航/工作台 + JSON/Host + 翻译离线边界 + Git merge hint 帧 + P7

## 背景

DIFF-558 已做设置关于/更新 UI、Vault 外部冲突 UI、MCP `protobuf_wire` 与 Compose 帧 `152`；本条**不重复** 558 关于/更新/Vault 冲突 savecopy/MCP wire 链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### JSON Vault / Host 搜索

- `JsonVaultSearchPresentation`：`pathSegmentMatches` / `titleMatches` / `contentMatches`；`VaultSearchIndex` 复用。
- `HostWiringPresentation`：`canToggleContentSearch` / `showFilteredEmpty`；`HostScreen` 方案侧栏 `mooHostProfilesPane`、搜内容守卫与无匹配文案。

### 工作台导航

- `WorkbenchNavPresentation.showNavigationGroupLabel` 集中 `LayoutPolicy`；`Sidebar` 分组标题接线。

### F 工具引擎 UI（非 metadata）

- `UaWiringPresentation.canCopyResult`；`MessageBoardWiringPresentation.canEnterPresentation`；`ImageSvgWiringPresentation.canStartSvgBatch`；对应 Screen 按钮 `enabled`。

### 样式（CSS 组件批次）

- `mooJsonVaultSearch` 最小高度；`mooHostProfilesPane`（对齐 `.host-profiles` / `.http-collection`）。

### F20 翻译（离线 mock）

- `TranslationEngineTest`：空文本、Google 畸形 JSON、Bing 缺 token 页（本机 HttpServer，无公网）。

### Vault Git merge hint 叠层

- `GitMergeFlowOverlayCaptureTest` → `153-compose-git-merge-flow-hint-tab-focus.png`（状态胶囊 + 冲突/产品 hint + 刷新钮焦点，非产品主窗）。

### A02 / P7

- 命令盘 `vaultsearch`/`profilesearch`/`grouplabel` 等关键词。
- `prepare-p7-package-smoke.sh` 注释 DIFF-559 offline gate 范围。

## 验证

- `JsonVaultSearchPresentationTest` / `HostWiringPresentationTest` / `WorkbenchNavPresentationTest` / `UaRegexTimeWiringPresentationTest` / `MessageBoardWiringPresentationTest` / `ImageSvgWiringPresentationTest` / `TranslationEngineTest` / `CommandSearchCatalogTest` / `GitMergeFlowOverlayCaptureTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Vault Git merge/IME 手工 PNG、P7 三平台安装/公证、其余 substantial 引擎/UI、目标未达成。
