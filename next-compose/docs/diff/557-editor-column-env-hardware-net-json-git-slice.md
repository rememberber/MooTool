# DIFF-557：列编辑语义 + F08/F25/F11 引擎 UI + JSON 检查器 + Vault Git merge 产品 UI + CSS

## 背景

DIFF-556 已为余下 F 工具批量 `*WiringPresentation` 与 color/config/ua/quicknote CSS；本条**不重复** 556 接线/样式链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、substantial 缺口、目标未达成。

## 行为

### EditorHost 列选（对齐 Electron `columnEditingExtensions`）

- `EditorColumnEditPresentation`：`columnGestureActive` / `columnDragWithoutAlt` / 换行切换清列选；`ColumnEditBinder` 与 F01/F04 `EditorHost` 接线。
- `EditorBuffer.applyTheme`：wrap 变更时清除列选区（逻辑行语义）。

### F08 / F25 / F11 引擎 UI（非 metadata 批量接线）

- `EnvWiringPresentation`：刷新/导出/新增/保存守卫；`VariablesScreen` 接线。
- `HardwareWiringPresentation`：刷新/复制报告守卫；`HardwareScreen` 接线。
- `NetWiringPresentation` 扩展：`ping`/`ipRange`/`resolve`/`whois` 目标校验；`NetScreen` 按钮与 `runAction` 接线。

### JSON 检查器

- `JsonInspectorPresentation`：`showDuplicatePathList` / `structurePanelVisible`；结构行 CSS `mooJsonInspectorStructureRow`。

### Vault Git merge 产品 UI

- `GitMergeProductFlowPresentation.resolveActionsEnabled`；merge 走查 hint 样式 `mooGitMergeFlowHint`；`VaultGitDialog` 接线。

### 样式（CSS 组件批次）

- `mooJsonInspectorStructureRow` / `mooGitMergeFlowHint` / `mooEnvStatusFooter` / `mooHardwareToolbarMeta`

### A02

- `CommandSearchCatalog`：`mergeflow`/`mergeproduct`→Vault；`logical`→编辑器列选；`export-sections`→数据。

## 验证

- `EditorColumnEditPresentationTest` / `EnvWiringPresentationTest` / `HardwareWiringPresentationTest` / `NetWiringPresentationTest` / `JsonInspectorPresentationTest` / `GitMergeProductFlowPresentationTest` / `CommandSearchCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复证据 PNG）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗列选 IME 手工 PNG、P7 三平台安装/公证、其余 substantial 引擎/UI、目标未达成。
