# DIFF-127：编码/配置/加解密运算 toast

对照 Electron 工具在运算成功/失败时的 toast 反馈。

## 范围

- **F13 编码解码**：`convert` 正/反向成功与失败 toast。
- **F06 配置转换**：Properties↔YAML、YAML 校验通过、YAML 格式化成功/失败 toast。
- **F14 加解密**：`runCrypto`、签名校验、文件摘要、随机生成成功/失败 toast。

## 文件

- `EncodeScreen.kt`、`ConfigConvertScreen.kt`、`CryptoScreen.kt`
