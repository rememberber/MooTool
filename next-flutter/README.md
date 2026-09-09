# MooTool Next Flutter

独立桌面产品线，使用 Flutter 实现。功能与布局以 `next/` Electron 1.1.4 为参照，产品身份、数据、安装和更新与 Java / Electron / Tauri / macOS Native 隔离。

当前版本 **0.1.0**：P0–P6 工具与桌面通道已在代码侧落地；P7 更新只读 `next-flutter`，安装包脚本未签名。其余缺口见 [差异审查](docs/p7-gap-review.md)。页面不会提供假成功操作。

开发文档：

| 文档 | 用途 |
| --- | --- |
| [Cursor 开发主指南](docs/cursor-development-guide.md) | 产品边界、阶段、架构 |
| [逐工具功能规格](docs/feature-parity.md) | 26 个入口 |
| [验收](docs/acceptance.md) | 状态与检查 |
| [ADR](docs/adr/) | 编辑器、窗口、算法、数据 |

## 环境

- Flutter 3.47.2 / Dart 3.13（本机记录于 `docs/evidence/2026-09-08-p0-p2/`）
- 目标：macOS、Windows、Linux 桌面。移动端和 Web 不在本轮。
- 本机若只有 Command Line Tools、没有完整 Xcode，可以跑 `flutter test`，但 `flutter run -d macos` 可能无法链接插件。

## 运行

```bash
export PATH="$HOME/sdk/flutter/bin:$PATH"   # 或你的 Flutter SDK
cd next-flutter
flutter pub get
flutter test
flutter run -d macos -- --data-dir /tmp/mootool-next-flutter-dev
```

不要使用真实用户数据目录做测试。默认数据路径：

- macOS：`~/Library/Application Support/com.rememberber.mootool.next.flutter/`
- Windows：`%APPDATA%/MooToolNextFlutter/`
- Linux：`$XDG_DATA_HOME/mootool-next-flutter/`

## 产品边界

| 项目 | 值 |
| --- | --- |
| Product ID | `next-flutter` |
| 显示名 | `MooTool Next Flutter` |
| Dart package | `mootool_next_flutter` |
| 应用 ID | `com.rememberber.mootool.next.flutter` |
| 版本 | `pubspec.yaml` → `0.1.0+1` |
| Tag | `next-flutter-v0.1.0` |

## 打包（未签名）

```bash
python3 scripts/check-release.py --root .
./scripts/package.sh macos     # 需要完整 Xcode；本机 Command Line Tools 会失败
./scripts/package.sh windows   # 在 Windows 上
./scripts/package.sh linux     # 在 Linux 上
```

产物命名见 [docs/release.md](docs/release.md)。更新检查只读 `next-flutter`，下载校验后手动打开安装包，不会提示自动安装成功。

