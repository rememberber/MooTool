# ADR 006：更新与安装包

- 状态：采纳（客户端与打包脚本已落地；本机未产出安装包，未改仓库 `update-manifest.json`，未创建 GitHub Release）
- 日期：2026-09-09

## 决定

- 产品 ID 编译期固定为 `next-flutter`。更新只读根清单 `products.next-flutter`。节点缺失、非 `active` 或 `releases` 为空时显示未发布，不回退 Java / Electron / Tauri / Native。
- Feed：`https://raw.githubusercontent.com/rememberber/MooTool/master/update-manifest.json`，必须 HTTPS。
- Tag：`next-flutter-v{semver}`。GitHub Release `make_latest: false`。
- 安装包名：`MooTool-Next-Flutter-{version}-mac-{arch}.dmg`、`...-win-x64-setup.exe`、`...-win-x64-portable.zip`、`...-linux-x64.AppImage`、`...-linux-x64.deb`。
- 下载到本产品 `cache/updates/`，校验长度和 SHA-512（Base64）后才标就绪。取消、失败、hash 不符不进入就绪。
- **不签名**（用户 2026-09-09 明确要求）。客户端安装模式固定为手动：打开 DMG/安装包，不自动替换、不提示自动安装成功。
- 首次发布前不写入 `update-manifest.json` 的 `next-flutter` 节点，避免客户端发现无法下载的版本。

## 未验证

- 本机无完整 Xcode，`scripts/build-macos.sh` 会诚实失败。
- Windows / Linux 安装包未在本机构建。
- 未跑 tag 发布流水线，未实机安装/卸载。
