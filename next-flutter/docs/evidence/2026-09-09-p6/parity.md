# 对照

基线：Electron `MessageBoardTool.tsx` / `pageRanges.ts` / `pdfService.ts` / `imageTools.ts` / `imageVectorizationService.ts`。

已对齐：留言板 80 字与预设主题；页码 `1-3;2;7;9-10` 与奇偶/自定义规则；图片缩放/命名/Base64/水印锚点 fixtures。

已知差异：

- PDF 只处理本产品 SimplePdf，不是 pdf-lib 任意文档。
- SVG 为轮廓矢量化，不是 vtracer。
- 截图、剪贴板图片、防休眠、屏幕取色、托盘未做。
- 本机缺完整 Xcode，未跑 `flutter run -d macos`。
