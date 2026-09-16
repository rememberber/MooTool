# DIFF-079：首页分区、常用正则网格、计算器 log 与状态胶囊

- 编号：DIFF-079
- 影响：F00 首页；F15 常用正则；F21 计算器面板/log；F05 运行台检测胶囊；F18 时区胶囊
- 日期：2026-09-15

## 原行为（Electron）

- `.home-section` 为顶部分隔 + 12px 大写分区名，不是 inspector-card；贡献者圆形字头 + `--text-body`；作品列表 56px 行含描述；felixnan168/Lyp 走 Gitee
- `.common-pattern-list` 两列卡片 14px 内边距、7px 圆角、`--surface-soft`
- `.calculator-log` / `.operation-panel` 底 `--surface`、边 `--border-control`
- `.status-pill` 最小 34px；`--valid`/`--error` 用 success/error 底色

## 本产品行为

- 首页改为分区线布局；贡献者/作品行对齐；外链与 Electron 一致
- 常用正则两列可点卡片；计算器运算面板与 log 走 workspace + `borderControl`
- `MooStatusPill` 增加 Valid/Error；运行台检测与时间时区接入

## 理由

首页此前整块卡片化，贡献者/作品缺少 Electron 行结构，且两条贡献者链接指错主机。

## 证据

`HomeLinksTest`、`StatusPillTest`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
