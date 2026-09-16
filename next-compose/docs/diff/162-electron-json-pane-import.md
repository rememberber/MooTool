# DIFF-162：Electron JSON 分栏比例迁入 compose dp

## 背景

Electron `layout.paneSizes` 以 `json-three-pane` / `json-two-pane` 存归一化列宽比例；compose 以 `json` 工具 id 存 Vault/检查器绝对 dp。直接合并会把 `0.19` 之类写入设置，分栏宽度失效。

## 变更

- `ElectronPaneSizeImport.mergeElectronIntoCompose`：按 1320dp 参考宽把 Electron JSON 比例转为 Vault（index 0）与检查器（index 1）dp，并夹在现有 min/max 范围。
- `ElectronNextSettingsImport.mergeInto` 合并布局时调用上述转换，保留 compose 已有其它工具分栏。

## 测试

- `ElectronPaneSizeImportTest`

## 未覆盖

- 其它 Electron `storageKey`（HTTP/Host/QuickNote 组合键等）仍待逐工具映射。
