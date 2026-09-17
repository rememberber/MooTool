# DIFF-526：HTTP 公网 smoke + PDF 合并表单保留 + F17/P7 文档

## 背景

DIFF-525「未做」中的 HTTP 公网走查、PDF 合并后表单/签名字段保留、产品证据脚本与 P7 说明。本切片不重复 525 的非对称 Tab / 本机 Cookie 单测。

## 行为

### F09 HTTP

- `HttpEngine.isPublicSmokeUrlAllowed` / `resolvePublicSmokeUrl`：仅允许 `httpbin.org`（含子域）与 `localhost`/`127.0.0.1`。
- `HttpEngineTest.optionalHttpBinPublicGetSmoke`：需 `MOOTOOL_HTTP_PUBLIC_SMOKE=1`；默认 CI/`desktopTest --offline` 跳过；无网时 `Assume` 跳过并记 acceptance **未测**。
- `HttpEngineTest.publicSmokeAllowlistPermitsHttpBinAndLocalhostOnly`：离线必跑。

### F24 PDF

- 合并/拆分：`importPage` 子集经 `PDFMergerUtility.appendDocument` + `AcroFormOrphanWidgetsProcessor`/`AcroFormDefaultFixup` 保留页级表单/签名字段 widget（对齐 inspect 计数）；文档级书签仍不复制（与 Electron `pdf-lib` `copyPages` 一致）。
- 单测：`mergePreservesImportedPageFormAndSignatureWidgets`、`inspectCountsFormsBookmarksAndSignatureFields` 加强拆分/合并结构断言。

### F17 二维码

- `QrEngine.normalizeSize(Double)` 对齐 Electron `qrTools.test.ts` 的 `360.4 → 360`；fixture 见 `docs/fixtures/electron-next-qrTools-vitest.md`。

### 产品证据 / P7

- `scripts/lib/product-evidence-common.sh`：`mootool_evidence_print_http_public_smoke_hint`。
- `verify-product-evidence-prep.sh`、`prepare-p7-package-smoke.sh`、`scripts/README-product-evidence.md` 登记可选 HTTP smoke 与 P7 离线门禁关系。

## 验证

- `PdfEngineTest.mergePreservesImportedPageFormAndSignatureWidgets`
- `HttpEngineTest.publicSmokeAllowlistPermitsHttpBinAndLocalhostOnly`
- `QrEngineTest.normalizesSizeAndRejectsEmptyOrBrokenImages`（含 `360.4`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
- 可选：`MOOTOOL_HTTP_PUBLIC_SMOKE=1` 跑 `optionalHttpBinPublicGetSmoke`（有网时）

## 未做

六套 CSS 皮肤、产品主窗全 Tab 走查、multipart 文件上传公网走查、PDF 书签/跨文档 AcroForm 完整策略安装镜像验收、P7 三平台安装/公证/升级卸载、托盘权限对话框与非对称 Tab 逐控件手工走查。
