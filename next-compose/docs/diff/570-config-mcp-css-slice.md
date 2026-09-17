# DIFF-570：F06 run* 接线 + F06 CSS + Vault MCP 双库 notes read offset + 帧 165

## 背景

DIFF-569 已做 F18 `TimeWiringPresentation.run*`、`mooTimeQuickZones`、Vault MCP 双库 **json read** offset、Host 方案行证据 hint、Compose 帧 `164`；本条**不重复** 569 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F06 配置 run* 接线

- `ConfigWiringPresentation.runToYaml` / `runToProperties` / `runValidate` / `runFormat`（`ConvertOutcome` 对齐 `CronWiringPresentation.runPreview`）。
- `ConfigConvertScreen` 转换/校验/格式化按钮走 Presentation，不再直接 `ConfigEngine` + 分散 `runCatching`。

### 样式（CSS 组件批次，非 569）

- `mooConfigValidateLayout`（F06 `.yaml-validate-layout` 14dp padding）。
- `mooConfigValidateActions`（F06 `.validate-actions` 160dp 中栏）。

### Vault MCP stdio（双库 + read offset 余量）

- `subprocessDualVaultNotesReadHonorsOffsetWhenJsonGranted`：双库 access 同会话 **notes** read offset/length（**非** 569 双库 json read / 567 双库 notes search offset 链）。

### 证据脚本

- `mootool_evidence_print_config_validate_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F06 Compose 帧 `165` 提示。

### Compose 证据

- `ConfigValidateCaptureTest` → `165-compose-config-validate-tab-focus.png`（非产品主窗）。

## 验证

- `ConfigWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `ConfigValidateCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
