# DIFF-138：Git remote URL 校验

对照 Electron `vaultGitService.normalizeRemote`。

## 范围

- `GitEngine.normalizeGitRemote` / `setRemote`：仅允许 `http(s)://`、`ssh://`、`git://`、`git@`、`file://`；拒绝 `javascript:` 等；空字符串用于移除 remote。
- 单测：`rejectsUnsafeGitRemotes`。

## 文件

- `GitEngine.kt`、`GitEngineTest.kt`
