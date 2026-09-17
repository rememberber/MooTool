# DIFF-577：F10/F08/F02 导入·导出失败 toast + Vault MCP 双库 notes read→json read + 帧 172

## 背景

DIFF-576 已做 F04 `JsonWiringPresentation.runReadImportFile`/`runWriteExportFile`、F06 导入失败 `reformat.error.read` 可见、`mooConfigConvertActions`、Vault MCP 双库 **notes read→json search** body offset、Compose 帧 `171`；本条**不重复** 576 上述链。parity-gap 仍列六套 CSS 逐选择器皮肤、产品主窗 Tab/IME/TCC PNG、P7 三平台安装、目标未达成。

## 行为

### F10 Host 方案导入·导出失败可见

- 导入/导出经 `HostWiringPresentation.runReadImportProfile`/`runWriteExportProfile` 失败时写 `reformat.error.read`/`reformat.error.write` 并 **error toast**（576 仅 F06 配置转换 Tab 补 toast，Host 仍只写 `session.error`）。

### F08 环境变量导出失败可见

- `EnvWiringPresentation.runWriteExport` 失败时写 `reformat.error.write` 到状态栏 + **error toast**（不再仅 `variables.error.generic`）。

### F02 文本对比导入失败可见

- `TextDiffPresentation.runReadImportFile` 失败时除状态栏外增加 **error toast**（对齐 F06 576 导入失败反馈链，非 JSON/Config run* 重复）。

### Vault MCP stdio（双库 + 跨库 read→read）

- `subprocessDualVaultNotesReadThenJsonReadHonorsBodyOffset`：双库 access 同会话 **notes read** body 提取 JSON 路径 hint 后再 **json read** body offset/length（**非** 576 notes read→json search / 574 notes search→json read / 566 双库并行 read 链）。

### 证据脚本

- `mootool_evidence_print_host_apply_button_tab_focus_hint`；`verify-product-evidence-prep.sh` 打印 F10 Compose 帧 `172` 提示。

### Compose 证据

- `HostApplyButtonCaptureTest` → `172-compose-host-apply-button-tab-focus.png`（`mooHostApplyButton`，非产品主窗）。

## 验证

- `HostApplyButtonCaptureTest` / `AiIntegrationVaultMcpConnectionTest` / 既有 Host/Env/Diff 单测
- `./scripts/verify-product-evidence-prep.sh`（语法 + hint）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

Electron 六套 CSS 逐选择器全量移植、产品主窗 F10/F08/F02/IME/TCC 手工 PNG、P7 三平台安装/公证、整产品完成、目标未达成。
