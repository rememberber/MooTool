# 本轮验收记录

- 阶段/条目：F01 随手记 Markdown 预览与附件切片（P6）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `QuickNoteTool.tsx`、`quickNoteAttachments.ts`、`quickNoteVaultRepository.ts`；本产品 `docs/architecture.md` §5.4
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **128/128**（含 MarkdownPreviewEngineTest 3、NoteAttachmentEngineTest 3；此前 A03 为 122/122）
- 语义：编辑/分栏/预览三模式，切换不重建编辑缓冲；commonmark AST Compose 预览；脚本/原始 HTML 当文本；外部图片不联网；附件写入 `attachments/` 相对引用；粘贴/插入图片；孤立附件清理拒绝仍被引用的文件
- 差异：DIFF-023。粘贴图片走工具栏按钮，不拦截编辑器 Cmd/Ctrl+V
- 未测：窗口截图、拖入 RSTA、连续剪贴板手工往返、5 MiB 预览性能、安装镜像
- 下一轮：列编辑，或 JSON/随手记 Git、更新通道
