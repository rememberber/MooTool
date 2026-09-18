# DIFF-666：F14 对称/非对称/摘要/Base 密文复制 `cipherCopyActionEnabled`

基线：DIFF-665。

## 范围

- **F14**：`CryptoWiringPresentation.cipherCopyActionEnabled`（密文/摘要结果非空才可复制）+ 对称/非对称/Base `MooButton` 与摘要 `MooGhostButton` 接线；随机 Tab 复制复用同一守卫。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
