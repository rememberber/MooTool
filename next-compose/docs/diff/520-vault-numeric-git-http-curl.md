# DIFF-520：Vault 数值失焦提交 + Git merge/rebase 中止文案 + F09 cURL fixture

## 背景

DIFF-519「未做」仍列设置 Vault 数值失焦链、HTTP 大切片、Git pull/rebase 可感知性。Vault 自动提交/拉取间隔仍用 `SettingTextField` 逐字写入；Git 中止与确认文案不区分 merge/rebase；F09 cURL 单测未登记 Electron `httpTools.test.ts` 第二条 round-trip。

## 行为

### A01 设置 · Vault 数值

- `SettingsVaultNumericNormalize`：`autoCommitIdleSeconds` / `autoCommitInactiveSeconds`（5–3600）、`autoPullMinutes`（0–1440）失焦提交；非法整数 toast `settings.vault.numericInvalid` 并恢复已保存值。
- 设置页三处改 `SettingCommitTextField`（对齐 Electron `NumberSetting` + `clampNumber`）；`autoPullMinutes` 仍经 `AppContainer.updateSettings` 触发 pull 调度 `resetIntervalClock`。

### A03 Vault Git UI

- `GitOperationPresentation`：merge/rebase 分别映射 `git.abortMergeOnly` / `git.abortRebase` 与 `git.confirmAbortMerge` / `git.confirmAbortRebase`；未知操作回退 `git.abortMerge` / `git.confirmAbort`。
- Pull 按钮：`pullEnabled` 要求 remote、非 merge/rebase 进行中且 `conflicts == 0`（与 push 冲突期禁用一致）。

### F09 HTTP

- `HttpEngineTest.curlRoundTripMatchesElectronHttpToolsAcceptAndBody` 对齐 Electron `round trips the important request fields`。
- `docs/fixtures/electron-next-httpTools-vitest.md` 登记。

## 验证

- `SettingsVaultNumericNormalizeTest` / `GitOperationPresentationTest` / `HttpEngineTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 联网/二进制大走查、PDF/加解密大切片；工具 QR/随机长度与编辑器字号失焦提交见 [DIFF-521](521-settings-tools-editor-numeric-commit.md)。
