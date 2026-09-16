# DIFF-083：硬件组、环境表、PDF 表、留言板预设、色片悬停

- 编号：DIFF-083
- 影响：F25 系统信息；F08 环境变量；F24 PDF；F19 留言板；F22 调色板
- 日期：2026-09-15

## 原行为（Electron）

- `.hardware-group`：12/600 标题、两列 dt/dd、33 高、11 字号、`--border-soft` 分隔
- `.environment-table`：34 高 10/600 表头、36 高 11 字号行、键 10 等宽、右侧 116 动作、紧凑搜索
- `.pdf-table`：36 高 10/600 粘性表头、11 字号、9 字号副文、紧凑字段、图标删除
- `.message-board-preset`：两列 34 高、10 字号、7 主题点
- `.color-chip:hover`：2px accent 外描边、1px offset

## 本产品行为

- 硬件组两列定义列表与组间分隔
- 环境表改 `MooCompactSearch`、34/36 表头表行、28dp 复制/删除
- PDF 拆分/合并表头走 toolbar 底、紧凑字段、28dp 删除、完成/失败状态色
- 留言板 8 预设改两列芯片 + 主题色点；选中仍用 `session.message == label`
- 色片悬停画 2dp accent 外框

## 理由

DIFF-082 覆盖了搜索/菜单/HTTP 表，这批选择器仍是工具页密度缺口。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

crypto-panel / encode / qr / net / checkbox-row 仍待下一批。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
