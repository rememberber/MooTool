# DIFF-347：随手记新建文件夹前保存守卫

## 问题

新建笔记对话框在 [DIFF-223](223-quicknote-create-note-save-guard.md) 已用 `saveIfNeeded` 失败即中止。**新建文件夹**分支在清空编辑器前调用 `saveCurrent` 但未检查返回值，保存失败或冲突时仍会清空 `currentFile` 与编辑器，可能丢失未落盘笔记。

## 行为

- `dialogMode == folder`：创建目录并切换到目录选中态前，先 `saveIfNeeded`；失败则中止，不清空编辑器。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
