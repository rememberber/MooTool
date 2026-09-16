# DIFF-087：翻译工具栏、单词本/历史、P5 输入密度

- 编号：DIFF-087
- 影响：F20 翻译；F09 HTTP 集合；F10 Host 编辑条；P5 输入 30/11
- 日期：2026-09-15

## 原行为（Electron）

- `.translation-toolbar`：最小 46、7/10 垫、10sp 标签、select ≥130
- `.translation-editor-grid`：两列无圆角、中缝 `border-soft`；结果脚 34、10sp
- `.translation-record-layout`：220 侧栏、7 垫、42 头、11/10 记录
- `.translation-history-list`：11/9/11 字号
- `.p5-tool input/select`：高 30、11 字号、圆角 6
- `.http-workspace` 210 集合；`.host-workspace` 220 方案；`.host-toolbar` 同 46

## 本产品行为

- 翻译语言条走工具栏底；源/目标/引擎按钮 `widthIn(min=130)`；自动仍 `primary = expr`
- 翻译编辑无边框 14 字号 20 垫；结果脚 34、10 muted
- 单词本默认 220、`surfaceSubtle`、头 42、保存靠右
- 历史行 11 SemiBold / 9 时间 / 11 正文
- `MooTextField(dense=true)`：30 高、11 字号；HTTP URL/超时与 Host 名称使用
- HTTP 集合默认 210、头 44、脚 40；Host 方案默认 220、编辑条 46

## 证据

`desktopTest` 见 `docs/acceptance.md`。Zulu 21.0.12.1、`--offline`。

## 未做

Net/环境/硬件其余 `.p5-tool` 输入未全部改 dense。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
