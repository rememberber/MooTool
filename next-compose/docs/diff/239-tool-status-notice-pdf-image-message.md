# DIFF-239：PDF / 图片 / 留言板编辑清空状态栏 notice

## 背景

DIFF-238 已覆盖多数文本工具；PDF、图片、留言板仍有状态栏 `notice`（处理完成、历史恢复、复制等），用户改页码/水印/正文时应清空，与 Electron `onChange` 一致。

## 行为

- `PdfSession` / `ImageSession` / `MessageBoardSession` 增加 `onUserInput`（`ToolStatusNotice.kt`）。
- PDF 拆分/合并表中的页码、自定义规则、合并范围输入接入。
- 图片 Base64/压缩水印/SVG/重命名对话框中的文本字段接入。
- 留言板正文输入接入（仍保留清空 `error`）。

## 验证

- `ToolStatusNoticeTest.pdf_onUserInput_clears_notice`
- `./gradlew :composeApp:desktopTest --offline`
