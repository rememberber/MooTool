# 本轮验收记录

- 阶段/条目：F01 随手记切片（P6）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `QuickNoteTool.tsx`、`quickReplace.ts` / `quickReplace.test.ts`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **120/120**（含 QuickReplaceEngineTest 3、NoteVault 路径隔离；此前 F05 为 116/116）
- 语义：左文档库（搜索/新建笔记与文件夹/重命名/删除/导入导出/打开目录）、中 EditorHost、右 24 项快速替换（选区优先）、保存与切文件时写入、查找替换、字数统计、历史、分离窗口、Cmd/Ctrl+S；设置可改随手记目录
- 差异：DIFF-021。未做 Markdown 预览/分栏、列编辑、附件、Git、frontmatter、全文索引、外部冲突
- 未测：窗口截图、5 MiB 手工编辑、磁盘满、安装镜像
- 下一轮：随手记剩余能力，或 JSON/随手记 Git、备份/恢复
