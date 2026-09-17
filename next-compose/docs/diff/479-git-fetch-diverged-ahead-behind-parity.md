# DIFF-479：Vault Git 分叉后 `fetch` 同时更新 ahead/behind

## 背景

Electron `VaultGitService.status` 从 `git status --branch` 解析 `ahead`/`behind`，Git 面板「拉取远程」前的 `fetch` 动作为 `git fetch --prune origin`（见 `vaultGitService.ts`）。DIFF-474/478 已覆盖单向落后与 prune tracking ref，但未覆盖**本地与远端各自有新提交**时 fetch 后同时显示 ahead 与 behind，且不自动 merge 工作区。

## 变更

- `GitEngineTest.fetchReportsAheadAndBehindWhenBranchesDiverge`：client 本地提交、upstream 再提交并 push → client `fetch` 后 `ahead >= 1` 且 `behind >= 1`，`remote-only.txt` 仍不存在。
- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 `fetch-diverged-ahead-behind`。

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
