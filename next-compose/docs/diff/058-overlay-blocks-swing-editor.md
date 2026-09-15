# DIFF-058：历史/Git/冲突弹层与 Swing 编辑器互斥

- 编号：DIFF-058
- 影响：A02 历史；A03 Git；F01/F04 冲突对话框；编辑器叠层点击
- 日期：2026-09-15

## 原行为（Electron）

历史、Vault Git、外部冲突都是应用内遮罩，点空白关闭。页面没有独立的 Swing 层，遮罩能挡住编辑器。

## 本产品行为

- `HistoryBrowser`、`VaultGitDialog`、`VaultConflictDialog` 改为 `MooOverlay`（实底 + 点遮罩关闭 + Esc）。
- `ModalOverlayState`：打开 overlay 时 `EditorHost` 暂时不挂 `SwingPanel`，避免 AWT 编辑器浮在 Compose 遮罩之上抢走点击（此前分组弹层盖在 JSON/HTTP 上时，取消/保存点不到）。
- Workbench 的 Cmd/Ctrl+K、Esc 提到 `BoxWithConstraints`，遮罩打开时快捷键仍有效。

## 理由

Compose Desktop 的 `SwingPanel` 是重型组件，会盖住同窗口里的 Compose 遮罩。Electron 没有这个问题。弹层打开时卸下编辑器宿主，关闭后文本仍在 `EditorBuffer`。

## 证据

`desktopTest` **240/240**。分组弹层在有编辑器的页面上点不透的问题已编码修复；产品主窗需用本切片代码重拍才能作为点击验收。

## 受影响范围

- 工具内仍有部分 `Dialog`（图片/HTTP 小对话框等）未全部改为 overlay。
- 产品主窗 Tab/IME、托盘 TCC、六套 CSS、三平台安装未测。
