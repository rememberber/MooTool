# DIFF-551：Vault Git merge 产品流 + F02/F03/F16 引擎接线 + CSS 批次 + 导航关键词

## 背景

DIFF-550 已做余下 F 工具 **metadata**、`JsonInspectorPresentation` 与 color/host/time 等 CSS；本条**不重复** 550 的 metadata/JSON 检查器链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、工作台 substantial 缺口、目标未达成。

## 行为

### Vault Git merge 产品流（§B）

- `GitMergeProductFlowPresentation`：merge 冲突期自动选中首个冲突文件、ours/theirs 显隐、产品走查 hint 文案键；`VaultGitDialog` 接线。
- `prepare-git-merge-conflict-evidence.sh` 提示与 `GitMergeProductFlowPresentation.EVIDENCE_CONFLICT_FILE` 对齐。

### F02 / F03 / F16 引擎（非 metadata）

- `TextDiffPresentation`：160ms 自动比较 debounce、`nextNavIndex` 与 Screen 导航一致。
- `ReformatWiringPresentation`：`saveResult` 默认另存文件名。
- `CronWiringPresentation`：时区菜单 dedupe / session zone coerce。

### 样式（CSS 组件批次）

- `mooDiffWorkspace` / `mooCronBuilder` / `mooCronRunCell` / `mooReformatFileLayout` / `mooConfigConvertPane`

### A01 / A02

- `CommandSearchCatalog`：`vault` 增 ours/theirs/continue/conflict.json；`layout` 增 favorites/cron/whitespace。
- `ToolRegistry`：TextDiff/Reformat/Cron 增 history/favorite/whitespace 等深链关键词。

## 验证

- `GitMergeProductFlowPresentationTest` / `TextDiffPresentationTest` / `ReformatWiringPresentationTest` / `CronWiringPresentationTest` / `CommandSearchCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 merge PNG、分离窗/收藏 substantial 缺口、P7 三平台安装、目标未达成。
