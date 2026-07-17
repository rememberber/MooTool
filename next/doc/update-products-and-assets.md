# MooTool 多产品更新与安装包选择

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
          "releaseUrl": "https://github.com/.../next-electron-v1.0.0",
          "assets": []
        }
      ]
    }
  }
}
```

`planned` 和 `legacy` 产品不会被 Next Electron 解析。Java 的原更新文件保持不变，确保旧客户端继续工作。

## 3. 自动选择规则

客户端使用 Electron 主进程提供的 `process.platform` 和 `process.arch` 选择资产：

1. 只读取与编译期产品 ID 完全一致的产品节点。
2. 在该产品内按语义版本选择最高版本。
3. 只匹配当前系统和架构；`aarch64` 归一为 `arm64`，`amd64` 归一为 `x64`。
4. 精确架构优先于 `universal`，较小的 `priority` 数值优先。
5. 优先格式为 macOS `DMG > PKG > ZIP`、Windows `NSIS > MSI > Portable > ZIP`、Linux `AppImage > DEB > RPM > tar.gz`。
6. 没有匹配资产时不下载其他系统或架构的文件，只提供当前产品发布页。

更新 URL 必须使用 HTTPS。检查结果缓存在主进程，Renderer 只能请求“打开已选中的下载”，不能传入任意 URL。

## 4. Electron 产物命名

当前构建使用以下稳定命名：

- `MooTool-Next-Electron-{version}-mac-arm64.dmg`
- `MooTool-Next-Electron-{version}-mac-x64.dmg`
- `MooTool-Next-Electron-{version}-win-x64-setup.exe`
- `MooTool-Next-Electron-{version}-win-x64-portable.exe`
- `MooTool-Next-Electron-{version}-linux-x64.AppImage`
- `MooTool-Next-Electron-{version}-linux-x64.deb`

ZIP 等备用格式沿用同一前缀。Windows 安装版和便携版必须保留 `setup`/`portable` 后缀，避免两个 `.exe` 相互覆盖。

## 5. 发布流程

1. 仅修改目标产品目录和版本，禁止借用其他产品的版本号。
2. Electron 标签使用 `next-electron-v{version}`；Tauri 和原生 macOS 后续使用各自产品 ID 作为标签前缀。
3. 运行 `.github/workflows/next-build-installers.yml`，生成四个目标的独立 CI 工件。
4. 将工件上传到对应产品标签的 GitHub Release。
5. 在 `update-manifest.json` 的对应产品下新增 release，填写实际资产 URL；不要修改其他产品节点。
6. 确认 `next/package.json` 版本与 release 一致，执行 `npm run check` 和 Electron E2E。
7. 清单合并后，旧版本客户端会自动选中与自身系统和架构匹配的文件。

未来客户端接入时应复制协议实现并把编译期产品 ID 固定为自己的 ID，不能让用户手动切换到其他实现的更新通道。
