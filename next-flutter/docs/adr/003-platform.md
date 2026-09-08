# ADR 003：平台矩阵

- 状态：未验证（记录本机事实）
- 日期：2026-09-08

## 本机

- macOS 26.7，x86_64
- Xcode：仅 Command Line Tools（`/Library/Developer/CommandLineTools`），无完整 Xcode.app
- Flutter SDK：`/Users/zhoubo/sdk/flutter`（git clone stable）

## 目标

| 平台 | 最低候选 | P0–P2 状态 |
| --- | --- | --- |
| macOS | 13+ / 实际随 Flutter 3.47 为 12+ | 单元/widget 测试可跑；桌面 `flutter run -d macos` 可能因缺少 Xcode 失败 |
| Windows 10 22H2+/11 | 未在本机运行 | 未验证 |
| Ubuntu 22.04/24.04 | 未在本机运行 | 未验证 |
| Wayland 截图/托盘 | 未做 | 未验证 |

Intel Mac 主机构建已收到 Flutter 上游淘汰警告，不作为发行阻塞，但要写进证据。

P6 桌面通道：`com.rememberber.mootool.next.flutter/desktop`。macOS `MainFlutterWindow.swift` 用 `IOPMAssertionCreateWithName` 请求禁止显示器休眠；截图/取色返回 `UNIMPLEMENTED`。本机无完整 Xcode，该 Swift 代码未在本机编译运行。
