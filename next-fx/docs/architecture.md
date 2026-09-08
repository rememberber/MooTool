# JavaFX 技术架构

> 本文定义后续实现，类名、脚本和接口是拟建契约；当前没有可编译工程。版本依据见 [基线](baseline.md)。

## 1. 工程与依赖

默认 Java 25、OpenJFX 26.0.2、Maven Wrapper 3.9.16，编译 `release=25`；OpenJDK 的 vendor/patch 在 P0 固定。首版单 Maven 工程、单应用进程，多 Stage；不继承根 POM，也不读取根 `lib/`、`target/`、`jdks/`。

先用 classpath 放应用/普通三方库，JavaFX 使用明确的 module path；P0 验证 launcher、FXML（若实际使用）、原生库和打包。不要为了全量 JPMS 把自动模块硬塞入 jlink，也不要把所有 jar 合并成破坏服务发现的 fat jar。需要全模块化时单独 ADR。

JavaFX 模块按需使用：`javafx.controls`、`javafx.graphics`、`javafx.base`；Markdown/备选编辑器需要 `javafx.web`；ImageIO/AWT 托盘等需要 `java.desktop`，SwingFXUtils 才引入 `javafx.swing`。公开模块/API 优先；不使用 `com.sun.javafx.*` 或大范围 `--add-opens` 作为默认方案。JDK原生访问选项由实际模块/库清单决定，记录用途，不全局屏蔽警告。

采用程序化 JavaFX Views + CSS，构造器显式注入小型服务。FXML 可用于固定表单，但不能一半视图状态在 FXML Controller、一半在全局单例；不要引入 Swing 主壳、Spring 或复制原 Java UI 初始化链。

```text
next-fx/
  pom.xml                       独立版本和精确依赖/插件
  mvnw / mvnw.cmd / .mvn/       自有 Wrapper、校验及配置
  src/main/java/com/rememberber/mootool/nextfx/
    Launcher.java              不继承 Application 的启动入口
    app/                       Application、装配、身份、注册表、退出协调
    domain/                    无 JavaFX 的值对象、算法、稳定 ToolId
    application/               用例服务、任务、取消、文档/会话协调
    infrastructure/            SQLite、配置、文件、HTTP、Git、外部进程
    platform/                  macOS / Windows / Linux 能力适配
    ui/shell/                  主窗口、导航、设置、命令搜索
    ui/components/             按钮、分栏、列表、错误和进度
    ui/editor/                 EditorHost、编辑命令、语法/查找/选区
    features/<tool>/           ViewModel、View、工具专有协调
    worker/                    Regex/耗时媒体等受控子进程入口
  src/main/resources/
    styles/                    tokens、base、controls、themes、tools
    i18n/                      zh_CN / en_US / ja_JP
    icons/ fonts/              自有资源、许可记录
    db/migrations/             仅本产品数据库版本
    web/                       若选用，仅本地预览/编辑器制品
  src/test/java/               单元和服务集成
  src/test/resources/fixtures/ 独立固定样本
  scripts/                    app-image、安装包、验证
  packaging/                  各OS身份、图标、模块清单、打包锁定信息
  docs/adr/ docs/evidence/ release-notes/
```

目录按实际功能逐步建立，避免生成数百空类。`domain` 不引用 UI，View 不直接读 SQLite/启动进程；应用服务通过接口使用 repository/平台适配，`app` 负责装配。

## 2. 状态、命令和线程

三层状态：`AppState` 管理设置/导航/窗口；`ToolSession` 管理每工具输入、选项、运行任务及结果；`DocumentSession` 管理文档 ID、revision、dirty、编辑器实例/undo、路径及保存状态。编辑文字不以所有权不明的双向绑定在三个地方各存一份。

持久化只保存可恢复的 DTO，不保存 Node、Property、Future、连接或任意 Java 序列化。重启恢复文本/选项/选区/滚动；进程内切页和转移还要保留 undo。崩溃恢复 journal 和普通历史是不同用途。

统一动作注册包含 commandId、显示文字、快捷键、可用条件、scope 和执行器；按钮、菜单、键盘走同一个用例，避免键盘绕过忙态/权限/校验。服务错误模型：`code / userMessageKey / details / field / line / column / retryable / causeId`，日志有脱敏 correlationId，UI 显示可理解的错误。

| 工作 | 执行边界 | 约束 |
| --- | --- | --- |
| Scene Graph、Property 与用户交互 | FX Application Thread | 只做轻量状态应用；不在监听器里读文件/数据库 |
| I/O、HTTP、外部进程读取 | 虚拟线程 executor 或有界 I/O executor | 有并发上限、总超时/读取超时、取消；虚拟线程不是无限资源 |
| 格式化、diff、索引等CPU任务 | 有界 CPU executor | 默认最多 `max(1, min(4, cores-1))` 个并发，可基准调整 |
| SQLite 写操作 | 单写者队列 | 事务短；读连接与写连接不随意跨线程共享 |
| 自动保存、watcher | 每文档/每仓库串行队列 | revision/hash 对照，防止旧内容后写覆盖 |
| Java Pattern、可能不响应中断的处理器 | worker子进程 | deadline后终止整个任务进程，不能仅 cancel Future |

后台工作从 FX 线程取得不可变快照，结果回到 FX 时比较 sessionId、inputRevision、requestId、disposed。过期结果丢弃；不在 FX 线程 `get()/join()/waitFor()`。进度更新合并至约20–30Hz，避免大量 `Platform.runLater` 堵塞队列。JavaFX `Task` 一次执行创建一个实例，重跑不复用完成的 Task。

忙态可取消、可继续编辑、不会清空原输入。每个任务持有资源范围；退出/取消关闭 stream、HTTP请求、文件句柄、watcher、executor订阅及子进程。不能靠窗口不可见判断全部任务应该杀掉：正在下载/写文件要由退出协调器处理。

## 3. 原生编辑器路线与 P0 门槛

首选 RichTextFX 0.11.7 `CodeArea` + 虚拟滚动容器，封装成 `EditorHost`。不把 JavaFX 的 incubator RichTextArea/CodeArea 当作已稳定且无条件兼容的主依赖；若要选择它们，另做带版本范围的 ADR。

拟建接口的语义（不是可直接复制的完整源码）：

```java
interface EditorHost extends AutoCloseable {
    Node view();
    EditorSnapshot snapshot();
    void openDocument(DocumentSession document);
    void applyEdits(EditTransaction edits); // revision校验、一次命令一次undo
    void execute(EditorCommand command);
    void setTheme(EditorTheme theme);
    void setLanguage(EditorLanguage language);
    Subscription onChange(Consumer<EditorChange> listener);
    void focusEditor();
    void close();
}
```

`EditorSnapshot` 包括文档ID/revision、文本或快照句柄、caret/anchor/多选区、首可见行/水平偏移、折叠及换行设置。主题/字体/高亮变化不算内容修改，不重置 undo；程序更新必须带 origin，防止“更新 View → ViewModel → 再 setText”循环。

所有正式编辑页需要行号、语法高亮、缩进、括号/错误定位、复制粘贴、撤销重做、查找替换、字体/字号、软换行、选区和滚动恢复。折叠/多光标/矩形编辑不是仅引入依赖即可完成，按功能清单逐项适配。

查找支持普通/大小写/全词/正则、计数、前后定位、替换当前/全部；使用持久 mark 层避免重设文本，匹配上限和正则超时与独立 worker 一致。按 UTF-16 offset 与代码点/字素边界转换；列宽按视觉列（Tab 展开）定义，不用 `String.length` 当屏幕宽。

中文 IME：composition 中不自动格式化、不替换全文、不重绘打断候选、不重复写入未提交字；同时验证英文 dead key、emoji、组合附加符、CJK 字体回退、候选窗位置。输入法验收必须在真实桌面完成。

列编辑：矩形选择、跨短行输入/删除/粘贴、行尾补空格、选区方向、Tab/emoji、软换行的逻辑行语义、一次事务一次撤销。默认矩形操作按逻辑行/视觉列，软换行时可提示切换为不换行；明确操作结果，不能悄悄破坏原换行设置。

高亮和 lint 按 revision 异步执行，200–300ms防抖；不每键重解析数 MiB。大文件模式先呈现可编辑文本再补高亮，限制昂贵分析；通知用户具体限制，禁止静默截断保存内容。

P0 必测：中文/日文输入；500KB/3MiB JSON；5MiB随手记；十万行滚动；单行1MiB；查找1000匹配；列编辑20行；编辑→undo→分离/收回→redo；50次窗口转移；字体切换；对话框层级。输出时间、heap/RSS和录像/截图。

若原生方案有门槛失败：先做有界修复并留测试；核心能力仍无法满足时，ADR 比较原生路线与 **仅编辑器使用离线 CodeMirror 的 WebView**。允许选择后者，但不能悄悄改成全应用网页或删掉需求。

## 4. WebView 的明确边界

Markdown 预览默认允许使用 WebView，本地 Markdown parser 输出后用 allowlist 清理 HTML，JavaScript 默认关闭；外链交系统浏览器，本地图片仅通过受控 Vault/附件路径访问。阻止 `javascript:`、意外远程资源加载和路径逃逸；预览不能获得文件系统/进程桥。

如果 P0 选择 WebView 编辑器：只加载本产品打包的离线 HTML/JS/CSS，不加载 CDN，不导航任意网页，不提供通用 `readFile/exec/eval` bridge。协议只允许文档编辑/选区/主题等结构化命令，参数校验和大小限制；Java对象桥持有强引用、按实例清理，消息带documentId/revision。用户文字用序列化数据传递，不拼入可执行 JS。

Node最多是构建该局部资产的开发依赖，成品不依赖系统 Node 或 Electron 的 `node_modules`。若采用，提交独立 lockfile、源码和可复现资源构建命令；首版普通构建可使用已提交并核验的资源制品。跨引擎语法、IME、焦点/菜单、DPI、粘贴图片、undo、转移和内存都要再次验收。

## 5. 多窗口与生命周期

Electron 的不销毁 WebContentsView 在 FX 中对应 **同一 ToolSession、同一活动工具 Node/编辑器** 在 dock 容器与独立 Stage 之间转移。Node 不能同时有两个父容器；不能在主窗口和独立窗口各创建一个编辑器然后靠文本镜像同步。

`ToolWindowCoordinator` 状态：`DOCKED / DETACHING / DETACHED / DOCKING / DISPOSED`。所有转移在 FX线程串行：禁止重复点击 → 关闭 transient popup/记录焦点 → 从旧容器移除 → 挂新容器 → 注册新 Scene 快捷键和主题 → 完成布局后恢复选区/焦点 → 解除旧绑定。只在真正销毁时 dispose，不能在窗口转移时 close 文档。

每工具最多一个独立 Stage。首页不分离；点击已分离工具显示“前置/收回”占位。关闭工具窗口默认收回，主窗口关闭遵循 ask/hide/quit。工具 Stage 不设成随主窗口最小化的 modal/utility owner；文件对话框、菜单和错误弹窗则归当前实际宿主窗口。

独立窗口是普通可操作窗口，标题 `MooTool Next FX — 工具名`，显示尺寸状态独立保存。多屏恢复把丢失屏幕上的bounds夹回可见工作区；DPI与逻辑坐标单独保存/计算。转移不重启 HTTP/Git/代码执行，也不清空 undo。

应用生命周期设独立退出协调器：可隐藏时 `implicitExit=false`；无托盘/Dock恢复通道不得制造不可找回的后台应用。真正 Quit 依次禁止新任务、处理dirty/保存失败、停止任务/worker、关闭数据库/平台持有者，再 `Platform.exit()`。取消退出则回到完整工作状态。

## 6. 窗口装饰和设计实现

P0/P1 默认 `DECORATED` 确保系统缩放、移动、系统菜单与可访问性。最终尽量对齐一体化工具栏；`WindowChrome` 可实验 `EXTENDED + HeaderBar`，必须按平台检测并能回退。该 API 在FX26仍为preview；不要把Java语言`--enable-preview`与这个库API状态混为一谈。[官方定义](https://openjfx.io/javadoc/26/javafx.graphics/javafx/stage/StageStyle.html)

不在所有 OS 画假的 macOS 红黄绿，不为几像素差异引入脆弱 JNI。若使用无边框，必须实现缩放边缘、拖拽排除、双击最大化、系统快捷键、阴影、高DPI和多屏，还需实际验收；否则记录保留系统标题栏的差异。

颜色/间距等见 [UI](ui-spec.md)。Scene、独立 Stage、Popup/ContextMenu/DialogPane都要应用同一theme snapshot；仅给主Scene换CSS不能认为全应用已切换。RichTextFX token styles与WebView CSS若存在，均由一个ThemeSnapshot派生。

## 7. 算法与库选择契约

下表是候选路线，不是已验证的依赖锁。P0/P3/P4实施时检查官方稳定版本、许可、原生制品及JDK25兼容，固定在本产品POM和ADR；不因根Maven已有某库就继承。

| 能力 | 首选路线 | 不可忽略的语义 |
| --- | --- | --- |
| JSON/XML/JavaBean | Jackson streaming/tree、JavaParser；JSONPath候选Jayway | streaming先检测重复key；BigInteger/BigDecimal/原始字面量；XML禁外部实体；JSONPath方言差异 |
| YAML/Properties | SnakeYAML安全构造 + 自有path模型 | key冲突、数组、显式类型、别名深度；不实例化任意Java类型 |
| Diff | java-diff-utils + 字符细分与Unified适配 | 不按同行号逐个字符串比较；重复行、末尾换行和偏移正确 |
| 格式化 | JavaParser/成熟Java formatter；DOM或parser处理XML/HTML；自有Nginx tokenizer | 字符串和注释不可改义；每语言有幂等/错误样本 |
| Crypto | JCA/JCE + Bouncy Castle | 具体mode/padding/编码/SM2参数，跨实现互通；禁止provider默认猜测 |
| Protobuf | 锁定平台protoc → FileDescriptorSet → DynamicMessage/JSON适配 | protoc随包或本产品受控安装，版本/架构/hash固定；import路径allowlist、循环/超时；不是固定生成几个message |
| Cron | cron-utils + 明确的兼容解析/渲染 | 源6/7字段，日周/year/DST差异；未知语法报错 |
| Regex | Java Pattern 独立worker | JS语义差异可见；灾难性回溯可终止；global由遍历实现 |
| QR/条码 | ZXing | 图片像素/纠错/logo、剪贴板和往返解码 |
| 图片 | ImageIO/BufferedImage；透明/EXIF适配；自有或vendored ImageTracer Java | SVG是path；替换算法要映射真实参数，记录视觉差异；复制第三方源码保留许可 |
| PDF | PDFBox候选，操作真实页面 | 页码顺序/范围、特殊对象、加密、资源限制；不默认复制旧iText依赖 |
| 系统 | OSHI/JNA + 受限平台适配 | unavailable与0不同；刷新/权限/平台返回可验证 |
| HTTP | JDK HttpClient 或 OkHttp（二选一在ADR锁定） | 行列表保持重复参数；流式10MiB上限、取消、解压、代理/TLS、cookie语义 |
| Git | 首选系统Git CLI适配；无Git时本地Vault继续可用 | argv传参、超时、凭据脱敏、冲突可继续/中止；不能默认push/强制reset |
| Markdown | flexmark等Java parser + 受控预览 | 相对附件、HTML清理、离线显示 |
| UA | 维护的Java UA解析器/规则库 | 用Electron样本核对名称/字段，不宣称不同规则库完全一致 |

## 8. 用户代码与内部 worker

用户代码用明确配置的外部 Java/Groovy/Python/Node 路径；应用自带runtime仅保障应用启动，不承诺包含javac或全部运行环境。源程序及参数由 `ProcessBuilder(List<String>)` 传入，工作目录真实生效，stdout/stderr独立持续排空，编码/退出码/耗时可见。

内部regex/media worker与用户代码分开：打包runtime保留`bin/java`或提供明确worker launcher；验证`java.home`定位的自身runtime，而非PATH里的另一个Java。worker使用明确entrypoint、限额和schema，不允许任意类执行请求。以应用权限运行的子进程**不是安全沙箱**，界面只运行用户明确提交的代码，不将文档预览变成执行入口。

停止采用进程组/Windows Job Object等平台策略，ProcessHandle后代遍历只能作补充，测试快速派生子进程及超时强杀。累计源码1MiB、输出2MiB，输出上限后持续排空/丢弃并标记截断；不能因停止读取导致子进程死锁。默认用户代码超时60秒（本产品设计值，可设置），停止后清理临时目录，失败报告不泄漏凭据。

## 9. 验证和性能

单元测试使用JUnit，UI自动化候选TestFX，实际锁定兼容组合；UI测试独立Maven profile，测试启动一次Toolkit，禁止多个测试同时争用全局FX状态。[TestFX项目](https://github.com/TestFX/TestFX)

Linux首选Xvfb虚拟显示作CI交互；FX26 headless可做节点快照/逻辑实验，不能替代系统剪贴板、IME、托盘、真实窗口或读屏测试。正式平台在各自桌面环境验收，结果见 [验收](acceptance.md)。

先量测再优化：懒初始化工具/图标/字体、复用会话、表格/树/编辑器虚拟化、缓存缩略图、取消过期任务、避免每键重建节点。JFR/heap dump留在本产品证据目录且清理用户正文和凭据；不以无限增大Xmx掩盖泄漏。
