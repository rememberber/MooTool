# 数据、平台、安装与发布

## 1. 独立产品身份

以下为 `next-fx` 拟定契约，P0实现并测试。当前尚未在根发布文档/更新清单登记，也没有发布版本。

| 项目 | FX值/规则 |
| --- | --- |
| 产品 ID | `next-fx`，编译期固定，不允许设置页切到其他产品 |
| 全名/窗口/快捷方式 | `MooTool Next FX`；工具窗口追加工具名 |
| 应用/Bundle ID | `com.rememberber.mootool.next.fx` |
| Java包前缀 | `com.rememberber.mootool.nextfx` |
| Maven坐标 | `com.rememberber.mootool:mootool-next-fx` |
| 版本唯一源 | `next-fx/pom.xml`；构建生成版本资源，UI/安装包/清单读取它 |
| 初始开发版本 | 建议 `0.1.0-SNAPSHOT`，不是已经发行的版本；发行时去掉SNAPSHOT |
| macOS应用名 | `MooTool Next FX.app`，安装路径不叫`MooTool.app` |
| Windows可执行/菜单 | `MooTool Next FX.exe`，独立Start Menu/卸载项/安装目录 |
| Windows升级身份 | 在P0生成本产品固定UpgradeCode，跨FX版本保持；禁止复制其他产品GUID |
| Linux包/launcher | `mootool-next-fx`；Desktop Entry `com.rememberber.mootool.next.fx.desktop` |
| 凭据命名空间 | service=`com.rememberber.mootool.next.fx`，按profile/account区分 |
| 锁/IPC | 产品ID+用户+profile的专有锁及私有端点；不使用其他产品端口/锁名 |
| tag / Release标题 | `next-fx-v{version}` / `MooTool Next FX {version}` |
| 更新节点 | 仅 `products["next-fx"]`，不继承Electron updater元数据 |
| 开发身份 | Bundle/配置/凭据/锁使用 `.dev` 或明确dev后缀；UI显示Development |

产品间不共享代码发布节奏。可复制有权算法/资源/fixture并保留许可，在本目录维护；禁止源码符号链接、根POM继承、以另一产品构建输出为依赖、安装另一个MooTool才能运行等关系。

不同产品可同时安装/运行/升级/卸载；同产品常规升级可替换自身旧版应用并迁移自身数据。并行运行多个FX版本时需显式不同profile，旧版不允许打开更高schema的同一可写数据库。

## 2. 路径与首次启动

默认目录由AppPaths单点解析，用户home/系统标准目录来自平台API；不以`user.dir`或安装目录作为普通用户数据根。XDG变量为空时使用标准回退。

| 内容 | macOS | Windows | Linux |
| --- | --- | --- | --- |
| 配置/启动定位 | `~/Library/Application Support/com.rememberber.mootool.next.fx/config/` | `%APPDATA%\MooToolNextFX\config\` | `${XDG_CONFIG_HOME:-~/.config}/mootool-next-fx/` |
| DB/Vault/附件默认数据根 | `~/Library/Application Support/com.rememberber.mootool.next.fx/data/` | `%LOCALAPPDATA%\MooToolNextFX\data\` | `${XDG_DATA_HOME:-~/.local/share}/mootool-next-fx/` |
| 缓存/下载 | `~/Library/Caches/com.rememberber.mootool.next.fx/` | `%LOCALAPPDATA%\MooToolNextFX\cache\` | `${XDG_CACHE_HOME:-~/.cache}/mootool-next-fx/` |
| 日志/锁/恢复journal | `~/Library/Application Support/com.rememberber.mootool.next.fx/state/` | `%LOCALAPPDATA%\MooToolNextFX\state\` | `${XDG_STATE_HOME:-~/.local/state}/mootool-next-fx/` |

表内`~`/变量只用于文档表达；不要把未展开的字符串写到磁盘。dev在各目录使用独立后缀。bootstrap配置保存自定义dataRoot位置，不依赖dataRoot才能找到自己。

数据根结构建议：`product.json`（productId/schema/profile）、`MooToolNextFX.db`、`vaults/json/`、`vaults/quick-note/`、`attachments/`、`images/`、`backups/`。锁存固定state目录；自定义dataRoot另加写入锁，两个profile指向同库要拒绝并发写入。

首次启动创建自有目录并标记productId；不会查找并打开 `~/.MooTool`、Electron默认userData、其他next目录或数据库。识别到另产品product标记时拒绝作为本产品应用数据根；显式导入复制到新目录后再使用。

更改dataRoot：校验目标与空间/权限 → 暂停写入和后台Git → 创建一致备份 → 复制并校验 → 在目标成功打开后原子提交bootstrap指针 → 失败回滚。原目录先保留为可恢复副本，不能先删除再迁移；不把“路径设置成功”当作数据迁移成功。

可选便携模式使用明确`--portable`和本产品marker/数据子目录，默认不启用；只读安装位置提示选择目录，不能无提示落到其他版本路径。为测试提供可注入AppPaths/profile，不能改系统HOME环境来隔离测试。

## 3. 数据模型和一致性

配置JSON、SQLite schema分别从本产品版本1开始；不要沿用Electron schema=12。推荐分开：Settings、Workspace/WindowState、ToolDraft、HistoryEntry、FavoriteFolder/Item、HttpCollection/Request/History、HostProfile、TranslationHistory/Word、ImageAsset、VaultIndex/DocumentMetadata、MigrationJournal。

SQLite：开启外键、WAL、busy_timeout；单写者，短事务；历史按工具裁剪200条，专用历史自行定义；大正文和附件避免多个无意义副本。时间用UTC instant保存，UI按用户时区显示。DB迁移有顺序编号/校验/备份，不支持的未来schema只读诊断或拒绝，不自动降级写坏数据。

配置文件：同目录临时文件、flush、原子替换；不支持原子移动时保留备份并明确恢复路径。SQL事务和文件系统不是同一事务，图片/附件用staging+引用提交/补偿，启动修复未完成journal，不能产生悬空引用。

保存策略：编辑防抖约600ms；每文档串行按revision写，切文档/退出前flush；保留dirty直到对应revision确认写成功。错误状态包含真实路径和原因，可重试/另存；不以Toast消失替代保存状态。撤销历史/光标不因自动保存而重设。

外部修改采用mtime/size/hash基线，watcher只提示待校验，不作为绝对正确事件；保存前复核，检测到本地与外部均改变则进入冲突。用户选择比较/另存/覆盖当前内容，不能自动“最后写入者胜出”。原子改名、删除后重建、重命名大小写、多屏窗口同时编辑都需测试。

凭据：macOS Keychain、Windows Credential Manager/DPAPI、Linux Secret Service候选适配；service/account用本产品命名空间。后端不可用时允许仅本次会话，禁止明文静默持久化或源码固定密钥；日志、历史、备份默认不含token/密码/私钥/认证header。用户主动保存的HTTP认证字段应有明确开关和遮蔽策略。

## 4. Vault、附件和Git

JSON和随手记默认两个自有Vault。文档本体用普通JSON/Markdown文件，元数据和索引自有；原版frontmatter只在导入适配中解析，保留未知字段，不将元数据显示到正文。路径统一normalize/realpath，防止`..`/symlink写出库根；外部文件编辑通过用户明确选取的独立授权路径处理。

树支持名称/路径/正文搜索、创建/修改时间排序、CRUD/移动/拖放、忽略、展开模式、重启恢复。watcher需要递归注册/新目录处理、事件合并和失效重扫，不能以每键全盘扫描替代索引。

附件导入：校验图片和大小 → 写临时文件 → 命名去重/内容hash → 落入自有附件目录 → 成功后插入相对Markdown引用。文本粘贴不被截获；连续图片粘贴按用户顺序，失败不留下假引用。移动/导出文档同步处理相对引用；清理先扫描引用，提供候选列表，不删除仍被引用资源。远程图片不默认批量下载。

Git的范围是用户明确配置的Vault仓库。初次配置可让用户选择目录/remote，不能自动拿另产品仓库开始提交。支持状态、diff、历史、提交、pull、push、冲突解决/继续/中止；凭据失败可重试。本地编辑无需Git可用。

Git各仓库串行锁；自动保存完成并校验后才做checkpoint。默认策略参考autoCommit=true、idle30s、inactive120s、autoPullMinutes=0，但**只有仓库已配置且用户启用策略后才调度**。自动提交不自动push；不以reset/clean/force push清理冲突；pull前处理dirty和冲突，正在编辑文件不能被无提示重载。

允许用户主动选择同一外部Vault或Git remote作为协作资源，但必须显示这是共享资源，检测工作区冲突并对各自元数据命名空间隔离。默认安装互不影响不等于两个应用对同一文件写入可以忽略冲突。

## 5. 备份、恢复和跨产品导入

备份格式header包含`productId=next-fx`、formatVersion、appVersion、dbSchema、创建时间和文件hash清单。配置/DB/Vault/附件范围可选且显示清楚，默认排除系统凭据、运行时临时文件、下载缓存、日志、外部未选择的Vault。

SQLite备份使用在线backup API/一致snapshot或暂停连接后正确checkpoint；禁止WAL活跃时只拷贝`.db`。恢复先校验压缩路径（防Zip Slip）、hash、大小/解压上限、schema/版本 → 预览范围 → 自动备份当前FX数据 → staging恢复 → 成功后切换 → 失败完整回滚。

跨产品导入是单独向导：用户选源 → 只读扫描/预览映射和数量 → 复制到FX staging → 转换 → 校验/去重 → 提交FX存储 → 输出报告。源目录/DB/备份不改、不移动、不删除；原产品可继续使用。备份productId不同不能直接恢复覆盖FX数据库。凭据默认不迁移，重新配置。

导入报告记录来源版本/时间、各类数量、跳过/冲突/失败与可重试ID；二次导入幂等或明确生成副本。不在启动时自动导入、不持续双向同步原产品数据库。

## 6. 平台能力和降级

能力接口返回 `available / permissionRequired / unsupported / failed` + 原因/恢复动作。以下是实施路线，不是已验收支持列表。

| 能力 | macOS | Windows | Linux |
| --- | --- | --- | --- |
| 窗口/文件对话框/剪贴板 | JavaFX为基础，原生菜单/窗口行为实测 | 同左，高DPI/多显示器实测 | X11/Wayland分别记录 |
| 托盘/恢复主窗 | AWT SystemTray或经验证native适配 | 同左 | 桌面托盘不保证可用，保留其他恢复/退出路径 |
| 截图/取色 | 系统屏幕录制权限；JavaFX Robot/平台API实测 | Robot/平台API，DPI坐标换算 | X11与Wayland portal分别实现，拒绝/黑图不算成功 |
| 防显示器休眠 | 受控系统API/进程持有者 | 系统执行状态API持有者 | logind/桌面inhibit适配，缺失就显示限制 |
| hosts | `/etc/hosts`，特定写入动作提权 | 系统hosts，受限helper/UAC | `/etc/hosts`，polkit/明确helper |
| 环境变量 | 明确支持的用户shell/session配置 | 用户/系统注册表scope和环境刷新 | 明确配置文件/session，不能承诺统一全局变量表 |
| 凭据 | Keychain | Credential Manager/DPAPI | Secret Service，缺失仅会话 |

AWT使用EDT，FX使用FX线程；双向消息异步，避免互相阻塞等待。Robot必须按API线程约束调用，像素处理放后台；多显示器负坐标、scaleX/scaleY、逻辑坐标到图像像素各自记录。不把一次全屏screenshot位图持续留在缓存中。

系统资源修改先展示diff/目标/范围及备份，用户点击“应用到系统”才写；保存Host方案只写FX数据库。写入时重新校验源文件防并发覆盖；只提权必要操作，不以管理员启动整个应用。网络命令argv传参，cURL输入只解析不执行。

防休眠按窗口/演示持有token引用计数，最后一个释放后恢复；关闭/异常释放。后台hardware轮询、时钟、watcher按活跃需求管理，不因进入首页继续高频刷新所有工具。

## 7. 构建、运行时和安装包

目标发行矩阵：macOS arm64/x64、Windows x64、Linux x64。建议测试底线为macOS13+、Windows10/11 x64、Ubuntu22.04/24.04 x64；这些是目标范围，须在P0按实际OpenJDK/OpenJFX/原生依赖要求复核，未通过不承诺。Windows/Linux ARM可后续独立增加。

自带OpenJDK runtime、JavaFX对应OS/arch原生库、所选字体/图标、SQLite/protoc/媒体处理等必要制品。普通用户不需要Java/Node/Git来启动主应用；Git和用户代码运行环境缺失只影响对应功能，并给安装/配置入口。

推荐打包路径：应用及普通依赖置classpath；jlink使用匹配版本/架构的JDK模块和JavaFX jmods构建runtime；jpackage引用该runtime及应用输入。JavaFX jmods、Maven模块和SDK不可混版本；自动模块不传给jlink。实际模块通过jdeps+运行时验证维护到packaging清单；反射/ServiceLoader的驱动/编码/安全provider需要实际检查。

JDK25的jpackage不再默认绑定服务；按需添加模块或显式`--bind-services`。为内部worker保留runtime里的Java launcher，不能套用`--strip-native-commands`后仍假定`java.home/bin/java`存在。必须在成品中运行SQLite/TLS/GB2312/正则worker/图片/PDF smoke验证。[JDK25打包说明](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-overview.html)

各OS/架构原生构建并测试，不承诺单机交叉产出所有安装器；包格式推荐：macOS DMG，Windows MSI或EXE安装器及可选ZIP便携包，Linux DEB/RPM及可选tar.gz。AppImage是额外打包任务，jpackage不会直接产出。WiX/rpm-build/fakeroot等工具版本在对应runner固定。

文件名：`MooTool-Next-FX-{version}-{mac|win|linux}-{arm64|x64}-{packageKind}.{ext}`，例如未来`...-win-x64-setup.msi`、`...-linux-x64-portable.tar.gz`；相同扩展名也用packageKind消歧。应用版本和安装器数字版本分开转换，记录SemVer预发布及平台限制映射，禁止悄悄截断产生两个同安装身份版本。

签名/公证状态是FX自己的发行决定，不继承Electron当前策略。配置证书时验证签名链、macOS公证/所有内嵌原生二进制；无证书时可生成明确标注的测试或手动分发包，不假称已签名/系统信任通过。卸载只处理FX安装资源，默认保留用户数据；即便选择删FX数据也不能递归删除任意自定义共享目录。

## 8. 更新协议与发行流程

兼容根清单schemaVersion=1的注册结构，仅读取 `products.next-fx.releases`。每版本包含version/title/notes/prerelease/releaseUrl/assets；每asset含platform/architecture/packageType/priority/fileName/url/sha512/size。platform归一为darwin/win32/linux，aarch64→arm64、amd64→x64；SHA-512沿用根清单的Base64表示。

更新选择：自身productId → 自身通道可用SemVer → 当前OS/arch → 合适包型/优先级。无匹配只给自身发布页，不回退下载Electron/Java或其他架构。稳定版忽略预发布，beta/rc可接收后续预发布或正式；不用GitHub仓库全局Latest，也不字符串比较版本。

状态：idle/checking/upToDate/available/downloading/verifying/ready/error。无清单节点显示尚无发行，不报“已是最新版”掩盖缺节点；下载临时文件、超时/取消、实际字节和hash核对。HTTPS，URL来自已验证自身清单/允许源，不让任意UI文字成为下载/执行地址；checksum验证完整性但不等于发行者签名认证。

首版采用**下载并验证→用户打开安装器完成安装**；这相对Electron部分平台自动安装是FX主动差异，发布前登记。不能以一个`jpackage`包就声称有原子自更新/回滚能力。后续自动安装单独设计校验、退出/数据flush、提权、签名、失败恢复，不接Electron的blockmap/yml或quitAndInstall。

P7接入步骤：

1. 增量登记根 `RELEASE_CONVENTIONS.md` 的FX行、版本源与 `next-fx-v*`；创建本产品CI工作流，读取本目录脚本，不改其他产品tag匹配。
2. POM版本、生成资源、安装metadata和`release-notes/{version}.md`一致；说明按English/中文/日本語维护，明确独立产品线。
3. 只构建、测试、收集FX产物；tag只解析自身前缀，错误产品/版本/架构/缺少说明直接失败。
4. 在对应tag创建本产品Release并上传包；FX默认 `make_latest=false`，不抢占根约定的Electron全局Latest。
5. 资产验证可下载、size/hash匹配后，原子增量更新`products.next-fx`；其他节点语义保持不变，解决并发更新时基于最新清单合并。
6. 实测FX旧版发现/下载/验证本版，其他产品即使版本号更高也不影响；保留发布和安装证据。

本次文档任务不执行上述发布、不预填虚构资产URL、不修改根清单。未来实施可制作本产品CI变更供审查，发布动作按当时用户任务范围执行。

## 9. 共存验收是发行门槛

在干净用户/VM中依次安装原Java、Electron、FX，实际可用的其他产品也加入矩阵；逐个检查应用名/ID/安装路径/快捷方式/卸载项。同时运行并写不同主题/草稿/历史/凭据，重启确认隔离；用文件系统审计或目录hash验证只改本产品目录。

先升级FX，再更新另产品，确认只升级各自版本；卸载FX后另产品仍可启动且数据一致，反向亦然。FX新装不得弹出另产品已有草稿。显式导入后源产品保持原样；两个产品主动打开同一文件时必须出现冲突检测结果。

覆盖锁/IPC/托盘菜单/系统热键/文件关联/更新通道；文件关联默认不注册或由用户选择，不抢占现有关联。系统hosts/环境变量本身是共享资源，测试它们的作用范围和备份恢复，不把这类用户明确操作误判为产品私有数据隔离。
