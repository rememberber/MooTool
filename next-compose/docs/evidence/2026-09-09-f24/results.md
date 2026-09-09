# 本轮验收记录

- 阶段/条目：F24 PDF（P4）
- 本产品工作树：仅 `next-compose/`
- 依赖：Apache PDFBox **3.0.4**
- 参考：Electron `pdfService.ts`、`pageRanges.ts`、`PdfTool.tsx`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **87/87**
- 语义：拆分/合并 Tab、最多 20 项、奇偶/自定义页码、源顺序去重、`_split.pdf` 后缀、合并保存对话框、历史与分离窗口、取消删除半成品
- 差异：DIFF-012（PDFBox importPage vs pdf-lib）
- 未测：窗口截图、加密样本、表单/书签/签名、安装镜像
- 下一轮：P4 剩余 F23 图片助手
