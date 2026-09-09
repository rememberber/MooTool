# 本轮验收记录

- 阶段/条目：F05 代码运行（P6）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `RuntimeTool.tsx`、`runtimeExecutionService.ts`；本产品 `docs/feature-parity.md` F05、`docs/ui-spec.md` L4
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **116/116**（含 CodeRunEngineTest 4；此前 F20 为 112/112）
- 本机运行时：Java 21.0.12.1（`java -version` 可用）；Python 3.9.6；Node v24.15.0；Groovy 未安装（检测为不可用，不假成功）
- 语义：三主 Tab（Java/Groovy、Python、Node.js）+ 首 Tab 内 Java/Groovy；独立草稿/参数/工作目录；检测版本与设置路径；真运行/停止、stdout/stderr 流、退出码/耗时/命令摘要；历史与分离窗口；Cmd/Ctrl+Enter；关闭应用 `cancelAll`
- 差异：DIFF-020（ProcessBuilder argv、ProcessHandle 杀树、Node 非 Prettier、自带 JRE ≠ 用户 javac）
- 未测：窗口截图、安装镜像、Windows 杀树、关闭应用时无限循环收尾的手工 UI、可拖动分隔条自动滚尾
- 下一轮：P6 剩余随手记（F01）、JSON/随手记 Git、备份/恢复
