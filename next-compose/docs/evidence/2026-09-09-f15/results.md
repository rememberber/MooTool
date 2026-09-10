# 本轮验收记录

- 阶段/条目：F15 正则（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `regexTools.ts`、`RegexTool.tsx`；引擎 Java Pattern，不宣称 JS 兼容（DIFF-003）
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**39/39** 通过（RegexEngine 5、RegexWorkerClient 1、收藏持久化 1）
- 语义：`global` 为 `Matcher.find` 遍历；零宽匹配前进 1；空模式返回 0 条；非法模式报错并保留原文；命名组 `(?<name>…)`；21 条常用模式从 Electron 复制，`htmlId` lookbehind 可用
- 隔离：匹配走独立 JVM worker（`-Xmx64m`），默认 2s / 10000 条；超时 `destroyForcibly`。Java 21 对 `(a+)+$` 已能很快拒绝，超时 fixture 使用仍会长时间回溯的 `(.*a){28}`
- 收藏：`dataRoot/favorites/regex.json` 原子写，增删后新 Store 实例可读
- 未测：窗口截图、分离窗口手工、重启 UI、worker 在安装镜像 classpath 上的启动、取消按钮竞态的手工核对
- 下一轮：F16 Cron 或其他 P3 工具；F04 Git 仍待做
