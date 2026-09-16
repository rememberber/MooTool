# DIFF-442：`GitEngine.fileDiffs` 路径规则 + Electron `vaultGitService` 对照单测

## 背景

Electron `VaultGitService.diff` 对非法路径抛 `Invalid Git path`，对已指定但工作区无对应变更的路径返回空 `files`。Compose `fileDiffs` 曾把 `../outside` 当成「全部变更」，并对无 porcelain 项的路径合成 HEAD↔工作区 diff，与 Electron 不一致。

## 行为

- `path` 非空但 `normalizeGitPath` 失败 → `IllegalArgumentException("Invalid Git path")`（`VaultGitDialog` 已有 toast 捕获）。
- `path` 合法但 `status.changes` 无匹配项 → 空列表（对齐 `valid..name.json` 用例）。
- 新增 `GitEngineTest`：`fileDiffsMatchesElectronPathRules`、`automaticCheckpointInitializesRepositoryOnFirstUse`、`automaticCheckpointPushesWhenRemoteConfigured`、`unicodePathsWorkForStatusDiffDiscardAndRename`；加强嵌套 Vault 初始化不吞父仓未跟踪文件。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（574/574）
