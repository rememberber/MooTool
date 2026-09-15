# DIFF-029：随手记 frontmatter、Vault 树 CRUD、Git 定时器、自定义分组与跨产品导入

- 编号：DIFF-029
- 影响：F01 随手记、F04 JSON、A01 布局/Vault/数据、A03 Git/导入
- 日期：2026-09-14

## 原行为（Electron）

随手记 `quickNoteVaultRepository.ts` 用 YAML frontmatter（`font_name`、`font_size: "15"`、`line_spacing: "1.0"`、`line_wrap: "1"|"0"`），正文检索，`.gitignore` 过滤，目录 CRUD/复制/移动，拖入编辑器。JSON Vault 同样有树 CRUD 与内容检索。Git 空闲/失焦自动检查点在有 remote 且 ahead/有变更时还会 push；自动 pull 按分钟。导航自定义分组可命名/排序/删除。跨产品导入只读检查 Java/Electron 来源后写入 Next 自己的库。

## 本产品行为

- `NoteFrontmatter` 用 SnakeYAML SafeConstructor 解析，手写序列化以保持 Java 兼容的引号标量。编辑器只显示正文。
- `NoteVault`/`JsonVault` 支持列表检索（路径/标题/可选正文）、gitignore、mkdir/rename/move/duplicate/递归删除。
- 编辑器 `TransferHandler` 接收文件：图片进附件目录，文本插入光标。不拦截普通文本粘贴。
- `VaultGitCheckpointScheduler` / `VaultGitPullScheduler` 对齐 Electron 空闲一次/失败重试；自动检查点仍可能 push（与 Electron 相同）。token 继续走 ASKPASS。
- 设置可 CRUD 自定义分组；侧栏 208–300 拖宽、双击恢复 248；窗口宽于 960dp 以下自动折叠导航与右栏。
- `CrossProductImporter` 支持 Electron `mootool-next.json`、Java `MooTool.db`（SQLite 副本只读）和笔记/JSON 目录。先备份本产品，不改来源，不把对方路径设为写入根。

## 理由

规格要求 frontmatter 不能当正文露出，全文检索与 Git 自动策略必须真实执行。导入必须是用户主动、可预览、可备份的适配器。

## 证据

`NoteFrontmatterTest`、`NoteVaultTest`、`JsonVaultTest`、`VaultGitCheckpointSchedulerTest`、`CrossProductImporterTest`。`desktopTest` 见 `docs/evidence/2026-09-14-f01-vault-git/`。IME/列编辑窗口手势、三平台安装、明暗截图未测。

## 受影响范围

- 旧笔记无 frontmatter 仍可当 legacy 正文读入。
- 自动检查点会在配置了 origin 时 push，与 Electron 一致；失败可见，不 hard reset。
- 六套 Electron CSS 皮肤、线性图标、IME 窗口验收仍未做。
