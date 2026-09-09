# ADR 003：平台矩阵

- 状态：macOS 本机构建已验证（未签名 DMG）；Windows/Linux 未在本机运行
- 日期：2026-09-09

## 本机

- macOS 26.7，x86_64
- Xcode 26.5（`/Applications/Xcode.app`）。系统 `xcode-select` 仍可能指向 Command Line Tools；打包脚本在未设置 `DEVELOPER_DIR` 时自动改用 Xcode.app，不修改系统默认路径。
- Flutter SDK：`/Users/zhoubo/sdk/flutter` 3.47.2
- Intel Mac 构建有 Flutter 上游淘汰警告，不作为发行阻塞。

## 目标

| 平台 | 最低候选 | 状态 |
| --- | --- | --- |
| macOS | 12+（随 Flutter 3.47） | 本机 `flutter build macos --release` 已产出 `.app` 与未签名 DMG；临时 `--data-dir` 可启动 |
| Windows 10 22H2+/11 | 未在本机运行 | 未验证 |
| Ubuntu 22.04/24.04 | 未在本机运行 | 未验证 |
| Wayland 截图/托盘 | 未做 | 未验证 |

P6 桌面通道：`com.rememberber.mootool.next.flutter/desktop`。macOS `MainFlutterWindow.swift` 提供：IOPM 防休眠、NSPasteboard 剪贴板、NSStatusItem 托盘、关闭策略、`CGDisplayCreateImage` 截图、`NSColorSampler` 取色。截图前会暂时隐藏主窗口。ScreenCaptureKit 未用。Release entitlements 关闭 App Sandbox 并允许出站网络。安装包不使用 Developer ID；本机产物为 adhoc，`spctl` 拒绝。
