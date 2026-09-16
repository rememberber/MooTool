# DIFF-213：随手记 Action 对话框文案与重命名默认值

## 背景

Electron `ActionDialog` 对文件重命名预填 `metadata.title`；目录用路径末段；主按钮为「创建/应用/删除」，次按钮为「取消」。

Compose 此前工具栏重命名预填含扩展名的文件名；树右键未读非当前笔记的 title；主按钮统一为「保存」，次按钮为「关闭」。

## 行为

- `quickNoteRenameDefault`：与 Electron `openAction` / `openTreeAction` 一致。
- 树右键重命名非当前文件时 `readNote` 取 title。
- 对话框主按钮：`note`/`folder` → `quickNote.create`；`rename`/`move` → `quickNote.apply`；`delete` → `quickNote.delete`；次按钮 `common.cancel`；空名称时禁用主按钮（移动除外）。

## 验证

- `QuickNoteRenameDefaultTest`
- `./gradlew :composeApp:desktopTest --offline`
