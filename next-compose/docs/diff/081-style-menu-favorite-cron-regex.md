# DIFF-081：上下文菜单、收藏行、Cron 运行格与正则结果卡

- 编号：DIFF-081
- 影响：A01/A02 菜单与收藏；F01 颜色触发；F10 Host 右键；F15 正则结果；F16 Cron 运行
- 日期：2026-09-15

## 原行为（Electron）

- `.quick-note-tree-menu` / `.vault-tree-menu` / `.host-profile-menu`：最小 156、内边距 5、圆角 7、条目 30×11、悬停 `--control-hover`
- `.favorite-item`：`--surface-soft`、12/10 字号、右侧图标删除
- `.cron-runs`：两列、1px 缝、38 高、等宽序号 + 10px 等宽时间
- `.regex-results article`：`--surface`、6 圆角、9/11 等宽
- `.quick-note-color-trigger`：36×32、16 圆点；菜单 4 列 28 色块

## 本产品行为

- 新增 `MooMenu` / `MooMenuItem` / `MooMenuSeparator`，用于「更多」、设置下拉、Vault/Host 右键、随手记语法/溢出菜单
- 正则/Cron 收藏改 `FavoriteRow`；历史清空走 danger
- Cron 下次运行两列格；正则命中卡对齐结果文章密度
- 随手记颜色改为色点触发器 + 四列色板

## 理由

Material 默认菜单行 48dp，树/溢出菜单与 Electron 桌面密度差一截。收藏与 Cron 运行列表此前用通用主按钮和大号正文。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

HTTP Method 等其余下拉仍是 Material 默认行高。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
