# DIFF-080：历史行、色板、集合列表、快速替换、kbd 与 YAML 校验

- 编号：DIFF-080
- 影响：A02 历史；A01 强调色/快捷键；F09 HTTP 集合；F10 Host 方案；F01 快速替换；F06 YAML 校验
- 日期：2026-09-15

## 原行为（Electron）

- `.history-item`：底部分隔、13/10/11 字号、右侧 28px 图标删除
- `.color-swatch--active`：外圈为本色 4px 环
- `.http-saved-item` / `.host-profile`：悬停/选中 `--control`，11/10 字号
- `.quick-replace-actions button`：10px、最小 30、`--surface` + `--border-control`
- `.setting-row kbd`：最小 84px、底边 2px；快捷键旁展示 `formatShortcut`
- `.validation-output` / `--error`：success/error 底色

## 本产品行为

- `HistoryBrowser` 行结构对齐；删除改 28dp ghost
- 强调色色板选中环对齐；HTTP/Host 列表悬停选中走 `control`
- 快速替换改紧凑列表按钮；快捷键行保留可编辑字段并加 `MooKbd`
- YAML 校验输出走 Valid/Error 底色

## 理由

历史/集合/快速替换此前用通用主按钮与蓝色选中底，和 Electron 工具侧栏密度不一致。快捷键需要可见 kbd 同时保留冲突校验字段。

## 证据

`ShortcutBindingsTest.formatDisplayUsesPlatformMetaKey`。`desktopTest` 见 `docs/acceptance.md`。

## 未做

仍不是六套 CSS 全文移植。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
