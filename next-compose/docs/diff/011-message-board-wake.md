# DIFF-011：留言板演示唤醒使用操作系统进程

- 编号：DIFF-011
- 影响：F19 留言板
- 日期：2026-09-09

## 原行为（Electron）

主进程 `powerSaveBlocker.start('prevent-display-sleep')`，按单个 blocker id 启停。

## 本产品行为

`DisplayWakeLock` 按持有者 token 引用计数。macOS 启动 `caffeinate -d`；Linux 尝试 `systemd-inhibit`；Windows 用 PowerShell 调用 `SetThreadExecutionState`。退出演示、切走工具、关闭应用时释放。进程拉起失败则明确提示，演示仍可继续，不假装常亮成功。

## 理由

Compose Desktop 没有 Electron `powerSaveBlocker`。子进程与 token 释放路径可单测，且不引入 JNA。

## 证据

`MessageBoardEngineTest`：80 字 UTF-16 截断、预设/主题、字号搜索。`DisplayWakeLockTest`：多 token 只在最后一个 holder 释放时停止会话。本机未测显示器实际是否熄屏。

## 受影响范围

- Linux 无 systemd-inhibit 时常亮不可用。
- Windows 依赖 PowerShell；策略禁止脚本时失败。
- 字号适配用 Compose `TextMeasurer`，与 DOM `scrollHeight` 像素可能有 1px 级差异。
