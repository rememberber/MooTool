# DIFF-568：F09/F10 集合行 CSS + F20 run* 守卫 + Vault MCP 双库 json search offset + 帧 163

## 背景

DIFF-567 已做 F09 响应头 bar/状态色/响应编辑器内边距、F01 随手记 Git flush Presentation、Vault MCP 双库 **notes** search offset、证据 hint、Compose 帧 `162`；本条**不重复** 567 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### 样式（CSS 组件批次，非 567）

- `mooHttpSavedList`（F09/F10 `.http-saved-list` / `.host-profiles > div` 5px 内边距）。
- `mooHttpSavedItem` 内聚 8×9 内边距与 3dp 行距；F09 集合行 Method 用 accent 字重（对齐 `.http-saved-item em`）。
- F10 Host 方案列表复用 `mooHttpSavedItem` / `mooHttpSavedList`（对齐 `.host-profile`）。

### F20 翻译 run* 接线

- `TranslationWiringPresentation.canRunTranslate`；翻译 Tab「立即翻译」在空源/在途/超长时禁用。

### Vault MCP stdio（双库 + offset 余量）

- `subprocessDualVaultJsonSearchHonorsOffsetWhenNotesGranted`：双库 access 同会话 **json** search offset/limit（**非** 567 双库 notes offset / 564 单库 json offset 链）。

### 证据脚本

- `mootool_evidence_print_http_collection_saved_item_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F09 Compose 帧 `163` 提示。

### Compose 证据

- `HttpSavedItemCaptureTest` → `163-compose-http-saved-item-tab-focus.png`（非产品主窗）。

## 验证

- `TranslationWiringPresentationTest` / `AiIntegrationVaultMcpConnectionTest` / `HttpSavedItemCaptureTest`
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
