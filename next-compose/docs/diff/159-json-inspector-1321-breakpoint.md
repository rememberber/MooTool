# DIFF-159：JSON 检查器 1321dp 断点

## 背景

Electron `JsonTool` 用 `matchMedia('(min-width: 1321px)')` 初始化并在媒体查询变化时同步 `inspectorOpen`；compose 仅在 `<960dp` 紧凑态互斥 Vault/检查器，960–1320 仍可常开检查器，与 Electron 不一致。

## 变更

- `LayoutPolicy.JSON_DESKTOP_INSPECTOR_WIDTH_DP = 1321f` 与 `jsonInspectorDesktopOpen()`。
- 非紧凑宽度下，内容宽跨越 1321dp 时同步 `session.inspectorOpen`（与 Electron 一样不在同带宽内反复覆盖用户手动关闭）。
- JSONPath 弹层「使用」后写入 `json.notice.pathApplied`。

## 测试

- `LayoutPolicyTest.jsonInspectorDesktopBreakpointMatchesElectron`。

## 未覆盖

- 三栏 `minimumWidth` 1100 / 两栏 720 仍由窗口最小 960×640 与分栏手柄约束，未单独复刻 Electron `ResizableColumns.minimumWidth`。
