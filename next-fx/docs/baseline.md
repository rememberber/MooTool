# 源码基线与参考地图

## 1. 本次核实范围

日期：2026-09-09。开始读取时仓库 HEAD：`3416c70e7a23fb1d5dcbf6244073dae490e8912d`；读取的是当时工作树。其他产品和根发布文档有未提交修改，本次不改动；并行工作可能继续改变仓库HEAD。检查范围为注册表、布局/CSS、设置、窗口管理、若干算法/服务及既有产品规格，**未启动 Electron 做实时截图验收，也未构建 JavaFX**。

逐工具规格是开发要求，不能解释为每个原版操作已在本次运行验证。详细实现开始前要补源码审计和同环境实测；静态源码、原版测试、旧文档不一致时，先复现再决定。

| 项目 | 源码观察 | 证据（相对仓库根） |
| --- | --- | --- |
| Electron 版本 | 1.1.4 | `next/package.json` |
| 原 Java 产品 | Maven 1.8.6，Java 21，Swing/FlatLaf；不是 JavaFX | `pom.xml`、`src/main/java/com/luoboduner/moo/tool/ui/` |
| next-fx 初始状态 | 空目录；现在只有本次开发规格 | `next-fx/` |
| 入口 | 首页 `mootool` + 25 工具，6 分组 | `next/src/app/toolRegistry.ts` |
| 沉浸布局 | 所有非首页工具 | `next/src/features/workbench/immersiveTools.ts` |
| 主窗口 | 默认 bounds 1440×920，最小 1080×720 | `next/electron/main/index.ts` |
| 工具窗口 | 默认 1100×760，最小 760×560，关闭收回；每工具保留同一 WebContentsView | `next/electron/main/toolWindowManager.ts` |
| 导航尺寸 | CSS 定义展开宽 248，隐藏标题宽 84；须结合后续覆盖与实际 computed style | `next/src/shared/styles/global.css` |
| 默认风格 | modern、system、blue、UI 13、编辑器 14、统一背景 | `next/src/shared/contracts/settings.ts` |
| 导航与最近 | classic、展开、显示分隔线，最近默认隐藏；最近最多 5 项 | 同上、`next/src/app/appStore.ts` |
| 设置 | 主工作区内 SettingsPage，11 类，源 schema 12 | `next/src/features/settings/SettingsWindow.tsx` |
| 历史/收藏 | 通用历史每工具最多200；通用收藏仅 regex/cron/colorBoard | `historyRepository.ts`、`toolRegistry.ts` |
| 限额 | HTTP 响应10 MiB；代码1 MiB、输出2 MiB | `networkService.ts`、`runtimeExecutionService.ts` |

这里的 bounds 是 Electron 窗口配置值，不是已测量的 JavaFX Scene 内容尺寸。截图对齐须记录客户区与窗口外框，不能将装饰边框误差误判成内部布局误差。

## 2. 参考优先级

用户目标 → 本产品已记录的设计决定/验收 → 冻结 Electron 的真实行为、源码及有效测试 → 旧 Java 的算法/平台能力 → 旧迁移计划、截图和其他产品文档。

优先对齐顺序：入口、面板关系、动作顺序、参数/默认、结果语义、状态/键盘、视觉细节。错误算法不因“对齐”而强制保留；同时保留 `sourceObserved` 和 `fxExpected`，说明原因。

本仓库已有 Compose 等规格可以作为问题清单参考；它们的技术选型、平台结论和状态不能移植成 JavaFX 事实。本产品文档自足，编译/测试不依赖这些目录。

## 3. 源码阅读地图

以下 Electron `src/` 从 `next/src/` 起，`main/` 从 `next/electron/main/` 起；Java 相对 `src/main/java/com/luoboduner/moo/tool/`。路径是阅读参考，禁止写进 FX 的源码目录或 classpath。

| 范围 | Electron 入口 | Java 补充 |
| --- | --- | --- |
| 工具、导航、会话 | `src/app/toolRegistry.ts`、`appStore.ts`、`features/workbench/Workbench.tsx` | `ui/form/MainWindow.java`、`ui/startup/` |
| 视觉和公共控件 | `src/shared/styles/global.css`、`src/shared/components/` | `ui/UiConsts.java`、`util/EditorFontUtil.java` |
| 设置和快捷键 | `src/features/settings/`、`src/shared/contracts/settings.ts`、`src/shared/i18n/messages.ts` | `util/ConfigBaseUtil.java`，只参考字段，不复制存储地址 |
| 编辑器 | `TextCodeEditor.tsx`、`findReplace.ts`、`codeEditorViewState.ts`、`features/quickNote/QuickNoteCodeEditor.tsx` | `util/TextAreaUtil.java`、`util/UndoUtil.java`；Swing 控件不直接成为 FX 主编辑器 |
| JSON | `src/features/json/`、`main/jsonVaultRepository.ts` | `ui/form/func/JsonBeautyForm.java`、`util/JsonBeautyVaultUtil.java` |
| 随手记/附件 | `src/features/quickNote/`、`main/quickNoteVaultRepository.ts` | `util/QuickNoteVaultUtil.java`、`QuickNoteFrontmatter.java`、`QuickNoteAttachmentUtil.java` |
| Git | `main/vaultGitService.ts`、`vaultGitCheckpointScheduler.ts` | `util/QuickNoteGitUtil.java`、`VaultExternalRefreshSupport.java` |
| 网络和系统 | `main/networkService.ts`、`systemService.ts`、`displaySleepService.ts` | `service/`、`util/HostFileUtil.java`、`EnvironmentVariableService.java` |
| 代码执行 | `src/features/runtime/`、`main/runtimeExecutionService.ts` | `ui/form/func/JavaConsoleForm.java` |
| 动态 Protobuf | `src/features/protobuf/` | `util/ProtoBufUtil.java`、`ProtocRunner.java` |
| 图片/PDF | `src/features/image/`、`src/features/pdf/`、`main/imageVectorizationService.ts`、`pdfService.ts` | `util/ImageSvgUtil.java`、`ImageCompressUtil.java`、`ImageWatermarkUtil.java` |
| 数据/恢复 | `main/historyRepository.ts`、`favoriteRepository.ts`、`p5Repository.ts`、`backupService.ts`、`legacyMigrationService.ts` | `domain/`、`dao/`、`util/SqliteUtil.java` |
| 更新和安装 | `main/update*.ts`、`next/package.json`、`next/doc/update-products-and-assets.md` | 根 `RELEASE_CONVENTIONS.md`、`util/DownloadLinkSelector.java` |
| 测试与运行场景 | `next/src/**/*.test.ts`、`next/tests/electron/*.spec.ts` | `src/test/` 中对应工具测试 |

逐工具 TSX 与 Java Form 对照见 [功能规格](feature-parity.md)。同名工具还需查看 shared/contracts 和 main service，不能只复制页面按钮。

## 4. 已识别的易错点

- `java` ID 对应 Java、Groovy、Python、Node.js，不是只有 Java；应用自身 JVM 与用户代码执行环境不同。
- `messageBoard` 是本地大字展示/演示，不是发布到服务器的留言系统。
- 所有非首页工具使用沉浸工作区，不应为每页重复增加品牌区、大标题和卡片边框。
- HTTP 当前六种 Body MIME；GET/HEAD/OPTIONS 的 Params 追加 URL，其他方法有正文发正文、无正文发 URL 表单。文件上传 multipart 不是已核实基线。
- Cron 输入6/7字段，`?`/week/year 做适配，不等同完整 Quartz；JavaFX 选用库必须建兼容层，不能按库默认悄悄接受5字段。
- Crypto 对称路径使用 ECB/PKCS#7、Hex，密钥先按字符截断/补字符 `0` 再转 UTF-8，中文 key 的行为必须核对。
- Protobuf `keepCase: true`，解码 JSON 包含 defaults，long/enum/bytes 为字符串；Java JsonFormat 默认规则可能不同。
- 图片要求真 SVG 矢量化；颜色运算五种；随手记快速替换24项。库中有 OCR 不代表本版需新增 OCR。
- 源历史支持标志与专用集合/单词本/Vault 不同；不得统一塞入一个文本历史模型。
- 根发布约定尚未登记 `next-fx`；本产品 ID、版本源和清单节点是拟定契约，不是已经上线。

## 5. 官方资料与版本决策

以下在 2026-09-09 查询。采用 **OpenJDK 25 + OpenJFX 26.0.2 + Maven 3.9.16** 作为 P0 输入组合；不声称本次编译验证过。JDK 的长期更新依赖所选发行商，不能把 Oracle JDK 的授权或 Gluon 商业支持条件套到所有 OpenJDK/OpenJFX 包上。P0 从发行商官方源选定可再分发构建并记录 patch、架构、SHA-256 与许可。

| 官方资料 | 本规格依据 |
| --- | --- |
| [OpenJFX 26 Highlights](https://openjfx.io/highlights/26/) | FX26 最低 JDK24；headless 是 prototype，不能代替真实桌面验收 |
| [Gluon JavaFX](https://gluonhq.com/products/javafx/) | 查询时列出26.0.2；使用公开可分发制品，商业 LTS 补丁可用性另核验 |
| [OpenJFX 入门](https://openjfx.io/openjfx-docs/) | JavaFX 独立于 JDK；可通过 Maven 取得平台模块、构建自有运行时 |
| [Maven 官方下载](https://maven.apache.org/download.cgi) | 查询时发布版为3.9.16，Wrapper固定版本和分发校验 |
| [RichTextFX](https://github.com/FXMisc/RichTextFX) | 原生 JavaFX 编辑器基础，查询 README 为0.11.7；列编辑/IME/转移行为仍须本产品验证 |
| [StageStyle 26](https://openjfx.io/javadoc/26/javafx.graphics/javafx/stage/StageStyle.html) | EXTENDED/HeaderBar 为 preview，独立窗口装饰可用 DECORATED 兜底 |
| [Platform 26](https://openjfx.io/javadoc/26/javafx.graphics/javafx/application/Platform.html) | FX 线程调度和退出策略使用公开接口 |
| [JavaFX CSS](https://openjfx.io/javadoc/26/javafx.graphics/javafx/scene/doc-files/cssref.html) | JavaFX样式体系与浏览器 CSS 不同，Token 按控件属性实现 |
| [JDK25 打包概览](https://docs.oracle.com/en/java/javase/25/jpackage/packaging-overview.html) | 目标 OS 本机打包、自带运行时；JDK25 jpackage 不再默认绑定服务，必须核查服务模块 |
| [Protobuf DynamicMessage](https://protobuf.dev/reference/java/api-docs/com/google/protobuf/DynamicMessage.html) | 动态消息需要 Descriptor；粘贴 `.proto` 的编译须额外实现 |

以上资料支持平台约束；文档的 UI Token、目录、服务契约和性能阈值是项目设计目标。依赖在开发时若需升级，先核实官方发布/兼容信息并更新 ADR，不追随 `latest` 动态解析。

## 6. 基线证据保存

P0 建 `docs/evidence/<日期>-p0/`：environment.md、source-baseline.md、screenshots/、results.md。记录源版本/commit/相关 diff、OS/架构、实际客户区、DPI、语言、主题/风格、字体、样本和启动命令。

为对齐测试复制本产品自有 fixtures，记录源文件/版本、输入/选项、源观察、本版预期与比较方法。CI 不依赖相邻目录、不运行其他产品来临时制造期望结果；升级参考版是一次有意的文档变更。
