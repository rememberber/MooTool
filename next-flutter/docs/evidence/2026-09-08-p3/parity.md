# 对照

基线：Electron `next/` 1.1.4 的 `*Tools.ts` 与对应 `*.test.ts`。

已对齐的精确/结构样本：encode、calculator、time、regex 分组、cron 下次运行、UA 预设、config YAML、AES 密文、color 主题 SHA-256、diff unified、protobuf Person JSON↔Hex。

已知差异：

- QR 识别、非对称密码、SM4 未实现，页面不提供假成功。
- Java/HTML/XML 格式化不是 Prettier。
- Cron 自然语言不是 cronstrue；`L`/`#` 未实现。
- 文本对比 UI 尚未做同步滚动与字符级高亮，引擎结果已对齐。
