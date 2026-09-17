# DIFF-569：F18 run* 接线 + F18/F10 CSS + Vault MCP 双库 json read offset + 帧 164

## 背景

DIFF-568 已做 F09/F10 集合 `mooHttpSavedList`/`mooHttpSavedItem`、F20 `canRunTranslate`、Vault MCP 双库 **json search** offset、HTTP 集合 saved item 证据 hint、Compose 帧 `163`；本条**不重复** 568 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F18 时间 run* 接线

- `TimeWiringPresentation.runTimestampToLocal` / `runLocalToTimestamp`（`ConvertOutcome` 对齐 `CronWiringPresentation.runPreview`）。
- `TimeConvertScreen` 转换按钮走 Presentation，不再直接 `runCatching { TimeEngine… }`。

### 样式（CSS 组件批次，非 568）

- `mooTimeQuickZones`（F18 `.time-quick-zones` segmented 容器）。

### Vault MCP stdio（双库 + read offset 余量）

- `subprocessDualVaultJsonReadHonorsOffsetWhenNotesGranted`：双库 access 同会话 **json** read offset/length（**非** 568 双库 json search offset / 563 单库 json read offset / 566 双库 read offset=0 链）。

### 证据脚本

- `mootool_evidence_print_host_profile_item_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F10 Compose 帧 `164` 提示。

### Compose 证据

- `HostProfileItemCaptureTest` → `164-compose-host-profile-item-tab-focus.png`（非产品主窗，与 `133` 搜索框互补）。

## 验证

- `TimeWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `HostProfileItemCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
