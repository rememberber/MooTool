# 本轮验收记录

- 阶段/条目：F14 加解密与随机（P4 媒体/加密首个工具）
- 本产品工作树：仅 `next-compose/`
- 依赖：JCA + `org.bouncycastle:bcprov-jdk18on` **1.80**（SM2/SM3/SM4）
- 参考：Electron `cryptoTools.ts`、`CryptoTool.tsx`；架构 JCA/BC
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**72/72** 通过（CryptoEngine 5；此前 F07 为 67/67）
- 语义：AES/DES/SM4 ECB PKCS7 Hex；RSA PKCS#1 加解密/私钥操作/SHA-256 签名；SM2 C1C3C2 与 DER 签名消费 Electron 样本；MD5/SHA/SM3 文本与文件摘要；Base64/Base32；UUID/数字/字符串/密码；历史与分离窗口
- 差异：DIFF-008（非 ASCII 密钥拒绝；不兼容 Java Hutool generateKey）
- 未测：窗口截图、256 MiB 以上文件、哈希中途取消、安装镜像、与 Java Hutool 密文互操作（刻意不兼容）
- 下一轮：P4 其余（F17 二维码、F22 调色板、F19 留言板、F23 图片、F24 PDF）
