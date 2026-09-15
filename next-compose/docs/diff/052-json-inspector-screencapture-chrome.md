# DIFF-052：JSON 检查器 chrome、macOS 录屏权限与风格底

- 编号：DIFF-052
- 影响：F04 JSON 检查器；F00 首页分组卡片；F22/F23/托盘录屏权限；A01 工作区/侧栏风格底；ui-spec L1/L7
- 日期：2026-09-15

## 原行为（Electron）

JSON 检查器：缩进为 2/4 选择，格式选项为左控件右标签的 checkbox，转换 9 个动作是两列 `inspector-action-grid`，`className` 在转换区末尾，JSONPath 后再到结果。macOS 取色/截图需要 `NSScreenCaptureUsageDescription` 才能弹出 TCC。smartisan 等工作区有径向高光、侧栏有横向过渡。首页分区是 `home-section` 卡片。

## 本产品行为

- 检查器缩进改为分段 2/4；排序/忽略大小写/重复 key 改为开关+标签；转换动作按 Electron 次序排成两列网格；类名移到转换区；JSONPath 查询/选路径后仍保留路径树（Compose 额外能力）。
- macOS Info.plist 声明 `NSScreenCaptureUsageDescription`；权限失败时调用 `screencapture` 触发 TCC，仍失败则打开系统设置「屏幕录制」并给出可本地化提示。托盘取色/截图与调色板、图片走同一文案。
- smartisan/hero/claude 工作区叠加径向高光；smartisan/miui-v5 侧栏使用横向过渡。仍是 Compose token，不是 CSS 逐选择器移植。
- 首页关于/贡献者/赞赏等分区改为标题 + 卡片底。
- JSONPath 输入在检查器中拉满宽度（截图时曾偏窄）。

## 理由

检查器原先把开关做成 `"标签: true"` 按钮，转换区竖排，类名错位。Robot 截屏失败不会触发 macOS 权限对话框。工作区/侧栏此前是纯色，首页分区没有卡片层级。

## 证据

`InstallIdentityTest` 核对 plist 用途说明；`ScreenColorSamplerTest.permissionCopyOpensMacScreenCapturePane`。`desktopTest` **236/236**。本机 `screencapture -l` 截取首页/JSON/检查器/拆出窗/960 主窗/随手记/HTTP/设置，见 `docs/evidence/2026-09-15-inspector-screencapture/windows/`。TCC 对话框、IME/列编辑手势未做手工验收。

## 受影响范围

- 仍非 Electron 六套 CSS 逐选择器皮肤。
- 托盘/取色权限对话框需用户在真实窗口点一次才能验收。
- 三平台安装/升级/卸载未测。
