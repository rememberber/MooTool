# DIFF-474：Vault Git `fetch --prune` 与 behind 状态

## 背景

Electron `VaultGitService` 支持 `fetch`（`git fetch --prune origin`）；Compose `GitEngine.fetch` 已用于 Git 面板「拉取远程」前的 fetch 按钮，但缺少与 remote 领先时 **behind** 计数、且不自动 merge 工作区的集成单测。

## 变更

- `GitEngineTest.fetchUpdatesBehindWithoutPullingWorkingTree`：双 clone + 上游 push 第二笔提交 → client `fetch` 后 `behind >= 1` 且 `v2.txt` 仍不存在 → `pull` 后落盘。
- `GitEngineTest.fetchFailsWhenRemoteIsNotConfigured`。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
