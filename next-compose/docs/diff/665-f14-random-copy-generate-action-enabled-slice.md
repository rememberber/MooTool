# DIFF-665：F14 随机 Tab 复制/生成 `*ActionEnabled`

基线：DIFF-664（工作区）。

## 范围

- **F14**：随机 Tab 各行 `randomCopyActionEnabled` / `randomGenerateActionEnabled`（UUID 始终可生成；数字/字符串/密码受 `CryptoEngine` 长度边界约束）+ `CryptoScreen.RandomRow` 接线。
- **控件**：`MooGhostButton` 增加 `enabled`（0.42 透明度、`clickable(enabled=)`，对齐 `MooButton`）。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
