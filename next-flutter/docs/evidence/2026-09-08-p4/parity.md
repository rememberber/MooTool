# 对照

基线：Electron `next/` 1.1.4 `quickNote/`（`quickReplace.ts`、`quickNoteVaultRepository` frontmatter、`prepareMarkdownImageInsertion`）。

已对齐：

- 24 项快速替换 ID 与 Electron fixtures（空行、去重计数、驼峰、科学计数、千分位、引号列表、escape 往返）。
- 磁盘 frontmatter 使用 `font_name` / `font_size` / `line_spacing` / `line_wrap`；编辑器只显示正文。
- 列插入/删除一次 undo；中文与 tab 按 UTF-16 列。
- 附件拒绝 `..` 与绝对路径；孤立附件清理。
- 预览去掉 `<script>` / `<iframe>`。
- 切笔记先保存；切文档清空 undo；重启恢复正文/布局/选区（workspace JSON）。

已知差异 / 未做：

- 剪贴板图片无平台通道，页面说明后不拦截普通文本粘贴。
- 文档树拖放、Git watcher UI、5 MiB 冲突提示未做。
- Markdown 预览为子集渲染（标题/列表/任务/代码/表格/图片），不是完整 CommonMark + 语法高亮。
- 列坐标与 `TextField` 同为 UTF-16，emoji 可能占两列。
- 本机缺完整 Xcode，未跑 `flutter run -d macos` 视觉验收。
