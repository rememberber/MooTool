# DIFF-667：F14 非对称/Base 操作 `*ActionEnabled`

基线：DIFF-666。

## 范围

- **F14 非对称**：`publicEncryptActionEnabled` / `privateDecryptActionEnabled` / RSA 私加/公解 / `signActionEnabled` / `verifyActionEnabled`（密钥就绪 + 验签需明文/签名非空 + `asymBusy`）+ `CryptoScreen` 接线。
- **F14 Base**：`encodeBaseActionEnabled` / `decodeBaseActionEnabled` 接线。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
