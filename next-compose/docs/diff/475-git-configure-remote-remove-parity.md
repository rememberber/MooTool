# DIFF-475：Vault Git 清空 remote 与 Electron 对齐

## 背景

Electron `VaultGitService.configureRemote` 在保存空 URL 时执行 `git remote remove origin`；`status().remote` 变为空。Compose `GitEngine.setRemote` 已有相同逻辑，但缺少与 Vitest 用例「removes a configured remote when an empty URL is saved」及「Remote is already removed」的显式单测。

## 变更

- `GitEngineTest.configureRemoteRemoveClearsOriginFromStatus`
- `GitEngineTest.configureRemoteRemoveWhenAlreadyRemovedIsIdempotent`
- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 `configure-remote-remove`

## 验证

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest --offline
```
