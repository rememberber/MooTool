# ADR 004：复杂算法

- 状态：部分采纳
- 日期：2026-09-08

## JSON

Dart 重写 Electron `jsonTools.ts`：格式化/压缩、重复 Key 扫描（解析前）、排序、JSON↔XML、JSON↔JavaBean、字符串转义、JSONPath。JSONPath 显式解析点、下标、切片、联合、递归和 `?(@.field op literal)`，不执行嵌入 JS。大整数保真度与 JS/Dart `jsonDecode` 相同，超过 2^53 时与 Electron 一样可能丢失，记为已知差异。

## P3 本地工具

- **编码**：Unicode / URL UTF-8+GB2312（`gbk_bytes`）/ UTF-8 Hex / ASCII code point，与 `encodeTools.ts` fixtures 对齐。
- **时间**：IANA 时区（`timezone`），非法日期按格式化往返拒绝，不静默进位。
- **计算器**：自写算术解析器，禁止 eval；进制/GCD/LCM/排列组合。
- **正则**：Dart `RegExp`；零宽全局匹配手动前进。
- **Cron**：6/7 字段 Quartz 风格，`?`、范围、步长、MON-FRI；自然语言为自写摘要而非 cronstrue。`L`/`#` 尚未实现。
- **UA**：规则表解析预设 Chrome/Firefox/Safari/Android/curl 与 bot，不是只判断是否包含 Chrome。
- **配置**：Properties ↔ YAML，校验/格式化。
- **加解密**：AES/DES 为 ECB+PKCS#7+Hex，key 字符截断/补零后 UTF-8；AES 固定密文 `504a3eb1fee7af3af9561f37a6f12fa8` 与 Electron CryptoJS 一致。摘要含 MD5/SHA/SM3。Base32 对齐 hi-base32。SM4/RSA/SM2 本轮未实现，页面明确说明。
- **调色板**：主题色表 SHA-256 与 Electron 冻结值一致；运算与 `colorTools.ts` 对齐。屏幕取色未做。
- **文本对比**：行级 LCS + 字符级补丁 + 三行上下文 unified，对齐 `diffTools.ts` fixtures。
- **二维码**：`qr` 生成真实 PNG；文件/剪贴板识别尚未接入解码器。
- **格式化**：Nginx 对齐 `formatNginx`；XML/HTML 为标签感知缩进，不是 Prettier；Java 为字符串感知花括号整理，不是 prettier-plugin-java。
- **Protobuf**：运行时解析粘贴的 proto3，JSON↔Hex/Base64 与 wire dump。map/oneof/嵌套 message 未做。

## P4 随手记

- **快速替换**：24 项与 Electron `quickReplace.ts` 同 ID；`escape`/`unescape` 用 `jsonEncode`/`jsonDecode` 切片，与 `JSON.stringify(input).slice(1,-1)` 对齐。有选区只改选区，否则全文，一次 `apply` 一次 undo。
- **frontmatter**：磁盘文件使用 Java 兼容键 `title`/`syntax`/`font_name`/`font_size`/`line_spacing`/`line_wrap`；编辑器内存只保留正文。
- **列编辑**：`EditorDocument` 矩形插入/删除；列坐标按 Dart `String` UTF-16 code unit，与 `TextField` 一致。Emoji 可能占 2 个单位，测试已说明。
- **附件**：写入 `vaults/quick-note/attachments/<noteId>/`，拒绝 `..` 与绝对路径。预览剥 `<script>`/`<iframe>`，不执行脚本。
- **明确未做**：剪贴板图片（无平台通道）、文档树拖放、Git watcher UI、5MiB 冲突提示。

## PDF

未使用 `package:pdf` 冒充拆合。`SimplePdf` 读写本产品生成的未压缩 PDF 1.4 页面对象，可按页提取和合并并保留可提取文本。加密、ObjStm、任意外部 PDF 会拒绝。完整工具需要随包 pdfium/qpdf helper，见 P6。
