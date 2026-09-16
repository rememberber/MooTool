# DIFF-086：时间当前带、正则分栏、Cron 七字段、file-drop、计算器结果

- 编号：DIFF-086
- 影响：F18 时间；F15 正则；F16 Cron；F14/F17/F03 file-drop；F21 计算器；F07 Protobuf 定义区
- 日期：2026-09-15

## 原行为（Electron）

- `.time-workspace` 圆角 8 + 边；`.time-current-band` 最小 104、18/22 垫、12/600 标题、10 faint + 18 等宽值
- `.time-zone-band` 16/22 垫、11 muted 标签、快捷时区 27 高分段底
- `.regex-test-layout`：控件通栏 + `1fr` 源文 + 290 结果列；flags 为 10sp 勾选
- `.cron-fields`：七列 `minmax(65px, 1fr)`、9sp 标签、32 高输入；表达式栏 340；运行区通栏
- `.file-drop-row`：11 muted 文件名省略
- `.calculator-result`：最小 56、26 等宽 accent
- `.proto-definition`：0.34/0.66、11/600 头、10sp 选项

## 本产品行为

- 时间页工作区 8 圆角边；当前带 104 最小高、12 SemiBold、18 Medium 等宽；时区带与转换区去掉卡片壳
- 正则：表达式/flags 顶栏 14 垫；源文 `weight(1)`；结果固定 290、`surfaceSubtle` 左缝
- 正则 flags 改为 Checkbox + 10sp muted，不再用 `primary` 闩锁按钮
- Cron 七字段横排 compact 输入；预设 `FlowRow` 5 缝；表达式栏 340 `surfaceSubtle`；下次运行通栏两列 38 高
- 摘要/二维码 Logo/格式化文件名 11 muted 单行省略
- 计算器结果 56 最小、26 等宽 `accent`、日志 10 等宽底部分隔
- Protobuf JSON Tab 定义/转换 0.34/0.66，消息名 10sp + 104 Hex/Base64

时区快捷、Hex/Base64、工具栏「更多」仍用 `primary = expr`。

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

翻译工具栏 30dp 与 `FileDropRow` 见 [DIFF-094](094-style-filedrop-p5-toolbar.md)。file-drop 拖放区、Host 内嵌 `p5Toolbar`、系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
