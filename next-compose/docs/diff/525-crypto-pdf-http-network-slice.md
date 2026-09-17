# DIFF-525：非对称 Tab + PDF 结构探测 + HTTP Cookie 联网切片

## 背景

DIFF-524 已补 cURL urlencode、对称密钥字节 UI、PDF 加密 toast 与托盘 Host 菜单。本切片继续 DIFF-524「未做」中的非对称 Tab、PDF 表单/书签/签名探测与 HTTP 本机联网语义，不重复 524 的 urlencode/对称字节单测。

## 行为

### F14 加解密

- `CryptoEngine.deriveAsymmetricPublicKey`：RSA/SM2 从私钥还原 Base64 公钥（对齐 feature-parity「公钥还原」；RSA 输出 PKCS#1 公钥 DER，与 Electron X509 样本可互操作加解密/验签）。
- `CryptoEngine.asymmetricKeyStatus`：非对称 Tab 实时公钥/私钥就绪、RSA 模长、SM2 密钥字节提示。
- 非对称 Tab：「还原公钥」按钮、密钥状态行（对齐对称 Tab `{current}/{required}` 前置反馈思路）。

### F24 PDF

- `PdfEngine.inspect` / `analyzeStructure`：统计 AcroForm 字段数、文档大纲书签数、`PDSignatureField` 数量。
- 导入含上述对象的 PDF 时 `toastInfo` 展示计数（不假装完整保留策略已验收）。
- 单测：带文本域/签名字段/书签的样本 `inspect` 计数；拆分后输出页数与结构可读（PDFBox `importPage` 对文档级表单的保留范围仍待安装镜像大走查）。

### F09 HTTP

- 本机 `HttpServer`：`Cookie` 请求头往返、`Set-Cookie` 多行写入响应 Cookies 面板；cURL `-b` 与 `toCurl` 往返。

## 验证

- `CryptoEngineTest.derivesPublicKeyFromPrivateForElectronSamplesAndRoundTrip`
- `PdfEngineTest.inspectCountsFormsBookmarksAndSignatureFields`
- `HttpEngineTest.localServerEchoesRequestCookiesAndResponseSetCookie`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗 Git/设置截图、六套 CSS 皮肤、P7 安装、HTTP 公网大走查与 multipart 文件上传、PDF 表单/签名在合并后的完整保留验收、托盘取色/截图权限对话框手工验收、非对称 Tab 与 Electron 逐控件走查。
