# 本轮验收记录

- 阶段/条目：F18 时间转换（P3 的第一个完整本地算法工具，不是完整 P3）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `next/src/features/time/timeTools.ts`、`TimeConvertTool.tsx`
- OS/arch/JDK/Compose：macOS Darwin 25.6.0，Intel x86_64，Zulu JDK 21，Compose 1.12.0
- 已执行命令及退出结果：
  - `./gradlew :composeApp:desktopTest` 成功，20 个测试 0 失败（含 TimeEngine 7）
- 手工步骤与真实结果：未做窗口截图（无权限）；未重跑 `createDistributable`
- 已通过条目：epoch/负值/毫秒、显式单位、DST gap/overlap、闰年拒绝、同一瞬间跨时区；注册表将 `timeConvert` 标为 Available
- 未测：大屏时钟视觉、分离窗口手工、重启 UI、Windows/Linux
- 已记录 DIFF：DIFF-001 显式单位优先，不按 13 位改写毫秒
- 下一轮具体任务：P3 下一个最小完整工具（编码或计算器）；F04 Git/监视仍待做
