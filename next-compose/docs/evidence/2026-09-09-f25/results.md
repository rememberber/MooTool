# 本轮验收记录

- 阶段/条目：F25 系统信息（P5）
- 本产品工作树：仅 `next-compose/`
- 依赖：OSHI **6.8.2**
- 参考：Electron `HardwareTool.tsx`、`systemService.getSystemInfo`、Java `HardwareInfoUtil`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest` **92/92**（含 HardwareEngineTest 2）
- 语义：系统/CPU/内存/存储/网络 Tab、手动刷新、复制当前 Tab、序列号默认遮蔽、OS 与 JVM 分区、无通用历史、分离窗口、切走取消采集
- 差异：DIFF-014（OSHI vs systeminformation；Active 内存无对应值时为 `-`）
- 未测：窗口截图、安装镜像 JNA、无权限字段逐项
- 下一轮：P5 其余（F08 环境变量、F09 HTTP、F10 Host、F11 网络、F20 翻译）
