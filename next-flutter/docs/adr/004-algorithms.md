# ADR 004：复杂算法

- 状态：部分采纳
- 日期：2026-09-08

## JSON

Dart 重写 Electron `jsonTools.ts`：格式化/压缩、重复 Key 扫描（解析前）、排序、JSON↔XML、JSON↔JavaBean、字符串转义、JSONPath。JSONPath 显式解析点、下标、切片、联合、递归和 `?(@.field op literal)`，不执行嵌入 JS。大整数保真度与 JS/Dart `jsonDecode` 相同，超过 2^53 时与 Electron 一样可能丢失，记为已知差异。

## Protobuf

P0 样例 `DynamicProto` 运行时解析用户粘贴的 proto3 message，支持 string/int/bool/repeated string 的 JSON↔binary。不是 `package:protobuf` 生成代码路径。map/oneof/64-bit zigzag/嵌套 message 尚未实现。

## PDF

未使用 `package:pdf` 冒充拆合。`SimplePdf` 读写本产品生成的未压缩 PDF 1.4 页面对象，可按页提取和合并并保留可提取文本。加密、ObjStm、任意外部 PDF 会拒绝。完整工具需要随包 pdfium/qpdf helper，见 P6。
