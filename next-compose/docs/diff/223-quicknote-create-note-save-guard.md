# DIFF-223：随手记新建笔记前保存守卫

## 背景

Electron 在切换/新建前依赖自动保存与用户习惯；Compose 新建笔记分支调用 `saveIfNeeded` 但未检查返回值，保存失败或冲突时仍会 `createNote` 并切换，可能丢当前未保存内容。

## 行为

- `dialogMode == note`（新建笔记）时：`saveIfNeeded` 失败则 `runCatching` 失败，不创建新笔记、不 `openFile`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
