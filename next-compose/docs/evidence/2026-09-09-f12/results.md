# 本轮验收记录

- 阶段/条目：F12 UA 分析
- 本产品工作树：仅 `next-compose/`
- 依赖：`com.github.ua-parser:uap-java:1.6.1`
- 已执行：`./gradlew :composeApp:desktopTest`，**32/32** 通过（UaEngine 3）
- 名称归一：Chrome Mobile → Chrome，Mobile Safari → Safari，iPhone OS → iOS；engine 由 UA 推断（DIFF-002）
- 未测：窗口截图、剪贴板权限失败的手工核对、与 ua-parser-js 逐字段全量对照
- 下一轮：F15 正则（需进程隔离超时）或其他 P3 工具
