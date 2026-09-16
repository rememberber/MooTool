# DIFF-082：紧凑搜索、HTTP 表、菜单全覆盖、翻译/图片/UA 列表

- 编号：DIFF-082
- 影响：A01 菜单；F09 HTTP 集合搜索与 Params/Cookie 表；F10 Host 搜索；F04 Vault 搜索；F20 单词本；F23 图片库；F12 UA 结果
- 日期：2026-09-15

## 原行为（Electron）

- `.compact-search`：30 高、内输入透明、悬停/焦点边
- `.http-entry-row`：勾选 14、字段 27、底部分隔、空表添加为 control 条
- 工具下拉与溢出菜单与树菜单同一密度
- `.translation-record` / `.image-list-item`：`--control` 悬停选中、11/10 或 10 字号
- `.ua-result-panel`：两列 1px 缝、72 高卡片

## 本产品行为

- 新增 `MooCompactSearch`；HTTP/Host/JSON Vault/翻译搜索改用它
- 其余 `DropdownMenu` 改 `MooMenu`（含 HTTP Method/Body、「更多」、时区、纠错、编码等）
- HTTP Params/Cookie 改表头+勾选+紧凑字段+28dp 删除
- 翻译单词本、图片库行、UA 结果格对齐对应选择器

## 理由

DIFF-081 只覆盖了部分菜单；集合搜索仍走 34dp 通用输入；HTTP 表用 TabChip/主按钮，密度高于 Electron。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
