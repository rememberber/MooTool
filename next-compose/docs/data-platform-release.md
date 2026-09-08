# 数据、平台能力与独立发布

## 1. 身份表：必须统一使用

以下为本产品拟定值，尚未注册到根清单或创建安装包。由单一 `ProductIdentity` 和构建配置生成，不在工具页面散落字符串。

| 项目 | 规定值 |
| --- | --- |
| Product ID | `next-compose` |
| 完整名称/安装显示名 | `MooTool Next Compose` |
| 应用 ID / bundleID / AppUserModelID | `com.rememberber.mootool.next.compose` |
| Kotlin 包前缀 | `com.rememberber.mootool.next.compose` |
| macOS 应用目录 | `MooTool Next Compose.app` |
| Windows 安装目录/快捷方式/卸载显示名 | `MooTool Next Compose`，不得仅为 MooTool |
| Linux 包/命令 | `mootool-next-compose` |
| Linux desktop entry | `com.rememberber.mootool.next.compose.desktop` |
| 版本来源 | 本产品 `gradle.properties` 中 `appVersion`；初始建议 `0.1.0` |
| Git tag | `next-compose-v{semver}` |
| Release 标题 | `MooTool Next Compose {semver}` |
| 更新产品节点 | `update-manifest.json` 的 `products.next-compose` |
| Secret service/account namespace | `com.rememberber.mootool.next.compose` |
| URL scheme（确需才启用） | `mootool-next-compose` |
| 自动化/开机项/helper 标识 | 以 `com.rememberber.mootool.next.compose` 开头 |

Windows `upgradeUuid` 首次创建时生成一次并提交到本产品配置，后续版本稳定不变；不复制其他版本 UpgradeCode。应用 ID、安装目录、可执行名称、快捷方式和卸载注册项分别验证，不能只改一个 bundleID 就声称隔离完成。

macOS 的 bundle 名、菜单/Dock 名、安装路径和 bundleID 都明确。Linux 的包名、desktop 文件、图标名、Exec/启动器身份与窗口分组映射分别核实；不依赖大小写差异隔离。

## 2. 路径布局与运行隔离

用 `AppPaths` 解析 OS 目录及自定义根，禁止仅依赖 `user.dir`、临时目录或应用展示名。显示实际绝对路径供用户检查。

| 数据类型 | macOS | Windows | Linux |
| --- | --- | --- | --- |
| 配置/引导描述 | `~/Library/Application Support/com.rememberber.mootool.next.compose/config/` | `%APPDATA%/MooToolNextCompose/config/` | `$XDG_CONFIG_HOME/mootool-next-compose/` |
| 默认持久数据 | 上述 Application Support 根下 `data/` | `%APPDATA%/MooToolNextCompose/data/` | `$XDG_DATA_HOME/mootool-next-compose/` |
| 缓存/更新暂存 | `~/Library/Caches/com.rememberber.mootool.next.compose/` | `%LOCALAPPDATA%/MooToolNextCompose/cache/` | `$XDG_CACHE_HOME/mootool-next-compose/` |
| 日志/状态 | `~/Library/Logs/MooToolNextCompose/` | `%LOCALAPPDATA%/MooToolNextCompose/logs/` | `$XDG_STATE_HOME/mootool-next-compose/` |

Linux XDG 变量缺省时分别使用 `~/.config`、`~/.local/share`、`~/.cache`、`~/.local/state`，不直接使用字符串 `$XDG_*`。非绝对/不可写值需可解释降级。

持久数据根建议：

```text
data/
  product.json                     # productId、schemaVersion；防止选错目录
  mootool-compose.sqlite
  vaults/quick-note/
  vaults/json/
  images/
  drafts/
  backups/
  imports/
```

- 设置目录与数据根区分：更改自定义数据根后，固定配置目录保留指向新根的引导描述，重启才找得到。
- 开发/测试使用 `MOOTOOL_COMPOSE_DATA_DIR` 等本产品专用覆盖参数，覆盖后配置、数据、缓存和锁均落隔离 profile；凭据用测试 backend，禁止访问真实 Keychain。
- 同一产品的单实例锁按用户/固定身份管理；需要多测试 profile 时额外包含规范化 profile 标识。第二次启动只聚焦本产品，不能通过 Java/Electron 的 lock/端口联络。
- 临时文件、进程管道、下载目录、日志、托盘和唤醒 token 全部自有；不争用固定通用 localhost 端口。
- 自定义根不得等于应用安装目录、另一产品的标记目录或它们的子目录。选择已有普通文档库时显示实际影响，不自动取得写入所有权。

## 3. 持久化模型

### 3.1 设置

独立 settings schema 从 1 起，按 general/appearance/layout/editor/network/data/vault/runtime/tools/shortcuts 分域。读取时合并默认值、校验范围，升级显式迁移；不能复制 Electron 的 schemaVersion=12 当成本产品版本。

设置 JSON 采用同目录临时文件写入、flush、原子替换；启动读取失败保留损坏副本并展示恢复入口，不直接覆盖。写入失败保留内存未保存状态。多窗口通过统一 repository 发布变化。

### 3.2 SQLite

首选显式 SQL、事务、版本化 migration；不沿用其他版本数据库文件或连接。表建议：

| 实体 | 关键字段/约束 |
| --- | --- |
| schema_migrations | version、applied_at、checksum |
| tool_sessions | tool_id、schema_version、state_json、updated_at |
| history | id、tool_id、operation、summary、input/output/options、created_at；每工具保留上限 |
| favorites | id、tool_id、group/name、payload、排序；唯一/重复策略明确 |
| http_requests / http_history | 完整有序行数组、body/type、请求选项、响应摘要 |
| translation_words / translation_history | 原文、译文、语言、服务、编辑时间 |
| host_profiles | 名称、内容、创建/修改时间；区别系统生效状态 |
| image_assets | 相对文件路径、类型、尺寸、摘要、时间；事务协调文件 |
| import_runs | 来源产品/版本、内容 hash、导入映射、状态、回滚信息 |

数据库不能当无限大文件仓库；大文本草稿/图片/附件存文件，DB 存索引与元信息。事务内更新索引，临时文件完成后提交；崩溃后清理孤儿 temp 的规则明确。

迁移前使用真实 SQLite 一致性备份或关闭连接后复制，包含 WAL 处理；不能运行中只复制 `.sqlite` 主文件。验证迁移成功才切换 schema，失败可恢复旧快照。

### 3.3 凭据与敏感数据

SecretStore 使用平台凭据系统：macOS Keychain、Windows 凭据/DPAPI 适配、Linux Secret Service；所有条目有本产品 namespace。平台服务不可用时提供会话内存模式或用户明确设置的加密备选，不能无提示退到明文。

代理密码、Git token 不写普通 settings、日志、进程 argv、默认备份。HTTP 认证与私钥相关历史默认遮蔽或不持久化，界面说明仍保存哪些内容。工具展示的用户输入加密操作与产品自身凭据存储分别实现。

## 4. Vault、附件和文件冲突

JSON/随手记默认分别使用自有根，可以由用户设置。路径 normalize + canonical 校验，同时处理 symlink、大小写折叠、`..`、跨卷移动；字符串 `startsWith` 不足以防目录逃逸。

保存流程：读取基准内容 hash/元信息 → 编辑 dirty → 同目录临时写 → 提交前再次比较当前外部版本 → 原子替换 → 更新索引及已保存 revision。mtime 只能帮助快速判断，不能唯一判断相同内容。

外部变化与本地 dirty 同时存在时保留两个版本，展示 diff/重新载入/另存/合并选择；不以最后写入覆盖。watcher 事件去重、防止自己的写入触发循环；初次扫描和后续增量合并应有 revision。

重命名/移动涉及目录、附件引用、索引与选中状态：先构建完整计划，检测冲突，再提交；失败回滚。跨卷移动采用复制校验后删除，不能假设 rename 原子。

图片附件先保存并校验可读取再插入正文引用；连续粘贴串行保序。同名生成稳定唯一名，存内容 hash 可用于去重。清理仅删除确定未引用且属于本产品的附件；不追随 symlink 清理外部文件。

文件导入支持 UTF-8/BOM、CRLF/LF、无末尾换行等明确策略；不能无说明改变二进制或未知编码。用户原文件默认只读导入/另存，覆盖通过明确动作。

## 5. Git 工作流

GitService 按规范化仓库根串行调度。优先 Git CLI，运行前检测版本/路径；远程、用户名、token 属于本产品设置。不要全局修改用户 git config 或将 token 嵌入 remote URL。

功能范围：仓库初始化/绑定、状态、文件 diff、暂存/取消暂存、提交、日志/详情、拉取、推送、冲突定位、merge/rebase 等进行中状态及继续/中止、明确的丢弃/恢复入口。具体按钮覆盖以 JSON/随手记当前 Git UI 为参照。

- 操作前 flush 对应文档保存；保存失败停止 Git 动作；dirty 工作树、未完成 merge 和远程认证失败清楚显示。
- 自动提交参考空闲 30 秒/工具失活 120 秒；默认自动拉取间隔 0（关闭）；实际 timer 条件写成测试。
- 自动任务只作用于用户配置的本产品文档库，不对任意选中仓库自动提交/push。
- 自动 pull 遇本地未保存、冲突或 Git 操作进行中暂停并显示原因，不自动 hard reset/force push。
- 自动与手动任务共享仓库锁，退出取消/收尾；watcher 暂停/合并刷新不丢外部变化。
- 丢弃更改和中止操作要显示目标及影响，可恢复副本按策略保留。

明确区分“本地备份”“Git 提交”“推送远程”。提交成功不能显示同步成功，远程失败不应删掉本地内容。

## 6. 备份、恢复、数据迁移

备份包含 productId、appVersion、schemaVersion、时间、内容清单/hash、设置、SQLite 一致性快照、Vault/附件及图片。缓存/下载/日志不默认打包，凭据不默认导出。大型文件后台处理，显示数量/大小/进度。

恢复：检查格式/版本/磁盘空间 → 解压到临时目录并校验路径/hash → 展示覆盖/导入范围 → 备份当前数据 → 关闭 writer/watcher → 提交 → 验证并重建索引 → 恢复服务。失败保留旧数据、清楚报告恢复位置；压缩包路径穿越/链接不能写出目标根。

移动数据目录：检查目标可写和身份 → 暂停写入 → 一致性快照/复制校验 → 更新引导配置 → 启动新根 → 验证成功后提示旧副本位置。失败回退旧配置，不能先删原数据。

Java/Electron 数据导入是**用户主动选择的独立适配器能力**，不作为安装依赖，也不默认启动扫描。首版必需本产品备份恢复；跨产品导入范围应按明确支持的来源版本列明，不承诺读取所有历史格式。

每个导入适配器：只读检查来源（SQLite 使用一致性副本）→ 类型/数量预览 → 当前 Compose 备份 → 事务写入自有模型 → 导入报告/幂等标识 → 回滚演练。原产品数据库/配置/文档保持不变；不会把其 Vault 路径直接设为本产品默认写入根。

## 7. 平台能力矩阵

所有能力经平台接口暴露 Supported/Unavailable/PermissionRequired/Failed，不以布尔 false 混淆所有失败。以下为实施方向，未经真实 OS 测试不标支持。

| 能力 | macOS | Windows | Linux | 共同验收 |
| --- | --- | --- | --- | --- |
| 文件选择/打开目录 | AWT/原生适配 | 原生或经验证桌面 dialog | portal/桌面 dialog 或可用适配 | Unicode/空格、多选、取消、过滤 |
| 文本/图片剪贴板 | 系统 clipboard | 系统 clipboard | 桌面 clipboard，验证会话后端 | PNG alpha、DPI、所有权与取消 |
| 托盘/关闭 | AWT Tray/桌面整合 | 系统托盘 | 环境支持时启用 | 不可用时保持可恢复主窗口，不能 hide 后失联 |
| 截图/屏幕取色 | 录屏权限、屏幕坐标适配 | 桌面捕获/DPI 适配 | X11 与 Wayland 分开；Wayland 优先 portal | 真实图像、负坐标、多屏、拒绝/取消 |
| hosts | 受限提权服务 | UAC 受限写入 | polkit/受限服务 | diff、备份、并发变更检测与恢复 |
| 环境变量 | 明确支持的配置文件/会话 | 用户/系统注册表作用域 | 明确 shell/session/system 配置范围 | 新进程生效、备份、拒绝权限 |
| 屏幕唤醒 | 原生 assertion 适配 | 系统电源请求适配 | D-Bus/桌面 inhibitor | token 引用计数、退出释放 |
| 系统主题 | 经验证系统监听/框架状态 | 经验证系统监听/框架状态 | 桌面环境读取，失败可手动选 | system 模式跟随，显式主题稳定 |
| 读屏 | Compose 与 Swing 分别验证 | Java Access Bridge 与打包模块 | 官方当前存在支持限制 | 键盘仍须可用，不夸大平台支持 |

普通业务不需要主进程管理员权限。需要提权的操作传受限的目标和变更计划，不向 helper 暴露“任意 shell”入口。系统 hosts/环境为共享系统资源，应用内提示影响范围；这是显式修改的正常效果，不声称不同工具箱间完全无影响。

## 8. 独立构建与打包

使用 Compose Gradle 插件的 jpackage/jlink，安装镜像包含所需 JVM。官方提供 DMG/PKG、MSI/EXE、DEB/RPM，各格式在对应 OS 构建；JAR 不等于独立桌面安装包。[官方打包说明](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html)

首发包型建议：macOS arm64 DMG、Windows x64 MSI、Linux x64 DEB；RPM/EXE 按测试增加。AppImage 不是 Compose 插件的原生 TargetFormat，若要提供需单独工具链/验收，不能编造 `TargetFormat.AppImage`。

JDK 模块至少按实测包含 UI、HTTP、SQLite、加密 provider、命名/日志、可访问性及所需 helper 模块；`suggestModules` 是线索，反射/native 依赖仍要真启动验证。先保证正确，再裁 runtime；不得用完整开发 JDK 下启动成功替代最终镜像检查。

原生库和 protoc/tracer/helper 按 OS/arch 分发，并保留许可证/checksum；开发态和安装态通过一个资源定位服务解析。应用不能从 build 工作目录或相邻产品找 helper。

产物命名：

```text
MooTool-Next-Compose-{version}-mac-arm64.dmg
MooTool-Next-Compose-{version}-win-x64-setup.msi
MooTool-Next-Compose-{version}-linux-x64.deb
```

版本从 appVersion 单一来源生成 UI、安装元信息、tag 校验与说明。首轮稳定安装包按合法三段版本生成；beta/rc 初期只作明确的测试镜像/工件，不进入自动安装通道。若后续发行预发布安装包，先实现各平台单调可升级版本映射与测试，不能直接把 `0.1.0-beta.1` 填入 MSI/DMG 数字字段。

## 9. 更新与发行

遵循根 `RELEASE_CONVENTIONS.md` 的独立产品规则。首次真实发行时增量登记 `next-compose`：本产品版本来源/tag/工作流、更新清单节点及独立下载入口；仅新增相关配置，保留其他产品工作。

- 只读取编译期 `next-compose`；不提供切换成 Electron/Java 更新的设置。
- SemVer 只在同产品内比较，稳定版忽略 prerelease；根全局 `/releases/latest` 不用于版本判断。
- 只选当前 OS/arch 包，架构按实际进程/安装架构识别；无匹配包只打开本产品 release 页，不下载别的平台包。
- 下载 HTTPS URL 到自有临时目录，校验字节数与 SHA-512，成功后原子标记 ready；部分下载、取消、断网可重试，校验失败不执行。
- 首版完整流程为检查→展示同产品说明→下载→校验→打开安装包。不能标“自动安装完成”或调用 Electron updater。
- 未签名包清楚说明平台安装体验；签名/公证是本产品配置，不能借其他产品证书/元数据假装签名。启用自动安装需另行实现平台退出/替换/失败恢复。
- Compose 为非主力独立产品，发布使用 `make_latest: false`，不覆盖 Electron 的仓库全局 Latest。
- tag `next-compose-v*` 只构建本产品；一般 PR 用目录过滤；不要使 `v*` Java 流水线误发布 Compose。
- Release 正文完整产品名、独立安装说明、English/中文/日本語、本产品前后版本变化。源文件 `release-notes/{version}.md`。
- 上传并验证实际资产后才将清单节点启用/追加版本，禁止预填不存在的下载链接。

建议将独立构建流程留在本目录 scripts，根 `.github/workflows/next-compose-build.yml` 只调用它；这样取出目录仍可本地构建。签名、上传和公开发布不是本次文档交付的已执行动作。

## 10. 并存、升级与卸载验收

至少同时安装 Java、Electron 和 Compose；其他版本若已有包也纳入，不要求为了测试先编译所有产品。

1. 启动三个程序，进程/窗口/Dock/托盘/开始菜单可区分，Compose 的二次启动只聚焦 Compose。
2. 分别修改主题/草稿/历史、创建同名笔记，重启后互不影响；默认目录、DB、Secret 命名空间不同。
3. 升级 Compose 后只替换本产品，旧数据仍可用，Java/Electron 安装文件和配置不变。
4. 以临时测试数据演练卸载；仅卸载本产品，默认保留用户数据；用户主动清理时严格限定本产品标记路径。
5. 使用文件摘要/目录快照证明其他产品未变化；系统 hosts 等显式操作另列测试，不能算进默认隔离断言。
6. 不在开发机真实个人数据上演练破坏性卸载或恢复；使用 VM/临时账号/profile。

“产品独立”必须有安装与使用证据，不能仅凭项目目录名或 packageName 判断。
