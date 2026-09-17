# DIFF-527：托盘截屏权限文案 + PDF 书签 Electron 对齐 + HTTP multipart 准备

## 背景

DIFF-526「未做」中的托盘权限对话框代码路径、PDF 书签与 Electron `copyPages` 策略、可选 httpbin 公网 smoke 记录。不重复 526 的 AcroForm 合并单测与 QR fixture。

## 行为

### A03 托盘 / F23 / F18 取色

- `ScreenCaptureFailureMessages`：托盘取色/区域截图、调色板、图片工具共用权限与业务错误映射（`TrayDesktopActions` → `trayCaptureMessage`；`ColorBoardScreen` / `ImageScreen` 复用同模块）。
- macOS 打开「屏幕录制」系统设置 URI 与 `openedSettings` 双行文案仍由 `ScreenCaptureAccess` 提供；托盘未知异常回退 `image.error.capture`。

### F24 PDF

- 常量 `PdfEngine.ELECTRON_PARITY_OUTLINES_NOT_COPIED` 与单测断言：拆分/合并经 `importPage` 的子集**不**复制文档级书签（与 Electron `pdf-lib` `copyPages` 一致）；页级表单/签名字段保留见 [DIFF-526](526-http-pdf-merge-smoke-p7-slice.md)。

### F09 HTTP

- `HttpEngine.prepare`：请求已带 `Content-Type: multipart/form-data; boundary=…` 时正文按 UTF-8 字节原样发送（手工 Body 编辑器场景；无 Electron 式 multipart 文件 Tab，见 [DIFF-523](523-http-pdf-crypto-tray-update-slice.md)）。
- 可选：`MOOTOOL_HTTP_PUBLIC_SMOKE=1` 跑 `optionalHttpBinPublicGetSmoke`（acceptance 记录是否执行）。

## 验证

- `ScreenCaptureFailureMessagesTest`
- `PdfEngineTest.inspectCountsFormsBookmarksAndSignatureFields`（拆分后书签为 0）
- `HttpEngineTest.preparePostsMultipartBodyUnmodifiedWhenContentTypeHeaderPresent`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗全 Tab 走查、multipart **文件**上传 UI/公网走查、PDF 跨文档书签复制、P7 三平台安装/公证/升级卸载、托盘权限对话框**手工**验收、非对称 Tab 逐控件走查。
