# 本轮验收记录

- 阶段/条目：F02 文本对比（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 依赖：`io.github.java-diff-utils:java-diff-utils:4.15`
- 参考：Electron `diffTools.ts`、`TextDiffTool.tsx`（对齐 Java DiffService 合同）
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**50/50** 通过（DiffEngine 6）
- 语义：行级 Myers + 配对行内字符差异；三行上下文 unified；忽略空白不隐藏 unified 补丁；保留末尾空行；CRLF 作为单一换行；异步比较按 revision 应用
- 额外：正则 worker 改为 Java 参数文件传 classpath，避免依赖增多后命令行过长
- 未测：窗口截图、同步滚动手工抖动、大文件、分离窗口手工、重启 UI
- 下一轮：F03 格式化、F06 配置转换或 F07 Protobuf；F04 Git 仍待做
