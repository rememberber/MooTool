# DIFF-008：对称密钥字节长度与 Java Hutool 路径

- 编号：DIFF-008
- 影响：F14 加解密/随机
- 日期：2026-09-09

## 原行为（Electron）

对称 AES/DES 使用 CryptoJS ECB + PKCS#7，密钥按 **Unicode 码点** 截断/右侧补字符 `0` 后再 UTF-8 编码；SM4 用 `sm-crypto` 同样的 16 码点密钥 hex。密文为小写 Hex。RSA/SM2 密钥与密文为 DER/点坐标的标准 Base64。SM2 密文为 C1C3C2，C1 **不含** `04` 前缀。

Java 版 Hutool `SecureUtil.generateKey` 会把任意口令派生成算法密钥，与 Electron 字节不一致。

## 本产品行为

对齐 Electron：ECB + PKCS#5/7、Hex 密文、码点截断/补零后 UTF-8。AES/SM4 要求恰好 16 字节、DES 恰好 8 字节；码点长度合格但 UTF-8 变长（例如 16 个汉字）时 **拒绝**，并在界面说明，不静默截断字节。SM2 加解密自动兼容有/无 `04` 的 C1。RSA 默认生成 2048 位 PKCS#1 DER Base64；解析同时接受 PKCS#1 与 X.509/PKCS#8。摘要/Base64/Base32 与 Electron 样本字节一致。

## 理由

规格以 Electron 为兼容基线，且要求非 ASCII 密钥有明确成功/拒绝，而不是沿用 Java Hutool 的口令派生。

## 证据

`CryptoEngineTest`：Electron AES/DES/SM4/MD5/SHA-256/SM3/Base64/Base32 固定样本；Electron RSA-512 与 SM2 密文/签名可解密、可验签；自生成 RSA/SM2 往返；汉字密钥 `invalid-key`。

## 受影响范围

- 不能用 Java 版 Hutool 密文直接当 Compose 对称输入（除非密钥碰巧是 ASCII 且未走 generateKey）。
- 文件摘要超过 256 MiB 拒绝；哈希过程中无单独取消按钮（可用文件对话框取消选择）。
- RSA 密钥生成在后台线程，2048 位可能需数秒。
