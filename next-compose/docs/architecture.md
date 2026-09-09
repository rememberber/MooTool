# Compose 技术架构与实现约束

## 1. 技术路线与版本冻结

采用 Kotlin Multiplatform + **一个 JVM desktop target**。macOS、Windows、Linux 的 Compose Desktop 共用该 JVM 目标，再由各自平台能力适配；不要建立 `macosArm64` Kotlin/Native 目标后假定可直接运行同一套 Desktop UI。

P0 优先验证 Compose Multiplatform 1.12.0 与官方支持的稳定 Kotlin/Gradle 组合，建议先用 JBR/JDK 21 工具链。`org.jetbrains.kotlin.plugin.compose` 与 Kotlin 插件版本必须相同；Gradle JVM、Kotlin jvmTarget、Java toolchain、jpackage JDK 明确对齐。这里是候选，尚未构建验证。[官方版本规则](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)

依赖全部写入 `gradle/libs.versions.toml`，提交 Wrapper 与校验值、必要锁文件/依赖验证元数据；不使用动态 `+`、未锁 SNAPSHOT 或机器私有绝对路径。开发机可设置自有 JDK 路径，但不得提交此机器路径。

Compose 1.12.0 的关联 Material3、Navigation 等并非全部稳定。默认 Foundation + 自有桌面组件，必要的稳定 Material 组件单独验证；不为了菜单/侧栏引入复杂 Android 导航框架。[发布说明](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.0)

正式支持矩阵取 Compose、Skiko、JDK、数据库/native helper 的交集；当前官方平台表未将 Intel Mac 列入 1.12.0 目标，不将其作为已支持平台。若计划兼容 Intel，P0 选择明确支持的版本组合并形成单独实验记录。

## 2. 建议工程结构

以下是待创建的结构；先建一个主应用模块，只有进程隔离或独立测试确有需要时再增加本产品内部模块。

```text
next-compose/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties                     # appVersion 唯一人工版本来源
  gradlew / gradlew.bat
  gradle/wrapper/
  gradle/libs.versions.toml
  composeApp/
    build.gradle.kts
    src/commonMain/kotlin/com/rememberber/mootool/next/compose/
      model/                            # ToolId、设置/会话 DTO、错误、任务状态
      domain/                           # 纯逻辑、格式语义、repository 接口
    src/desktopMain/kotlin/com/rememberber/mootool/next/compose/
      Main.kt
      app/                              # ProductIdentity、AppContainer、启动、注册表
      ui/theme/                         # MooTheme、Token、编辑器 theme adapter
      ui/components/                    # 分栏、工具栏、表格、弹层、树、状态栏
      ui/workbench/                     # 导航、搜索、设置宿主
      features/<feature>/               # Screen、Presenter、State、Actions
      editor/                           # EditorHost、Buffer、事务、Swing 适配
      sessions/                         # 工具会话、窗口 ownership、快照
      services/                         # 文件、HTTP、Git、进程、媒体、worker
      storage/                          # SQLite、设置、Vault、迁移、备份
      platform/                         # macOS / Windows / Linux adapters
    src/desktopMain/resources/          # 本产品资源、翻译、数据库 migration
    src/commonTest/kotlin/
    src/desktopTest/kotlin/
    src/desktopTest/resources/fixtures/
  resources/                            # 安装图标及按 OS/arch 分发的 helper
  scripts/                              # 独立构建、打包检查、证据收集
  docs/evidence/                         # 环境、截图、测试、平台结果
  docs/adr/
  release-notes/
  LICENSE.txt / THIRD_PARTY_NOTICES.md
```

`commonMain` 不导入 java.io、Swing、JDBC、JNA 或 Android SDK。桌面 UI 可以先在 `desktopMain`，不为未来移动端建立无实际用途的 expect/actual 壳。Mac/Windows/Linux 都在 JVM 的平台适配层，不伪造三个 KMP native UI 目标。

依赖方向：UI → Presenter/UseCase → Repository/Service 接口 → desktop 实现。禁止 `@Composable` 内直接操作数据库、Git 或文件；禁止数据层引用 UI。AppContainer 使用显式构造注入即可，暂不引入重型 DI 框架。

## 3. 状态与任务模型

工具注册包含：稳定 ID、分组、三语标题 key、关键词、图标、工厂、历史/收藏能力、开发状态。页面工厂延迟创建；启动时不构造所有编辑器、索引、Git 仓库或硬件扫描服务。

| 状态层 | 内容 | 生命周期 |
| --- | --- | --- |
| AppState | 设置、工具注册、当前工具、最近项 | 进程级，部分持久化 |
| ToolSession | 输入、选项、结果、当前 Tab、任务引用、文档 buffer | 工具会话级，切页/转移窗口不销毁 |
| EditorBuffer | 文本、revision、选区、undo/redo、语法模式 | 文档级，独立于 Composable |
| WindowState | 位置、大小、所承载 session、焦点归还 | 窗口级 |
| ViewState | hover、短暂弹层、展开动画 | 视图局部，不进数据库 |
| PersistedSnapshot | 草稿、文档路径、pane ratio、Tab、必要选区 | 按节流策略保存，不保存 Job/JComponent |

建议用 `StateFlow` 向 UI 暴露不可变状态、`SharedFlow` 或通道表达一次性事件；执行由 Presenter 持有明确 CoroutineScope。`remember` 只用于视图细节，不能作为唯一业务存储。避免将 5 MiB String 在每帧复制到多个 UI state 和数据库字段。

任务状态统一为 Idle、Running(taskId, stage, progress?)、Succeeded、Failed(error)、Cancelled。输入每次编辑增 revision；结果只在 taskId 与输入 revision 符合当前会话时提交。自动分析用 debounce + latest-wins；已发 HTTP、进程或文件任务由显式句柄取消。

错误包含稳定 code、本地化 message key、可重试标记及必要细节，如 InvalidInput、ParseError(line,column)、PermissionDenied、Conflict、MissingRuntime、Unsupported、IO、Network、LimitExceeded。日志隐藏凭据，界面保留可修正输入。

## 4. 线程、生命周期与性能

- Compose/AWT/Swing 视图创建与变更限定相应 UI/EDT；Swing 文档和组件操作统一通过经验证的 EDT dispatcher。不能在 UI 线程 `runBlocking` 或同步等待 `invokeAndWait` 形成死锁。
- 文件、SQLite、网络、Git 在 IO 执行域；解析、diff、压缩、PDF 在有界计算执行域。协调阶段用协程，不能把协程等同“自动后台”。
- 数据库单写队列，设置原子写，文档保存按文件串行；协程取消应传播 `CancellationException`，不要吞掉后改为成功。
- 纯 CPU 循环定期检查取消；仅 `withTimeout` 不能停止不响应中断的 JVM regex/第三方库。此类操作用可终止的本产品 worker JVM 进程。
- 进程输出、日志、历史预览、图片缓存均有上限；进程 stdout/stderr 同时持续读取，避免管道阻塞。
- 失活工具暂停时钟/硬件轮询等非必要工作；HTTP 和代码运行按用户行为保持或取消；切工具不无意重启任务。
- 使用 `DisposableEffect`/显式 close 释放监听器、watcher、弹层及窗口宿主；不要在 recomposition 每次注册回调。
- 首帧展示壳，后台恢复数据，打开某工具时才初始化它；每项初始化失败可重试。Java 的启动职责分离可作为思路，不复制其全局单例。

## 5. 编辑器方案：先做验收实验

### 5.1 EditorHost 合约

EditorHost 必须支持文本/语言、只读、字体主题、软换行、行号、语法、选区、滚动、聚焦、查找/替换、事务编辑、undo/redo、状态快照、图片/文件拖放转交。JSON、随手记、HTTP 正文、代码运行和 Diff 共用适配规范，但各自有独立 Buffer。

文本以 revision 驱动增量 edit；格式化/替换用一次 compound transaction。自动保存只观察 dirty/revision，不调用 `setText` 刷新全文。历史恢复是一项显式编辑动作，不能暗中混入旧 undo 事务。

本项目默认不要求 undo 跨进程重启，但要求切页、开关预览、主题切换、分离/收回后仍然保留 undo。模型 offset 可以统一 UTF-16，鼠标可视列/字素与 Tab 展开需要明确转换，不能用字符数代替屏幕坐标。

### 5.2 首选实验：RSyntaxTextArea + SwingPanel

RSTA 作为第三方依赖由本产品自带，使用自有 theme、行号、滚动条和查找 UI，不加载 Java 版的 Form/全局管理器。语法高亮/折叠能力可直接评估，矩形选择、多光标和图文粘贴仍需本产品补足。[RSTA 上游](https://github.com/bobbylight/RSyntaxTextArea)

`factory` 创建宿主组件，更新阶段只应用变化的属性；组件与 Document 的所有权明确，不因 Composable 重建丢掉文档状态。设置主题通过 adapter 映射 Compose Token，保留字体回退；现有 Java CJK painter 可作为一次性参考，不能直接依赖根源文件。

SwingPanel 默认处于 Compose 内容前方；菜单、命令搜索、对话框或面板覆盖可能被编辑器遮挡。P0 默认选择不会重叠的分栏、真实独立 Dialog/Popup 窗口，或临时撤下被遮挡的 host 并在关闭后恢复焦点；这些路径也必须实测。experimental blending 仅做实验，不能未经验证当成完整解决方案。[官方互操作约束](https://kotlinlang.org/docs/multiplatform/compose-desktop-swing-interoperability.html)

### 5.3 列编辑不能省略

矩形选择基于可视行列处理；明确软换行开启时使用逻辑行还是视觉行。本产品建议列编辑暂时按逻辑行并明确提示，列单位按等宽网格，Tab 展开遵循 tabSize，宽字符按实际布局映射，不能截断 surrogate/组合字符。

需要实现多行输入、删除、粘贴；每次动作统一 undo；短行补齐策略与换行分发策略固定测试。多行块与单行粘贴分别验证。普通选择和 IME composition 不受列模式破坏。

实验若无法满足这些条件，替代方案为本产品自有 Compose 增量编辑器或其他经验证成熟组件；由 ADR 明确成本和覆盖度。WebView/CodeMirror 只能作为显式备选，需自带资源、加载/通信/安全边界和包体证据；不默认引入整个 Electron/JCEF，也不悄悄改成网页壳。

### 5.4 Markdown

解析为结构化 AST，Compose 预览渲染标题、列表、表格、任务、代码、链接、相对附件；大文档异步解析并复用分块。禁止执行正文脚本/任意 HTML；外部图片加载必须明确策略，默认本地文档不无提示发起外部请求。

若选 HTML renderer，需验证表格、代码、图片的实际覆盖与渲染包体；Swing JEditorPane 的旧 HTML 支持不能直接等同完整浏览器预览。附件解析统一经过 Vault 路径边界。

## 6. 多窗口和会话转移

使用单一应用进程的 Window 集合。SessionManager 持有会话，WindowManager 只管理宿主。默认每个 Tool ID 一份活动会话，笔记/JSON 会话内部可有多个 Buffer；不把“分离”做成新建一个空工具。

转移状态：`Docked → Transferring → Detached → Transferring → Docked`。转移过程中冻结重复命令，旧宿主停止接收事件后才挂新宿主；失败回滚到旧宿主。捕获未提交 IME 状态并在安全时点转移，不能丢输入。

同一个 JComponent 不可同时加入两个父容器。可以转移同一 host，或在新窗口创建 host 并绑定持久 Document/UndoManager；无论方案，生命周期和旧监听释放要通过测试。不要序列化 JComponent，也不要为了序列化文本重置 undo。

- 关闭独立工具窗口默认收回主窗口；所有权转移结束才 dispose 宿主。
- 主窗口 hide 不销毁会话；主程序 quit 统一检查未保存数据、在途任务并关闭服务。
- 退出失败（保存冲突/磁盘满）保留窗口与恢复入口，不能仍然 exitApplication。
- 设置广播至所有窗口，主题/字体不重建 Buffer；全局动作只路由活动窗口。
- 恢复布局只恢复位置/会话，不能重启上次 HTTP 请求、命令、提权或 Git push。

## 7. JVM 能力与算法选型

下表是首选实现方向，精确 artifact/version 在对应阶段核验上游后锁定。不整体照搬 `pom.xml`，尤其不为一个算法拉入整个 Java UI、旧数据库层或不适合本产品分发的许可依赖。

| 能力 | 首选方向 | 关键验收/边界 |
| --- | --- | --- |
| 设置/DTO | kotlinx.serialization | schema v1、默认值、未知/损坏数据、原子写 |
| DB | SQLite JDBC + 显式 SQL repository | 三平台 native 解包、WAL、事务、迁移与备份 |
| 备份/恢复 | 自有 `BackupEngine` zip + SHA-256 清单 | 不含缓存/日志/凭据；恢复前 `pre-restore` 快照；拒绝路径穿越。见 DIFF-022 |
| HTTP | OkHttp **4.12.0** + 自有 `HttpEngine` | 冻结 GET/表单语义；重复 Header 按多值发送；10 MiB 解压上限；可取消。见 DIFF-018 |
| 翻译 | OkHttp **4.12.0** + 自有 `TranslationEngine` | Google 1800/并发 3 保序；Bing 全文 POST + 会话缓存；15s 总超时；冷却 fallback。单词本/历史 JSON。见 DIFF-019 |
| JSON | Jackson 流式/token/树 + 自有适配 | 重复 Key、数字字面量、深度上限；普通 Map 不保留重复 Key |
| JSONPath | Jayway 候选 + Electron 样本适配 | filter/union/slice/escape；禁止执行任意 JS |
| XML / YAML | JAXP + SnakeYAML 2.3 SafeConstructor | 禁用外部实体、限制深度/别名膨胀；不声明注释无损；配置转换见 DIFF-006 |
| Diff | java-diff-utils 或同类成熟实现 | 行与字符差异、统一模式、重复行、偏移 |
| Java 格式化 | JavaParser 3.26.4 PrettyPrinter | 语法处理、错误不改原文；无外部 Node 依赖；见 DIFF-005 |
| HTML/XML/Nginx | Jsoup + JAXP 自写缩进 + Electron Nginx tokenizer | 保留字符串、注释、文本节点语义，幂等 |
| Cron | cron-utils / Quartz 语义适配器候选 | 6/7 字段、周编号、?、L/#、year、IANA zone/DST |
| 正则 | Java Pattern 为首选引擎 | 显示 Java 语义差异；独立 worker 超时/终止；不能宣称完全 JS 兼容 |
| UA | 维护中的 UA 规则库 | 浏览器/OS/设备/bot 样本与版本记录 |
| 加密 | JCA/JCE + Bouncy Castle 1.80 | 精确 key/模式/padding/字节/DER/SM2 参数，跨实现验签；对称路径见 DIFF-008 |
| QR | ZXing 3.5.4 | PNG/剪贴板真回读、Logo/纠错、坏图；历史见 DIFF-009 |
| 调色板 | 自有 ColorEngine + AWT Robot 冻结截图 | 主题/标准色 SHA-256、五运算、Shift 选对比色；权限/全黑拒绝见 DIFF-010 |
| 留言板 | 自有 MessageBoardEngine + OS 唤醒进程 | 80 字 UTF-16、8 预设/6 主题、自动适配字号；演示唤醒见 DIFF-011 |
| 图像 | ImageIO/Java2D + 自有 `data/images` 图片库 | EXIF 方向、alpha 棋盘格、1600 万像素上限、压缩/水印、区域截图。见 DIFF-013 |
| SVG | 本产品内嵌 ImageTracer.java **1.1.2**（Unlicense） | poster/photo/bw 参数映射到 ltres/pathomit 等；输出含 path，禁止嵌入 bitmap。见 DIFF-013 |
| PDF | PDFBox **3.0.4** | 真页面 importPage；页码顺序/去重对齐 Electron；加密 PDF 拒绝；取消删除本批半成品。见 DIFF-012 |
| 系统信息 | OSHI **6.8.2** | 真机采集；序列号默认遮蔽；OS 与本产品 JVM 信息分区；切走工具取消采集。见 DIFF-014 |
| 网络/IP | 自有 `NetEngine` + 平台进程 argv | IPv4↔Long fixture；`InetAddress` DNS；ping/ifconfig/netstat 可取消；WHOIS 端口 43。见 DIFF-015 |
| 环境变量 | 自有 `EnvEngine` + 本产品 `data/environment` | 用户/系统文件备份后写入；进程/JVM 只读；Unix 钩子使用 Compose 标记。见 DIFF-016 |
| Host | 自有 `HostEngine` + `data/hosts/profiles.json` | 保存方案不改系统文件；应用前 diff/备份/指纹冲突；提权失败保持原 hosts。见 DIFF-017 |
| 代码运行 | 自有 `CodeRunEngine` + `ProcessBuilder` argv | Java 源文件模式；白名单环境；1 MiB/2 MiB 上限；ProcessHandle 杀树。见 DIFF-020 |
| 随手记 | 自有 `NoteVault` + `QuickReplaceEngine` | 默认 `data/vaults/quick-note`；24 项替换对齐 Electron 样本。见 DIFF-021 |
| Git | 本产品 GitService 封装外部 Git CLI，缺失引导配置；JGit 可作验证后的替代 | 仓库锁、冲突、stash/操作状态、凭据、Git 不存在时仍可记笔记 |
| 时间/计算 | java.time、BigInteger/BigDecimal、自有表达式 AST | 时区、DST、精度、算符、溢出；不 eval 用户文本 |

## 8. 动态 Protobuf 的具体路线

首选随本产品分发对应 OS/arch 的 `protoc` 4.29.3（构建时从 Maven 分类器复制到 classpath `helpers/`），启动受控子进程输出 descriptor set，再由 JVM `DynamicMessage` 编解码。这样用户临时粘贴 `.proto` 不需要重新编译应用，也不要求用户自己安装 protoc。JSON 适配见 [DIFF-007](diff/007-protobuf-jsonformat.md)。

仅允许临时工作目录及用户明确选择的 import roots；禁止通过 import 跳出授权根、自动联网下载未知 schema。超时、大小、递归深度、编译 stderr 和临时文件清理统一处理。well-known types 需要随包带所需定义或明确支持策略。

`DynamicMessage` 依赖 Descriptor，不能用它冒充 proto 源码解析器。[官方 API](https://protobuf.dev/reference/java/api-docs/com/google/protobuf/DynamicMessage.html) Java 现有 `ProtoBufUtil` 的轻量解析可参考，但必须通过 nested/map/oneof/enum/import 等样本后才能成为替代。

JSON 适配固定字段原名、默认字段、64 位字符串、enum 名和 bytes Base64；不要直接套 `JsonFormat` 默认行为就声称与 protobufjs 相同。部分输入协议允许差异时在工具内说明，并留可重复 fixtures。

## 9. 进程与不可信计算

`ProcessService` 输入为可执行文件路径、argv 列表、工作目录、限定环境、时间/输出限制，输出为流与退出状态；不能拼用户字符串执行 shell。cURL 导入是解析文本，不是运行该命令。

本产品自己的 regex/格式化 worker 可以使用随包 JVM 的 java 启动独立 Main；只提供受限 IPC/stdio 协议，指定 `-Xmx`、超时和最大输入输出。需要结束整个进程树：Unix process group/后代追踪、Windows Job Object 或经验证等效实现，不能只取消 UI Job。

用户代码运行属于本机代码执行，界面显示运行环境与目录；不称为安全沙箱。Java/Groovy/Python/Node 工具由显式外部 runtime 启动，JRE 精简包不保证有 javac。应用本身的 JVM 和用户选择的 Java 执行环境分别管理。

## 10. API 示例边界

本文未提供可直接声称构建通过的 Gradle 模板。P0 从当前官方兼容配置建立工程，再将真实命令写入 README。若使用 `jvm("desktop")`，约定任务入口为 `:composeApp:run`、`:composeApp:desktopTest` 与 `:composeApp:createDistributable`，需先以 `:composeApp:tasks --all` 核实。

安装包必须执行 `runDistributable` 或直接启动镜像验证，因为完整 JDK 下 `run` 通过不能证明 jlink 后无缺少模块。不要只运行 JVM 单测而跳过 Swing/Compose/安装包行为。
