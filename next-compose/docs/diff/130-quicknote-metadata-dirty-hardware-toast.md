# DIFF-130：随手记 metadata 脏状态与系统信息 toast

## 随手记 metadata 脏状态（F01）

对照 Electron `metadataDirty`：

- 会话增加 `savedMetadata`，打开/保存后与磁盘元数据对齐。
- `quickNoteDirty()` 同时比较正文与 metadata；未保存时切回工具页不自动重载、状态栏显示未保存、`saveIfNeeded` 在仅改字体/语法/颜色等时也会保存。
- 自动重载仅在正文与 metadata 均未脏且磁盘有变化时执行。

## 系统信息（F25）

- 刷新采集成功/失败 toast；复制 Tab 内容沿用 `copyText` 全局 toast，去掉重复 `setStatus`。

## 文件

- `SessionManager.kt`（`QuickNoteSession`）、`QuickNoteScreen.kt`、`HardwareScreen.kt`
