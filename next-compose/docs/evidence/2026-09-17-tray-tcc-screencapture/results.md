# 证据：托盘 / 屏幕录制 TCC（2026-09-17）

- 准备：`./scripts/prepare-tray-screencapture-evidence.sh`（无 GUI；从既有产品窗帧恢复 `reference/57-color-baseline.png`）
- 校验：`./scripts/verify-product-evidence-prep.sh` 或 `ProductEvidencePrepScriptTest.prepareTrayScreencaptureScriptRestoresBaselinePng`
- 基线帧（调色板工具页，**非** TCC 系统对话框）：`reference/57-color-baseline.png`（源：`docs/evidence/2026-09-15-inspector-screencapture/windows/57-color.png`）

## 手工项（未执行则 acceptance 标未测）

1. 隔离数据目录 + `runDistributable`（见脚本输出）。
2. macOS：关闭 MooTool「屏幕录制」权限。
3. 托盘 → 取色 / 区域截图：error 行应走 `ScreenCaptureFailureMessages.trayCaptureMessage`（`color.error.permission`）。
4. F22 / F23 屏幕取色：同上；可选「打开系统设置」双行文案（`ScreenCaptureAccess.userMessage`）。
5. 重新授权后成功取色；保存 PNG 至本目录 `captures/`（需人工 GUI）。

## 自动化

- `ScreenCaptureFailureMessagesTest`、`ScreenColorSamplerTest`（URI / openedSettings 文案）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
