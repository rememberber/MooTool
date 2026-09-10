# 本轮验收记录

- 阶段/条目：F03 格式化（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 依赖：`com.github.javaparser:javaparser-core:3.26.4`，`org.jsoup:jsoup:1.18.3`
- 参考：Electron `reformatTools.ts`、`ReformatTool.tsx`；Nginx tokenizer 按 Electron 移植
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**56/56** 通过（ReformatEngine 6）
- 语义：文本/文件 Tab；Nginx/Java/XML/HTML；缩进 2–6；格式化在后台线程，按 generation 丢弃过期结果；语法错误保留原文并显示行列；保存走另存对话框，默认不覆盖原文件；写入失败可重试；历史与 Cmd/Ctrl+Shift+F；分离窗口
- 差异：DIFF-005（JavaParser / JAXP / Jsoup，不用 Prettier）
- 未测：窗口截图、大文件、真实保存失败路径的手工对话框、分离窗口手工、重启 UI
- 下一轮：F06 配置转换或 F07 Protobuf；F04 Git 仍待做
