# MooTool Next Flutter 发布说明

产品 ID `next-flutter`。版本来源 `pubspec.yaml`。Git tag `next-flutter-v{semver}`。Release 标题 `MooTool Next Flutter {version}`。`make_latest: false`。

本文件补充客户端与打包细节。仓库根 `RELEASE_CONVENTIONS.md` 仍是多产品总约定；首次打 tag 前应把本产品行补进该表。当前工作树里该文件还有其他产品的未提交改动，本轮不改。

## 安装包

| 平台 | 文件 |
| --- | --- |
| macOS arm64/x64 | `MooTool-Next-Flutter-{version}-mac-{arch}.dmg` |
| Windows x64 安装版 | `MooTool-Next-Flutter-{version}-win-x64-setup.exe` |
| Windows x64 便携版 | `MooTool-Next-Flutter-{version}-win-x64-portable.zip` |
| Linux x64 | `MooTool-Next-Flutter-{version}-linux-x64.AppImage`、同前缀 `.deb` |

全部未签名。macOS Gatekeeper 需在 Finder 中右键打开。Windows 可能出现 SmartScreen。

构建：

```bash
cd next-flutter
./scripts/package.sh macos    # 需要完整 Xcode
./scripts/package.sh windows  # 在 Windows 上
./scripts/package.sh linux    # 在 Linux 上
python3 scripts/check-release.py --root . --tag next-flutter-v0.1.0
```

CI：`.github/workflows/next-flutter-build.yml`。路径过滤 `next-flutter/**`，tag `next-flutter-v*`。不构建其他产品。不更新 `update-manifest.json`。

## 更新

客户端只读 `products.next-flutter`。下载校验后打开安装包。自动下载仍是手动安装。没有匹配资产时只打开发布页。

卸载：Windows 卸载项名称独立；删除用户数据需用户自己清理 `Application Support` / `%APPDATA%\MooToolNextFlutter` / `~/.local/share/mootool-next-flutter`。脚本不得遍历删除 `MooTool*`。
