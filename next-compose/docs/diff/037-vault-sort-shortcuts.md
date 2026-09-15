# DIFF-037：Vault 名称/时间排序与编辑器查找格式化发送快捷键

- 编号：DIFF-037
- 影响：F01 随手记文档库、F04 JSON Vault、F09 HTTP、A01 快捷键帮助、编辑器 IME 焦点下的工具快捷键
- 日期：2026-09-15

## 原行为（Electron）

随手记文档库可按 `modified`（默认）/`created`/`name` 排序；JSON Vault 按 `name`（默认）/`modified`。目录始终在前；时间新→旧。JSON `Cmd/Ctrl+F` 打开查找，`Cmd/Ctrl+Shift+F` 格式化；随手记另有 `Cmd/Ctrl+S` 保存；HTTP `Cmd/Ctrl+Enter` 发送。

## 本产品行为

- `VaultEntry` 带 `createdAt`/`modifiedAt`：文件属性 ISO；随手记优先 frontmatter。
- `VaultSort.compare` 对齐 Electron `sortNodes`；`buildVaultTree` 对每层兄弟排序。
- 排序写入 JSON/随手记会话快照，重启恢复。
- 编辑器焦点走 RSTA `InputMap`（覆盖默认查找），Compose 控件焦点走页面 `onPreviewKeyEvent`。HTTP 发送不经过 RSTA。

## 理由

feature-parity 点名创建/修改时间排序；ui-spec 键盘表要求查找、格式化、保存、运行/发送在当前工具真实执行，不能只做工具栏按钮。

## 证据

`VaultSortTest`、`EditorBufferShortcutTest`、`NoteVaultTest` 时间戳。`desktopTest` **202/202**，见 `docs/evidence/2026-09-15-vault-sort-shortcuts/`。窗口手势、IME 手工、三平台安装仍未测。

## 受影响范围

- JSON Vault 无 created 排序，与 Electron 一致。
- 空/非法时间戳排在有时间的文件之后。
- Compose 场景 PNG 与 `JFrame.paint` 不能代替快捷键窗口验收。
