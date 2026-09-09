# 本轮验收记录

- 阶段/条目：F19 留言板（P4）
- 本产品工作树：仅 `next-compose/`
- 依赖：本机 `caffeinate` / `systemd-inhibit` / Windows PowerShell；无新 Maven 库
- 参考：Electron `MessageBoardTool.tsx`、`displaySleepService.ts`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest`，**84/84** 通过（MessageBoardEngine 3、DisplayWakeLock 1；此前 F22 为 80/80）
- 语义：80 字 UTF-16、8 预设、6 主题、左/居中、字号 70–130 自动适配、会话持久化、沉浸展示 Esc 退出、唤醒 token；无通用历史
- 差异：DIFF-011（OS 进程保活 vs Electron powerSaveBlocker）
- 未测：窗口截图、显示器切换、实际熄屏/唤醒、安装镜像
- 下一轮：P4 其余（F23 图片、F24 PDF）
