# DIFF-038：Vault 全文检索内存索引

- 编号：DIFF-038
- 影响：F01 随手记文档库检索、F04 JSON Vault 检索
- 日期：2026-09-15

## 原行为（规格）

feature-parity 要求名称/路径/正文检索分清用途，全文搜索走后台索引，不能在输入每个字时同步扫描整个文件夹。

## 本产品行为

- `NoteVault.snapshot` / `JsonVault.snapshot` 在文件变更、库路径或 gitignore 设置变化时于 IO 线程重建，读取标题与正文一次。
- 搜索关键字与「搜正文」开关只过滤内存中的 `VaultIndexRecord`，不 `Files.walk`。
- 目录名命中时保留该目录及后代；文件命中时保留祖先目录。关闭「搜正文」时不匹配正文。

## 理由

每次按键 `list()` 会走盘并解析 frontmatter，库稍大就会卡住输入。索引必须改变查询路径，不能只做 debounce 假装后台。

## 证据

`VaultSearchIndexTest`。`desktopTest` **204/204**，见 `docs/evidence/2026-09-15-vault-search-index/`。窗口截图、IME 手工、三平台安装仍未测。

## 受影响范围

- 仍是进程内快照，不是落盘搜索引擎；进程退出后下次打开会重建。
- `list(query)` 测试入口仍会当场 snapshot+filter，行为与过滤结果一致。
