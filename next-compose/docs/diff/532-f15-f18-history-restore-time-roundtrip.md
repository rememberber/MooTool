# DIFF-532：F15/F18 历史 options 闭环 + 本地时间严格校验

## 背景

DIFF-531 已登记 F18/F25 Electron fixture；F15 仍缺 `regexTools.test.ts` fixture 登记。F18 `localToTimestamp` 需对齐 Electron Luxon「解析后再格式化必须与输入一致」拒绝非法本地时间；F15/F18 历史恢复逻辑散落在 Screen 内，缺可单测的 metadata/restore 模块。

## 行为

### F18 时间

- `TimeHistoryMetadata`：历史 `options` / Electron `extraData`（`zone` + `unit`）。
- `TimeHistoryRestore`：恢复时区/单位与 input/output 方向（timestamp↔local）。
- `TimeEngine.localToTimestamp`：DST gap/overlap 后增加格式化 round-trip 校验（对齐 `timeTools.ts`）。
- `TimeConvertScreen` / `LegacyTimeConvertDraft` 改用上述模块。

### F15 正则

- `RegexHistoryMetadata` / `RegexHistoryRestore`：对齐 Electron `onApplyRecord`（pattern/source + flags，恢复后 UI 仍 `runMatch`）。
- `RegexElectronCatalogTest`：锁定 21 个常用模式 id 顺序与 Electron `regexTools.ts`。

## 验证

- `TimeHistoryMetadataTest` / `TimeHistoryRestoreTest` / `TimeEngineTest`（含 `24:00:00` 拒绝）
- `RegexHistoryRestoreTest` / `RegexElectronCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，829/829 通过）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、Electron 13 位时间戳长度启发式（仍见 DIFF-001 显式单位）、目标未达成。
