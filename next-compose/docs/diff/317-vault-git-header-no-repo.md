# DIFF-317：Vault Git 顶栏状态文案对齐 Electron

## 问题

未检测到 Git 时，Compose 顶栏用红色 `git.unavailable`；Electron 顶栏仍用 `json.git.noRepo`，「未检测到 Git 命令」仅出现在工作区空态。

## 行为

顶栏 `strong` 区与 Electron 一致：未初始化仓库（含 Git 不可用）显示 `git.noRepo`；已初始化则显示分支与 ahead/behind。`git.unavailable` 仍仅在工作区居中空态展示。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
