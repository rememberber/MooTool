# MooTool 多产品发布约定

MooTool 采用“同一仓库、多个独立产品线”的发布方式。本文件是版本号、Git tag、GitHub Release、更新通道和发布自动化的统一约定；具体客户端的更新协议可以在各自实现文档中补充，但不得与本文件冲突。

## 1. 基本原则

1. Java、Electron 及未来的 Tauri、原生 macOS 实现是不同产品线，各自维护版本号和发布节奏。
2. 产品版本遵循语义化版本，例如 `1.0.0`、`1.1.0`、`2.0.0-beta.1`；不同产品可以同时拥有相同版本号。
3. Git tag 在整个仓库内必须唯一，因此除历史 Java 产品线外，tag 必须带稳定的产品 ID 前缀。
4. 产品线由 tag、Release、安装包名称、应用 ID 和更新清单共同隔离，不依赖长期分叉的产品分支。
5. 已发布的 tag 和 Release 不得改指向其他提交，也不得被另一个产品复用。

## 2. 产品与 tag 命名

| 产品 ID | 产品 | 版本来源 | Git tag | Release 标题示例 |
| --- | --- | --- | --- | --- |
| `java` | MooTool Java | `pom.xml`、`UiConsts.APP_VERSION`、`version_summary.json` | `v{version}` | `MooTool Java 1.7.10` |
| `next-electron` | MooTool Next Electron | `next/package.json` | `next-electron-v{version}` | `MooTool Next Electron 1.0.0` |
| `next-tauri` | MooTool Next Tauri | 启用产品时确定 | `next-tauri-v{version}` | `MooTool Next Tauri 1.0.0` |
| `next-macos-native` | MooTool Next macOS Native | 启用产品时确定 | `next-macos-native-v{version}` | `MooTool Next macOS Native 1.0.0` |

示例：

```text
v1.7.9                         # Java 1.7.9
v1.8.0                         # Java 1.8.0
next-electron-v1.0.0           # Electron 1.0.0
next-electron-v1.1.0           # Electron 1.1.0
next-electron-v2.0.0-beta.1    # Electron 2.0.0 beta 1
next-tauri-v1.0.0              # Tauri 1.0.0
```

历史 Java tag `v1.0.0` 至 `v1.7.9` 保持不变，后续 Java 版本继续使用 `v*`。`v*` 命名空间保留给 Java；新产品不得使用无产品前缀的 tag。除非进行单独评审和迁移，否则不把 Java 改为 `java-v*`，以免破坏历史连续性和现有自动化。

## 3. GitHub Release 约定

每个 tag 对应一个且仅一个产品版本的 GitHub Release：

- Release 标题必须包含完整产品名和版本号，不能只写 `v1.0.0`。
- Release 正文开头必须说明产品线，并注明它是否替代其他实现。
- Release 只上传当前产品的安装包和更新元数据，不混入其他产品的产物。
- 安装包文件名必须包含产品名、版本、操作系统和架构；同一平台存在多种包型时还必须包含包型。
- GitHub 自动生成的源码压缩包包含整个仓库，这是 monorepo 的预期行为；面向普通用户的下载入口应指向 Release 安装包。
- Release Notes 只能比较同一产品线的前后两个 tag，不能把另一产品的最近 tag 当作上一个版本。
- Electron 的版本说明以 `next/release-notes/{version}.md` 为唯一人工维护来源，GitHub Release 和 `update-manifest.json` 均从该文件生成。
- Java 的版本说明继续以 `src/main/resources/version_summary.json` 为唯一人工维护来源，GitHub Release 从当前版本节点生成。

建议在 Release 正文顶部使用类似说明：

```markdown
> 产品线：MooTool Next Electron
> 版本：1.0.0
> 本版本与 MooTool Java 独立安装、独立更新，不会覆盖 Java 版本。
```

## 4. `Latest` 与下载入口

GitHub 仓库只有一个全局 `Latest` Release，不能分别设置 `Latest Java` 和 `Latest Electron`。

本仓库约定：

1. `Latest` 表示当前推荐给新用户的主力稳定产品，而不是所有产品中数值最大的版本。
2. 当前主力产品为 MooTool Next Electron，因此稳定的 Electron Release 可以设为 `Latest`。
3. Java 独立维护版本及其他非主力产品发布时不得覆盖全局 `Latest`；自动化中应显式使用等价于 `make_latest: false` 的设置。
4. beta、rc 等测试版本必须标记为 pre-release，不得设为 `Latest`。
5. README 和官网应分别提供各产品的“最新版本”入口，不以仓库级 `/releases/latest` 代替产品更新清单。

Java 的历史更新协议没有预发布通道，因此 Java 发布工具只接受稳定版 `v{major}.{minor}.{patch}`。需要测试 Java 安装包时使用手动 CI 工件，不创建会被稳定客户端发现的预发布版本。Electron 支持 SemVer 预发布版本，并由客户端自动隔离稳定与预发布更新。

如果未来主力实现发生变化，应先修改本文件和下载入口，再调整哪个产品可以更新全局 `Latest`。

## 5. 分支策略

产品线不是分支线。默认分支应同时保存所有产品目录、公共资源、文档和 CI 配置。

- 日常开发使用当前主干与短期功能分支。
- 需要维护旧版本时，可以创建短期或维护型分支，例如 `release/java-1.x`，但发布身份仍由产品 tag 决定。
- 不为 Java 和 Electron 维护永久互不合并的两套主分支，避免公共文档、资源和自动化长期漂移。
- 同一提交可以被不同产品的 tag 引用，但每个 tag 只发布对应产品的产物。

## 6. CI 隔离与校验

发布工作流必须通过 tag 前缀触发，而不是根据整个仓库的最高版本判断产品：

| 工作流 | tag 触发规则 | 发布内容 |
| --- | --- | --- |
| `.github/workflows/build-installers.yml` | `v*` | MooTool Java |
| `.github/workflows/next-build-installers.yml` | `next-electron-v*` | MooTool Next Electron |

每条发布流水线至少应完成以下校验：

1. 从 tag 中解析产品 ID 和版本号。
2. 校验 tag 版本与该产品的版本来源完全一致。
3. 只构建、测试和收集当前产品目录的产物。
4. 校验产物文件名、操作系统、架构和包型没有冲突或缺失。
5. 创建或更新当前 tag 对应的 Release，并按本文件规则处理 pre-release 和 `Latest`。
6. 从当前产品的版本说明来源生成 Release Notes，不调用可能跨产品比较 tag 的默认自动说明。

普通提交或 Pull Request 可以使用目录过滤减少无关构建；正式发布必须以 tag 前缀作为最终产品边界。

## 7. 更新通道隔离

- Java 继续使用 `version_summary.json` 和 `download_links.json`，保证历史客户端兼容。
- Electron 使用根目录 `update-manifest.json` 中的 `next-electron` 节点。
- 未来产品必须在 `update-manifest.json` 使用自己的产品 ID，不得读取或写入其他产品节点。
- 各客户端只能比较自身产品节点中的版本，不能用 GitHub 仓库的全局 `Latest` 判断更新。
- 应用 ID、产品名和用户数据目录也应按产品隔离，保证不同实现可以同时安装。
- Electron 稳定版客户端忽略 `prerelease` 版本；beta/rc 客户端可以继续接收后续预发布版本，并在正式版发布后升级到正式版。
- Java 使用 `versionIndex` 判断版本先后及汇总中间版本说明，不使用字符串比较版本号。

Electron 的清单格式、资产选择和自动下载细节见 [`next/doc/update-products-and-assets.md`](next/doc/update-products-and-assets.md)。

## 8. 发布顺序

正式发布按以下顺序执行：

1. 更新目标产品的版本来源、变更说明和必要文档，并完成测试。
2. 创建符合本文件命名规则的 tag；CI 校验版本一致性。
3. 构建安装包和自动更新元数据，由流水线核对平台、架构、签名状态、文件名和更新元数据。
4. 所有目标校验通过后发布 Release，并按产品角色设置 pre-release 和 `Latest`。
5. Release 资产可访问后，再更新并启用该产品的更新清单。
6. 验证 README 下载入口、客户端更新检查和安装流程。

不要在 Release 资产尚未可用时提前向已发布客户端暴露新版本，以免客户端发现版本后无法下载。

当前 Electron 产品线明确允许未签名发布，流水线必须标注这一状态但不得因缺少证书而阻止 Release。未签名 macOS 客户端可以在后台下载并校验 DMG，完成后提示用户打开 DMG 并按常规方式拖入“应用程序”，但不得调用应用内自动安装；具体限制见 [`next/doc/update-products-and-assets.md`](next/doc/update-products-and-assets.md#6-未签名发布策略)。

## 9. Electron 首个正式版本

MooTool Next Electron 的首个正式版本固定使用：

```text
应用版本：1.0.0
Git tag：next-electron-v1.0.0
Release 标题：MooTool Next Electron 1.0.0
```

该版本不占用、删除或重建历史 Java tag `v1.0.0`，也不表示 Java 版本升级为 2.0.0。两个 `1.0.0` 属于不同产品线，可以在同一仓库中独立存在。
