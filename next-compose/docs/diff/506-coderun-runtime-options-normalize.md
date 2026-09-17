# DIFF-506：代码运行 `runtime.options` 长度规范化

## 背景

Electron `normalizeSettings` 对 `runtime.drafts` 按 1 MiB、`runtime.options.arguments` 按 2000 字符、`workingDirectory` 按 1000 字符截断（见 `next/src/shared/contracts/settings.ts`）。DIFF-151 迁入 Electron 草稿/参数时仅对源码应用 `MAX_CODE_BYTES`，运行参数字符串与从磁盘恢复的会话快照仍可能超长，导致 `CodeRunEngine` 在校验阶段拒绝运行或持久化不可预期的大 JSON。

parity-gap **F05 代码运行** 引擎链；避开 DIFF-501～505 控件半径外观链。

## 行为

- **`CodeRunRuntimeOptionsNormalize`**：`MAX_ARGUMENTS_CHARS = 2000`、`MAX_WORKING_DIRECTORY_CHARS = 1000`，与 Electron 一致；草稿四语言同步 `CodeRunEngine.MAX_CODE_BYTES`。
- **`ElectronNextSettingsImport`**：`parseCodeRunPatch` / `mergeCodeRunSnapshots` 输出经规范化。
- **`CodeRunSession`**：`snapshotState` 写盘与 `restore` 读盘均规范化，兼容旧会话中的超长字段。

## 验证

- `CodeRunRuntimeOptionsNormalizeTest`
- `ElectronNextSettingsImportTest.loadCodeRunPatchTruncatesOversizedOptionsLikeElectronNormalize`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品窗 F05 运行参数走查帧、六套 CSS 逐选择器皮肤、设置未实现分类面板、Git/Vault 新切片、P7 三平台安装验收、hero/claude 内容区 shell 走查。
