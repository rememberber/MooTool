# DIFF-443：Git `index.lock` 陈旧锁恢复与并发提交序列化

## 背景

Electron `VaultGitService.runWithIndexLockRecovery` 在 `index.lock` 冲突时延迟重试、隔离超过 5 分钟的陈旧锁（可选 `lsof` 检测占用）后重试。Compose `GitEngine.run` 此前直接失败。Electron 还对同 Vault 目录序列化 Git 操作；Compose 已有 `locked(root)`，需用单测锁定并发提交语义。

## 行为

- `GitEngine.run`：index.lock 失败 → 250ms 重试 → 陈旧锁隔离为 `index.lock.mootool-stale-*` 后再执行（隔离文件在 finally 删除）。
- macOS/Linux 在隔离前用 `lsof -t` 检测锁文件是否仍被占用（与 Electron 一致；无 `lsof` 时视为未占用）。
- `GitEngineTest`：`repairsStaleIndexLockAndRetriesCommit`、`recentIndexLockBlocksCommit`、`serializesConcurrentCommitsForSameVaultRoot`（对齐 `vaultGitService.integration.test.ts`）。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（577/577）
