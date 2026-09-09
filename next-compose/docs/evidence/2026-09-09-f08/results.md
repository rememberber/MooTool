# 本轮验收记录

- 阶段/条目：F08 环境变量（P5）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `VariablesTool.tsx`、`systemService` 环境读写
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **100/100**（含 EnvEngineTest 3；此前 F11 为 97/97）
- 语义：用户/系统/进程作用域、JVM 运行时 Tab、搜索/刷新/复制/导出、改前 diff、备份后写入、无权限保持原文件、不改写 `System.getenv`、无通用历史、分离窗口
- 差异：DIFF-016（本产品 `data/environment` 与 Compose shell 标记；Runtime 为 JVM 而非 Electron）
- 未测：窗口截图、系统提权对话框、Windows 注册表、新终端实际加载钩子、安装镜像
- 下一轮：P5 其余（F09 HTTP、F10 Host、F20 翻译）
