# DIFF-012：PDF 拆分合并使用 PDFBox importPage

- 编号：DIFF-012
- 影响：F24 PDF
- 日期：2026-09-09

## 原行为（Electron）

`pdf-lib` 读取字节后 `copyPages`。拆分输出为源目录 `{小写主名}_split.pdf`。合并弹出保存对话框，默认桌面 `merge.pdf`。加密文件未支持。

## 本产品行为

Apache PDFBox **3.0.4** `Loader.loadPDF` + `PDDocument.importPage` 复制页面对象，不把页面光栅化后再造 PDF。页码解析（逗号/中文逗号转分号、按 token 顺序去重、custom 与候选交集）对齐 `pageRanges.ts`。加密 PDF 以 `InvalidPasswordException` 明确失败。取消时删除本批已写出的半成品。

## 理由

架构指定 PDFBox，避免引入 iText。`importPage` 保留页面内容流，满足“真 PDF 页面操作”。

## 证据

`PdfEngineTest`：`1-3;2;7;9-10` 去重保序、奇偶/自定义规则、4 页+3 页按范围合并后文本可提取、`Quarterly.PDF` 奇数页拆到 `quarterly_split.pdf`、非 PDF/不足两个文件/取消无输出文件。

## 受影响范围

- 表单/书签/签名/注释的保留范围未做专项实测，不能宣称与 pdf-lib 像素级一致。
- 拆分默认覆盖已存在的 `*_split.pdf`，与 Electron 相同。
- 未跑安装镜像内的 PDFBox native 字体替代路径。
