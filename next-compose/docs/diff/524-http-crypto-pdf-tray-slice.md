# DIFF-524：HTTP cURL urlencode + 对称密钥字节 UI + PDF 加密/UI + 托盘 Host 菜单

## 背景

DIFF-523 已补 HTTP NUL/`--data-binary`、multipart 响应预览与更新调度 tick。本切片继续 DIFF-523「未做」中的 HTTP/Electron 差异、加解密 UI、PDF 加密走查与托盘路径，避免重复 523 的同类单测堆叠。

## 行为

### F09 HTTP

- `HttpEngine.parseCurl`：`--data-urlencode` 按 UTF-8 URL 解码正文；无 `Content-Type` 时默认 `application/x-www-form-urlencoded`（对齐 `httpTools.ts` 对 urlencode 参数的解析语义）。

### F14 加解密

- `CryptoEngine.symmetricKeyUtf8Length`：按与加密相同的截断/补 0 规则计算 UTF-8 字节数。
- 对称 Tab 密钥行旁实时显示 `{current}/{required}`，非法长度用 warning 色（对齐 `crypto.keyHint` / `invalid-key` 前置反馈）。

### F24 PDF

- Tab 工具栏显示 Electron 同款限制文案（20 项、仅未加密）。
- 导入加密 PDF 时除状态栏外弹出 `toastError`（`encrypted`）。

### A03 托盘

- `AppContainer.hostProfileMenuRevision` + Host 方案增删改/复制后 `notifyHostProfileMenuChanged()`；`Main.kt` 托盘 `LaunchedEffect` 监听该 revision，Host 列表变更时重建菜单（补充仅依赖工具 `revision` 时未切 Host 页的缺口）。

## 验证

- `HttpEngineTest.parseCurlDataUrlencodeDecodesBodyAndSetsFormContentType`
- `CryptoEngineTest.symmetricKeyUtf8LengthMatchesNormalizationRules`
- `PdfEngineTest.inspectRejectsEncryptedPdfWithStableCode`
- `HostProfileMenuRevisionTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 联网大走查与 multipart 文件上传、PDF 表单/书签/签名、托盘取色/截图权限对话框手工验收、加解密非对称 Tab 大切片。
