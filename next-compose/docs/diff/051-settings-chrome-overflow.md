# DIFF-051：设置页 Electron 分组/分段/色板与二维码/时间溢出

- 编号：DIFF-051
- 影响：A01 设置壳；F17 二维码、F18 时间、F22 调色板标题栏溢出；ui-spec L7
- 日期：2026-09-15

## 原行为（Electron）

设置页左侧分类导航有选中条；右侧按 `settings-group` 分组，行内 `setting-row` 左标签右控件。关闭行为与主题用 segmented；界面风格与语言用 compact `<select>`；强调色用 yellow/coral/blue/green/red/purple 色板，不是一排文字按钮。二维码/时间在窄宽度下不应把分离窗口铺成常驻主键。

## 本产品行为

- 11 类设置拆成与 Electron 对应的组：应用行为、主题与字体、功能导航、编辑体验、代理/超时、数据目录/备份/导入、Vault 路径/Git、运行时、工具默认、快捷键。
- 语言、界面风格、SQL 方言改为下拉；关闭行为/主题/导航样式/字号/纠错等用分段控件；开关用轨道开关。
- 强调色色板对齐 Electron 六色；旧设置里的 `orange`/`teal` 分别映射到 `yellow`/`green`，仍能解析。
- 二维码标题栏分离、时间历史/分离、调色板标题栏分离、JSON/随手记/格式化标题栏分离在内容宽 < 1440 时收入「更多」。时间页保留时钟主按钮。

## 理由

设置页原先把字段铺成扁平按钮列，1080 宽挤掉对照关系；强调色 id 与 Electron 不一致。二维码/时间是最后未收的标题栏分离按钮。

## 证据

`ThemeContrastTest.electronAccentIdsMatchPresets`。`desktopTest` **235/235**，见 `docs/evidence/2026-09-15-settings-chrome/`。真实窗口截图未取。

## 受影响范围

- 仍非 Electron 六套 CSS 逐选择器移植。
- 设置页没有运行窗口截图，不能标 A01 已验收。
- IME/列编辑手工、托盘权限、三平台安装未测。
