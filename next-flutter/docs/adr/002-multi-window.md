# ADR 002：多窗口

- 状态：部分采纳
- 日期：2026-09-08

## 决定

`SessionCoordinator` 是会话写所有者。分离/收回必须：结束 composition → 锁定 session → 提交 revision 快照 → 目标 ACK 同一 revision → 切换唯一 owner。失败则 abort 并恢复源窗口。

P2 在同一 Flutter engine 内完成分离状态（主区显示占位，会话对象不重建）。分离的 toolId 写入 `workspace.json`，重启后仍显示占位并可收回。HTTP 等任务留在 AppController，分离不重新执行。`desktop_multi_window` 需要各窗口独立 engine，当前开发机只有 Command Line Tools，完整第二 engine 尚未实机验证。

## 未验证

- 真实第二 Flutter engine 的插件注册
- 与 `window_manager` 同时使用时的关闭/焦点
- 100 次分离/收回的内存曲线

任务与 HTTP 运行仍应留在应用级服务；窗口只更换订阅者。
