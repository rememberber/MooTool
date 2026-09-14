# 本轮验收记录

- 阶段/条目：F01 随手记列编辑切片（P6 / EditorHost）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `TextCodeEditor.tsx` rectangularSelection；架构 §5.3
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **136/136**（含 ColumnEditEngineTest 7、EditorBufferColumnEditTest 1；此前 F01 预览为 128/128）
- 语义：逻辑行列选择；插入补齐短行；矩形粘贴/删除；Alt+拖动或锁定列编辑；一次 CompoundEdit 撤销；不拆 emoji/汉字
- 差异：DIFF-024
- 未测：窗口截图、真实 Alt 拖动手势、IME 预编辑与列模式同时、5 MiB、分离/收回后列选区
- 下一轮：JSON/随手记 Git，或 F04 冲突监视/检查器，或更新通道
