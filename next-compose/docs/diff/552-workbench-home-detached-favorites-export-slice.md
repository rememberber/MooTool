# DIFF-552：工作台/首页/分离/收藏 Presentation + CSS + F10/F22/F24 导出接线 + 设置导航 + P7 校验

## 背景

DIFF-551 已做 Vault Git merge 产品流与 F02/F03/F16 引擎/CSS；本条**不重复** 551 的 `GitMergeProductFlowPresentation` 或 merge 冲突链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、工作台 substantial 缺口、目标未达成。

## 行为

### 工作台 / 首页 / 分离 / 收藏

- `HomePresentation` + `mooHomeContent`/`mooHomeSection`：`HomeScreen` 布局常量与 Electron `.home-page` 对齐。
- `DetachedToolPresentation` + `mooDetachedPlaceholder`：分离窗标题 `工具 · 产品名`、主窗 `DetachedNotice` 占位。
- `WorkbenchNavPresentation`：分离占位与 `DetachPolicy.recentToolIds` 集中入口。
- `FavoritePresentation`：Cron/Regex/Color 收藏搜索与默认名称；`mooFavoriteRow` 行高。

### A01 设置导航 UI

- `SettingsNavPresentation`：分类 step/内容区顶栏图标与 `settings.category.*` 键；`mooSettingsNavHeader`。

### F10 / F22 / F24 导出接线

- `ToolsExportWiringPresentation`：`tools.exportDirectory` + Desktop 回退；F10 Host 导入/导出走 `chooseFileWithExportDirectory`；F24 合并默认路径；F22 SVG 批量目录初始路径。

### A02 / 命令盘

- `CommandSearchCatalog` layout：`detach`/`workbench`/`home`/`contributor` 等（**不含** 551 已增 merge 链重复）。
- `ToolRegistry`：首页 contributor 关键词；可分离工具增 `detach`/`分离`。

### P7 / 证据脚本

- `mootool_evidence_print_p7_smoke_hint`；`verify-product-evidence-prep.sh` 对 `prepare-p7-package-smoke.sh` 做 `bash -n`；`ProductEvidencePrepScriptTest` 锁定语法。

## 验证

- `FavoritePresentationTest` / `DetachedToolPresentationTest` / `ToolsExportWiringPresentationTest`
- `SettingsNavPresentationTest` / `WorkbenchNavPresentationTest` / `HomeLinksTest`
- `CommandSearchCatalogTest` / `ProductEvidencePrepScriptTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗全工具 Tab/分离窗/收藏 substantial UI 走查 PNG、P7 三平台安装/公证、其余 F 工具引擎大切片、目标未达成。
