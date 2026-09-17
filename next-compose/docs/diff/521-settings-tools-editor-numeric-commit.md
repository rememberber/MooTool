# DIFF-521：工具默认值 QR/随机长度 + 编辑器字号失焦提交

## 背景

DIFF-520「未做」仍列设置 **工具默认值** 中 QR 尺寸/随机串长度（Electron `NumberSetting`）与 **编辑器** JSON/随手记字号（Compose 仍为分段控件、非 11–24 数值失焦链）。

## 行为

### A01 设置 · 工具默认值

- `SettingsToolsNumericNormalize`：`qrCodeSize`（120–2000）、`randomStringLength`（1–4096）失焦提交；非法整数 toast `settings.vault.numericInvalid` 并恢复已保存值。
- 设置页两处改 `SettingCommitTextField`（对齐 Electron `SettingsWindow` tools 分组 `NumberSetting`）。

### A01 设置 · 编辑器

- `SettingsEditorNumericNormalize`：`jsonFontSize` / `quickNoteFontSize`（11–24）失焦提交；非法整数同样 toast 并回滚显示。
- 设置页两处改 `SettingCommitTextField`（边界与 Electron `normalizeSettings` / `RangeSetting` min–max 一致；Compose 用数值框 + 失焦提交而非 slider 即时写入）。

## 验证

- `SettingsToolsEditorNumericNormalizeTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 联网/二进制大走查、PDF/加解密大切片、界面字号 `appearance.fontSize` 仍用分段（Electron 为 range 即时提交）。
