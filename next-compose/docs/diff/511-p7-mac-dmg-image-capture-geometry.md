# DIFF-511：macOS P7 本机 DMG 烟雾、F23 区域截图几何引擎

## 背景

DIFF-510「未做」仍列：`MOOTOOL_P7_BUILD_DIST=1` 本机执行、P7 三平台安装/公证、其余 F-tool 引擎切片。本条在 macOS 上跑通 **P7 可选分发构建** 并记录证据；引擎侧补 **F23 图片** 区域截图选区 `move`/`resize` 几何（对齐 Electron `captureSelection.ts`），不重复设置分组 CSS。

## 行为

### P7（仅本机 macOS）

- `MOOTOOL_P7_BUILD_DIST=1 scripts/prepare-p7-package-smoke.sh`：JDK 21、`verifyNativePackageMetadata`、`desktopTest --offline`，并执行 `:composeApp:packageDistributionForCurrentOS`。
- 产物：`composeApp/build/compose/binaries/main/dmg/MooTool Next Compose-1.1.0.dmg`（Darwin x86_64 本机）。
- **不**声明 Windows MSI / Linux DEB·RPM 已验收；安装、公证、升级/卸载仍手工未测。

### F23 图片引擎

- `ImageEngine.moveCaptureRect` / `resizeCaptureRect` + `CaptureResizeHandle`：与 Electron `moveCaptureRect` / `resizeCaptureRect` 同边界语义。
- `ImageEngineCaptureGeometryTest`（`ImageEngineTest.captureSelectionGeometryMatchesElectronCaptureSelectionFixture`）覆盖 `captureSelection.test.ts` 样本。

## 验证

- `docs/evidence/2026-09-17-p7-mac-package-smoke/results.md`
- `./gradlew :composeApp:verifyNativePackageMetadata :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具 Tab 走查、P7 三平台真实安装/公证/升级卸载、F23 选区手柄 UI 接线见 [DIFF-512](512-f23-capture-ui-command-tab-walkthrough.md)、Win/Linux 分发包构建。
