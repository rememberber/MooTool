# 本轮验收记录

- 阶段/条目：F16 Cron（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 依赖：`com.cronutils:cron-utils:9.2.1`（Quartz）
- 参考：Electron `cronTools.ts`、`CronTool.tsx`
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**44/44** 通过（CronEngine 4、Cron 收藏 1）
- 语义：只接受 6/7 字段；空年输出 6 字段；年过滤在选定时区；默认/工作日/年字段/闰日与 Electron 样本对齐。自然语言来自 CronDescriptor，解析失败不编造
- 差异：DIFF-004（保留 Quartz `?`，不改成 Unix `*`）
- 未测：窗口截图、DST 间隙手工核对、`L`/`#` 全表、分离窗口手工、重启 UI
- 下一轮：F02 Diff、F03 格式化、F06 配置转换或 F07 Protobuf；F04 Git 仍待做
