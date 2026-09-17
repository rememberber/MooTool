# DIFF-515：Vault Git merge/rebase 状态 UI + push 守卫 + HTTP/翻译代理运行时

## 背景

DIFF-514 已为设置网络代理做失焦提交与加载规范化；F09/F20 发送仍直接读 `NetworkSettings` 原始字段。parity-gap 仍列 Vault Git pull/push/rebase 工作流可感知性；`GitEngine.pull` 在 merge/rebase 中会拒绝，但 `push` 仅依赖 UI 禁用。

## 行为

### A03 Vault Git

- `GitEngine.push`：merge/rebase 进行中与 `pull` 同样拒绝（文案含 merge/rebase / pushing）。
- `GitOperationPresentation` + `VaultGitDialog`：进行中显示 `git.operationMerge` / `git.operationRebase` 状态胶囊；有变更或冲突时显示 `git.counts`（仅 merge/rebase/冲突上下文，不恢复 DIFF-315 移除的常显 counts）。
- `GitRebaseGuardTest.pushBlockedWhileRebaseInProgress`；`GitOperationPresentationTest`。

### F09 HTTP / F20 翻译

- `NetworkSettings.toHttpProxyConfig()`：发送前 trim host/凭据、`sanitizeProxyPort` 合法端口（对齐 `SettingsNetworkNormalize`）。
- `HttpScreen` / `TranslationScreen`（含单词本重译）统一走该映射。
- `NetworkRuntimeTest`。

## 验证

- `GitOperationPresentationTest` / `NetworkRuntimeTest` / `GitRebaseGuardTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗冲突/Git merge/IME 截图、六套 CSS 皮肤、P7 Win/Linux 安装、`runDistributable` 烟雾、设置 TextField 链、其余 F-tool 大切片。
