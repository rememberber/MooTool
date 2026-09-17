# DIFF-478：Vault Git `fetch --prune` 清理已删远程分支

## 背景

Electron `VaultGitService` 的 `fetch` 动作为 `git fetch --prune origin`（见 `vaultGitService.ts`）。DIFF-474 已验证 fetch 后 `behind` 计数与不自动 merge 工作区，但未覆盖 **`--prune` 删除远端已移除分支的 `origin/*` tracking ref**。Compose `GitEngine.fetch` 同样带 `--prune`，需集成单测锁定。

## 变更

- `GitEngineTest.fetchPruneRemovesStaleRemoteTrackingBranch`：上游 push `feature` → client fetch 可见 `origin/feature` → 上游 `--delete feature` → client 再 fetch 后 remote 列表不含 `origin/feature`。
- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 `fetch-prune-stale-ref`。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
