# MooTool 多产品更新与安装包选择

> 仓库级版本号、Git tag、GitHub Release、`Latest` 和 CI 隔离规则以 [`RELEASE_CONVENTIONS.md`](../../RELEASE_CONVENTIONS.md) 为准；本文只说明客户端更新清单和安装包选择的实现细节。

## 1. 产品边界

MooTool 的各实现使用固定产品 ID，版本号、发布节奏、发布页和安装包互不影响：

| 产品 ID | 产品 | 当前状态 | 更新入口 |
| --- | --- | --- | --- |
| `java` | MooTool Java | 旧版独立维护 | `version_summary.json` + `download_links.json` |
| `next-electron` | MooTool Next Electron | 活跃，当前客户端 | 根目录 `update-manifest.json` |
| `next-tauri` | MooTool Next Tauri | 规划中 | 发布时启用自己的清单节点 |
| `next-macos-native` | MooTool Next macOS Native | 规划中 | 发布时启用自己的清单节点 |

Electron 客户端在代码中固定使用 `next-electron`，不提供运行时切换产品的设置。这样即使 Java 或其他 Next 实现的版本号更高，也不会触发 Electron 更新。

Electron 安装包还使用独立应用 ID `com.rememberber.mootool.next.electron` 和产品名 `MooTool Next Electron`，可以与其他实现并存安装。

## 2. 清单结构

根目录 `update-manifest.json` 是产品注册表。每个活跃产品维护自己的 `releases` 数组；每个发布版本包含独立发布页、说明和资产列表。

```json
{
  "schemaVersion": 1,
  "products": {
    "next-electron": {
      "displayName": "MooTool Next Electron",
      "status": "active",
      "releases": [
        {
          "version": "1.0.0",
          "title": "MooTool Next Electron 1.0.0",
          "notes": "Release notes",
          "prerelease": false,
          "releaseUrl": "https://github.com/.../next-electron-v1.0.0",
          "assets": []
        }
      ]
    }
  }
}
```

`planned` 和 `legacy` 产品不会被 Next Electron 解析。Java 的原更新文件保持不变，确保旧客户端继续工作。

首次 Release 发布前，`next-electron.releases` 保持空数组，不能预填尚不可下载的资产 URL。发布工作流在安装包和更新元数据全部上传成功后才写入 `1.0.0` 节点。

## 3. 自动选择规则

客户端使用 Electron 主进程提供的 `process.platform` 和 `process.arch` 选择资产：

1. 只读取与编译期产品 ID 完全一致的产品节点。
2. 在该产品内按语义版本选择最高版本；稳定版客户端忽略 `prerelease`，预发布客户端可接收后续预发布版和正式版。
3. 只匹配当前系统和架构；`aarch64` 归一为 `arm64`，`amd64` 归一为 `x64`。
4. 精确架构优先于 `universal`，较小的 `priority` 数值优先。
5. 自动更新优先格式为 macOS `ZIP > DMG > PKG`（MacUpdater 使用 ZIP）、Windows `NSIS > MSI > Portable > ZIP`；Linux 优先保持当前安装包型（例如 DEB 到 DEB、AppImage 到 AppImage），无法识别时再按 `AppImage > DEB > RPM > tar.gz`。面向用户的 macOS 首次安装仍推荐 DMG。
6. 没有匹配资产时不下载其他系统或架构的文件，只提供当前产品发布页。
7. 更新内容只汇总“当前版本之后、目标版本及之前”的同通道版本说明，并优先保留较新的说明。

更新 URL 必须使用 HTTPS。检查结果缓存在主进程，Renderer 只能请求检查、下载、安装或打开已缓存的发布页，不能传入任意 URL。

## 4. Electron 产物命名

当前构建使用以下稳定命名：

- `MooTool-Next-Electron-{version}-mac-arm64.dmg`
- `MooTool-Next-Electron-{version}-mac-x64.dmg`
- `MooTool-Next-Electron-{version}-win-x64-setup.exe`
- `MooTool-Next-Electron-{version}-win-x64-portable.exe`
- `MooTool-Next-Electron-{version}-linux-x64.AppImage`
- `MooTool-Next-Electron-{version}-linux-x64.deb`

ZIP 等备用格式沿用同一前缀。Windows 安装版和便携版必须保留 `setup`/`portable` 后缀，避免两个 `.exe` 相互覆盖。

四个目标还会上传独立的 Electron Updater 元数据：

- macOS Apple Silicon：`arm64-mac.yml`
- macOS Intel：`x64-mac.yml`
- Windows x64：`x64.yml`
- Linux x64：`x64-linux.yml`

客户端按自身架构选择 channel，避免两个 macOS 构建产生的同名 `latest-mac.yml` 在 GitHub Release 中互相覆盖。

## 5. 发布流程

1. 仅修改目标产品目录和版本，禁止借用其他产品的版本号。
2. Electron 标签使用 `next-electron-v{version}`；Tauri 和原生 macOS 后续使用各自产品 ID 作为标签前缀。
3. 在 `next/release-notes/{version}.md` 编写本版本说明；文件首行必须是 `# MooTool Next Electron {version}`。
4. 推送 tag 后，`.github/workflows/next-build-installers.yml` 校验 tag、`next/package.json` 和版本说明完全一致，再生成四个目标的独立 CI 工件。
5. 工作流将安装包、blockmap 和架构专用更新元数据上传到对应 GitHub Release；稳定版设为全局 `Latest`，预发布版不更新 `Latest`。
6. Release 成功后，工作流只更新 `update-manifest.json` 的 `next-electron` 节点并提交到 `master`；其他产品节点保持不变。
7. 清单更新后，旧版本客户端会自动选中与自身通道、系统和架构匹配的文件，并汇总所有跨过版本的更新内容。
8. 默认在后台静默下载，用户关闭该设置后则等待手动下载；下载完成后显示“安装并重启”，由 `electron-updater` 校验并安装。

## 6. 正式发布签名

通过 `next-electron-v*` tag 发布时，macOS 安装包必须完成 Developer ID 签名和 Apple 公证，否则工作流会在创建 Release 之前失败。请在 GitHub 仓库的 Actions secrets 中配置：

- `CSC_LINK`：Developer ID Application 证书的 HTTPS 地址或 Base64 内容。
- `CSC_KEY_PASSWORD`：证书密码。
- `APPLE_ID`：用于公证的 Apple ID。
- `APPLE_APP_SPECIFIC_PASSWORD`：Apple ID 的 app-specific password。
- `APPLE_TEAM_ID`：Apple Developer Team ID。

Windows 代码签名当前不会阻断发布，但正式分发强烈建议同时配置 `WIN_CSC_LINK` 和 `WIN_CSC_KEY_PASSWORD`；未配置时工作流会给出警告。手动触发单平台构建允许在没有证书时生成测试工件，但这些未签名工件不得用于正式自动更新。

两个 macOS 构建在打包后都会再执行严格的 `codesign` 校验。只有 Release 成功、全部资产可访问后，工作流才会更新 `update-manifest.json`，因此签名或公证失败的版本不会暴露给已发布客户端。

未来客户端接入时应复制协议实现并把编译期产品 ID 固定为自己的 ID，不能让用户手动切换到其他实现的更新通道。
