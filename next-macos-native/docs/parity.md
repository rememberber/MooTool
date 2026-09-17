# 与 next Electron 的功能对齐

参照 `next/src/app/toolRegistry.ts` 的 26 个入口。这里记录当前可运行的范围；“尚未覆盖 / 明确边界”列描述**与 Electron 的能力差距**（含 0.8 有意不交付项），不代表原生版未完成迁移。0.8 对齐口径：**功能入口、面板顺序与主要操作语义**与 Electron 一致；各行左列「当前原生实现」为已交付范围。

功能与布局均以 Electron 对应页面为基准，使用 macOS 原生控件保留面板顺序和主要操作语义。独立实现不意味着任意改变既有工作流程。

| 工具 | 当前原生实现 | 尚未覆盖 / 明确边界 |
| --- | --- | --- |
| 首页 | 品牌、快捷搜索、常用工具（工具名随语言）、贡献者、赞赏、源码链接、其他作品、项目链接；主要区块标题与导语支持 zh-CN / en-US / ja-JP；其他作品描述与「提交问题」链接随语言切换 | 与 Electron 完全一致的动效 |
| 随手记 | 文档库、正文检索/排序、批量导入、自动保存、按文档恢复光标/滚动与字体/字号/颜色/行距/换行/语法；**文档库侧栏**与**编辑区**工具栏/查找/快速替换支持三语；Markdown **预览**（代码块/任务列表/图片附件提示与缩放 sheet）与**图片插入**状态/错误随 `language` 三语（`quickNote.preview.*` / `quickNote.image.*` / `quickNote.status.insertedImages` 等）；编辑设置/查找限额与**图片附件**导入、清单、导出、备份恢复等 Core 校验错误随 `language` 三语（`quickNote.error.*` / `attachment.error.*` / `backup.error.attachment*`）；编辑/分栏/预览保留撤销；24 项选区/全文快速替换、JavaScript 查找替换、列表前缀、JSON/XML 格式化；Markdown 表格对齐、任务/有序/无序列表、标题/引用/代码块/行内预览；图片选择/粘贴/多图拖入、缩放/长图预览、附件导出及备份恢复；保存时镜像正文与引用的 `attachments/*` 到 `quick-notes/`（可纳入 Git）、Finder 定位、**Vault Git 面板**（`git.*` 三语，含 Diff、丢弃、合并/变基中止与继续、冲突 ours/theirs）；FSEvents 监听、外部增删改自动合并或冲突提示、「从磁盘刷新」；设置中可开启自动检查点（含 push）与定时自动 Pull | 外部拉取的 Electron 风格附件文件名与磁盘双向同步；其余语法格式化、列编辑及完整编辑装饰；远端图片、HTML、脚注与完整嵌套块尚未支持；GIF/WebP 预览首帧。细节见 [随手记工作区与边界](quick-note-workspace.md) |
| 文本对比 | 对照 Electron 的比较/清空/交换/复制、上一处/下一处、忽略空白、行与字符/仅字符/仅行高亮、左右/统一视图与历史布局；双编辑器同步滚动、自动比较、UTF-16 字符范围、三行上下文统一补丁、增删改统计、草稿与视图状态恢复；**工作区**标题、工具栏、分栏标签与状态栏支持三语；输入超限等**引擎错误**随 `language` 三语（`textDiff.error.*`） | 原生 NSTextView 高亮覆盖文本行与字符，不含 Monaco 完整边缘标记；总输入限 50 万 UTF-16 单元、行数乘积限 1000 万，单对长行超过 4000 单元改用行级高亮。详见 [文本对比工作区](text-diff-workspace.md) |
| 格式化 | 与 Electron 的文本 / 文件两个标签及顶部类型、缩进、格式化、历史、复制、导出、清空布局对应；Nginx、Java、XML、HTML；原生版额外保留 JSON 兼容旧草稿。文本原位格式化且可撤销；文件原文与只读结果双栏、UTF-8 导入、新文件导出；历史及本产品工作区恢复；**工作区**标题、分段、工具栏与文件双栏标签支持三语；宿主/`ReformatTools` 边界错误（大小/超时/类型/Nginx 嵌套等）与撤销动作名随 `language` 三语（`reformat.error.*` / `reformat.undo.*`） | 仅支持 Electron 格式化页的四种类型，JavaScript、SQL 属于其他编辑能力范围；使用原生文本编辑器，不含 Monaco 完整装饰。输入限 2 MB，解析器 2.8 秒超时；文件导入为副本，不监听或回写原文件；Prettier/Java/XML 解析器抛出的语法细节仍可能为英文。详见 [格式化工作区与边界](reformat-workspace.md) |
| JSON | 文档库、主编辑器、可折叠检查器；可撤销格式化/压缩/排序、忽略大小写和重复键检测；JavaScript 查找替换；JSONPath 筛选/递归/通配符/切片/联合查询、严格 JSON Pointer、路径选择与值预览；JSON/XML、JavaBean 转换、键值互换和转义；结果弹窗/使用结果、字体/换行、自动保存与编辑位置恢复；**主工作区**工具栏/检查器/查找条/结果与路径弹窗及 JSON 专用历史 sheet 支持三语；**结构树/路径选择面板**（`JSONTreePane`）标题、空状态、统计、底栏与右键菜单支持三语；**文档库侧栏**（搜索/排序/工具栏/菜单/弹窗）支持三语；保存时镜像到 `json-vault/`、Finder 定位、**Vault Git 面板**（`git.*` 三语，含 Diff、丢弃、合并/变基中止与继续、冲突 ours/theirs）；FSEvents 监听、外部增删改自动合并或冲突提示、「从磁盘刷新」；设置中可开启自动检查点（含 push）与定时自动 Pull | Monaco 全部编辑装饰/折叠/快捷操作；JSON 数字使用 JavaScript Number；JavaBean 为字段声明和示例类型推断；XML 不接受 DTD；JSON worker 校验与 **JSONEngine 宿主侧**错误支持三语（请求携带 `language`）；XML 解析器抛出的 `msg` 仍可能为英文。具体边界见 [JSON 引擎说明](json-engine.md) |
| 代码运行 | 本机 Python、Node、Swift、Java source-file、Groovy；独立临时文件；环境变量；超时、输出上限；工具栏提示与常见校验错误支持三语 | 运行时安装、交互式 stdin、调试、长期会话；运行用户代码具有当前用户权限 |
| 配置转换 | Yams 解析 YAML；YAML/JSON/Properties 互转；转换模式 Picker 支持三语（稳定键 `yamlToJson` 等）；Properties 边界与格式错误随 `language` 三语（`config.error.*`） | Properties 支持点分层级及标量；不支持续行、转义键/值、值首尾空白、数组/null 等无法无损表示的数据；Yams/JSON 解析失败仍为系统或英文信息 |
| Protobuf | Hex/Base64 wire 解码，varint/fixed32/fixed64/length-delimited、UTF-8 检查；wire 解析校验错误随 `language` 三语（`protobuf.error.*`） | `.proto` schema、按 schema 编码、嵌套类型推断；不支持废弃 group wire 类型 |
| 环境变量 | 查看进程环境、`.env` 草稿、导出、传给代码运行；输入区标题与导出按钮支持三语 | 不写入 shell profile、launchd 或其他应用环境 |
| HTTP | cURL 导入/导出；方法、URL、启用/禁用查询参数和 Cookie；原始/JSON/URL 编码表单与 **multipart/form-data**（文本字段 + 本地文件，单文件/总正文 10 MB）；集合分组、搜索、保存/替换/删除；正文/响应头/Cookie 分栏（最终响应 `Set-Cookie` 含 Domain/Path/Expires）；JSON 显示格式化；超时、重定向、跨工具切换取消；临时 URLSession；**设置中的 HTTP 代理**（主机/端口/可选认证）；请求/正文类型、参数与 Cookie 表单、multipart 编辑器、cURL 导入与请求集合弹窗支持三语；**NetworkServices / cURL 导入 / 请求头·URL·Multipart 校验**等 Core `ToolError` 随 `language` 三语（`http.error.*` / `http.curl.*` / `http.field.*` / `http.multipart.*`） | 完整 cURL 选项、Cookie 文件、SOCKS/系统代理；GET/HEAD 不附正文；响应上限 10 MB；底层 URLSession/系统网络错误仍为系统语言 |
| Host | 与 Electron 相同的配置列表 + 编辑器分栏（`host-workspace`）；读取 `/etc/hosts`、IP/映射校验、多配置保存/搜索、导出独立文件；配置写入 `workspace.json`；菜单栏托盘可切换已保存配置；SQLite `t_host` 可合并导入；侧栏搜索、空状态、编辑器标题与保存/读取状态支持三语；**校验/保存**错误与校验结果摘要支持三语（`host.error.*` / `host.validate.*`） | 不执行管理员提权覆盖系统 Hosts |
| 网络工具 | DNS（dig）、Ping、Whois、ifconfig/netstat、DNS 缓存刷新、主机解析、IPv4↔Long、/24 IP 段 Ping 探测、TCP 端口扫描（常见端口或自定义）、本机地址列表；面板分区、按钮与输出标题支持三语；输入校验错误随 `language` 三语（`net.error.*`） | 与 Electron 相同的并发/取消细粒度控制；Windows/Linux 命令差异不适用本产品线；命令 stderr 仍为系统语言 |
| UA 解析 | Safari、Chrome、Edge、Firefox、Opera；常见 OS/设备规则 | 未集成完整 UA 数据库；伪装或罕见 UA 可能识别不准确 |
| 编码转换 | UTF-8 Base64、Base32、URL component、Hex、Unicode UTF-16 转义、常用 HTML 实体；交换输入/结果提示支持三语；编解码校验错误随 `language` 三语（`encode.error.*` / `hex.error.*`） | 其他字符集、完整 HTML 命名实体库 |
| 加密工具 | 与 Electron 相同的五标签布局（对称 / 非对称 / 摘要 / 编码 / 随机）：对称 **AES-GCM** 及 **AES/DES/SM4 ECB**；非对称 **RSA**（含私钥加密/公钥解密）与 **SM2**（`sm-crypto` 同源脚本、C1C3C2）；摘要/编码/随机与 Electron 一致；五标签分段、字段标签与随机/摘要主操作支持三语；对称 **AES/DES/SM4 ECB**、Base32 与 TextTool legacy **AES-GCM** 校验错误随 `language` 三语（`crypto.error.*` / `encode.error.base32*` / `textCrypto.error.*`） | AES-GCM 仍使用 Hex 密钥与 Base64 密文；SM2 依赖内嵌脚本而非原生曲线实现；随机串字符集细节可能略有差异；主 `CryptoToolView` 引擎结果摘要仍为操作输出而非独立 i18n 键 |
| 正则 | ICU 正则、ims 标志、UTF-16 偏移、捕获分组、模板替换；表达式/标志/替换模板与匹配·替换模式控件支持三语（模式持久化为 `match`/`replace`）；匹配超时/上限提示随 `language` 三语（`regex.error.limits`） | 与 JavaScript RegExp 存在语法差异；2 秒/10000 项匹配限制；ICU 语法错误仍为系统或英文 |
| Cron | 五段 Unix 与六/七段 Quartz（`?`、MON–SUN、秒字段）、`L`/`W`/`#`（月末、最近工作日、第 N 个/最后一个周几）、构建器与常用预设（含 Electron 四条 + 月末/首个周五）、**工作区**构建器/表达式/时区/预设与下十次执行标题支持三语；常见表达式 **`describe` / `naturalSummary` 可读摘要**、时区选择与下十次执行时间随 `language` 三语（`cron.describe.*` / `cron.weekday.*`）；解析/构建/运行次数等**引擎错误**随 `language` 三语（`cron.error.*`） | 完整 cronstrue 级自然语言、复杂 `L/W` 组合与列表混写 |
| 二维码 | Core Image 生成、四级纠错、含静区 PNG、Vision 图片识别；工具栏、空状态与识别结果区主文案支持三语；生成/识别常见错误提示三语 | 摄像头、Logo、批量或其他条码；Vision 系统错误仍为系统语言 |
| 时间转换 | 与 Electron 对齐的当前时间带、时区选择与快捷区、时间戳↔本地时间双向转换（秒/毫秒单位）、大时钟浮层、历史记录；面板主文案与历史摘要支持 zh-CN / en-US / ja-JP；「详细解析」保留 ISO 8601/日期文本及秒毫秒/UTC 多行输出；**校验错误与详细解析字段标签**支持三语（`timeConvert.error.*` / `timeConvert.detail.*`） | 独立全屏 Portal 动效；13 位及以上数字按毫秒解析（与 Electron 一致），更短毫秒戳请用手动单位或 ISO |
| 留言板 | 预设文案（含主题色）、字号、左/居中对齐、前景/背景、草稿持久化、自适应预览、独立全屏窗口、Esc 退出、展示时阻止显示器休眠；工具栏与编辑区主控件支持三语；8 条常用留言预设与 Electron `messageBoard.preset.*` 对齐且三语 | 与 Electron 一致的完整主题面板、字号百分比自适应算法与入场动画 |
| 翻译 | 与 Electron 相同的「翻译 / 词库 / 历史」标签；macOS 15+ 系统 Translation、分栏原文/译文（`translation`）与词库分栏（`translation-words`）；词条写入 `workspace.json`；系统词典入口；SQLite 词条/历史可合并导入；翻译/词库/历史子面板主控件与空状态支持三语 | 第三方翻译供应商；macOS 14 无系统 Translation，仍可使用词库/历史与词典 |
| 计算器 | 运算优先级、幂、科学计数、常用函数、常量、64 位进制转换；模式 Picker 支持三语（`expr`/`decToRadix`/`hexToDec`/`binToDec`）；表达式与进制校验错误随 `language` 三语（`calculator.error.*`） | 任意精度、单位换算；三角函数使用弧度 |
| 调色板 | 系统 ColorPicker、屏幕取色、HEX/RGB/HSL/SwiftUI；颜色/正则/Cron **收藏**（`toolFavorites`，与 Electron 收藏夹语义一致）；工具栏、预览标题、结果栏**格式标签**（`color.label.*`）与 HEX 校验提示支持三语；收藏弹窗标题与操作支持三语 | 全部色彩空间和 Electron 的配色功能；收藏夹分组 UI 较简；SwiftUI 代码片段语法仍为英文 API |
| 图片工具 | 拖放、预览、可拖动分栏（预览 / 导出选项）、比例缩放、PNG/JPEG/TIFF、JPEG 质量、文字水印、系统截图；工具栏与导出侧栏主控件支持三语；分栏宽度写入 `workspace.json`；加载/导出/截图常见错误提示三语 | 批处理、多图库列表、矢量化、复杂编辑；导出新位图，不保留原 EXIF/色彩配置/动画帧 |
| PDF | PDFKit 预览、多文件排序合并、页码提取、文本提取和导出；工具栏与列表面板主控件支持三语；加载/页码/导出常见错误提示三语 | OCR、密码处理、复杂压缩/水印；重建页面不保留原文档的书签和签名 |
| 系统信息 | OS、CPU 核数、内存、运行时间、磁盘、system_profiler 详细信息；刷新/详细报告按钮与输出区标题支持三语；摘要块字段标签随界面语言 | 持续传感器监控和 Electron 全部硬件指标；system_profiler 原始输出仍为英文 |

## 工作台

已实现分组侧边栏、**界面语言**（zh-CN / en-US / ja-JP，侧栏与搜索；**菜单栏托盘**菜单项随 `general.language` 刷新）、**GitHub 更新检查**（`autoCheckUpdates`）、**菜单栏托盘**（打开/设置/取色/截图/翻译/Host 配置，设置中可关闭）、**关闭主窗口**（询问/隐藏/退出，与 Electron `closeBehavior` 一致）、常用工具、**自定义分组**（设置中管理名称与工具列表，写入 `workspace.json`）、最近使用、⌘K 搜索、深浅色、原生编辑/文件对话框、独立工具窗口、草稿恢复、历史/收藏和本产品备份。工具窗口使用同一个原生版工作区状态，切换不会清空文本草稿。HTTP 配置/请求集合、JSON 格式/结构视图选项、格式化类型/缩进/文件内容与结果、文本对比视图/高亮/空白选项、随手记文档设置/查找替换面板选项、文档库层级/选择/搜索/排序/展开状态均随本产品工作区保存。0.1.0–0.7.0 备份无需转换即可读取，新字段按默认值初始化。

文档与文件夹通过稳定标识关联，移动或重命名不会改变打开的文档；失败的批量导入不会部分写入。删除活动文档时保留当前内容为草稿。编辑器状态使用 UTF-16 选择范围及滚动位置；切换文档时清理当前视图的撤销栈，重启不保留撤销历史。

### 0.8 有意边界（非本轮交付）

以下与 Electron 的差异为**产品/技术边界**，不视为 0.8 缺陷：安装包后台下载与静默安装（仅 GitHub API + 打开下载页）；Compose/Electron **遗留迁移服务**级一键全量镜像（原生提供 `mootool-next.json`、SQLite 与 `quick-notes` / `json-vault` / Java 磁盘目录**合并导入**，见 [数据迁移](data-migration.md)）；Monaco 级编辑装饰；用户可编辑的全局快捷键与托盘热键（与 Electron 相同为设置页只读说明）；第三方/系统解析器与 `system_profiler` 等输出仍为英文或系统语言。

**0.8 多语言（zh-CN / en-US / ja-JP）**已覆盖侧栏/搜索/26 工具名、首页与设置导航、工作台工具栏与 ToolPage/共享编辑器栏、应用菜单、数据迁移与备份确认、文档库与 Vault Git、HTTP/Host/翻译/Cron/网络/加密等主要工具工作区，以及 **MooToolNextCore** 用户可见 `ToolError`、JSON worker（`JSONDispatch.js`）、Cron `describe`/`naturalSummary`、随手记预览/附件与菜单栏托盘状态等；各工具行「尚未覆盖」列仍列 Monaco、附件双向同步、完整 cronstrue 等**产品能力**差异，而非 0.8 i18n 缺口。全局快捷键与 Electron 相同为设置页只读（⌘K / ⌘, 等，不可自定义绑定或托盘全局热键）。

**迁移与布局**：设置「数据迁移」合并 `mootool-next.json` 工作台布局/代理/编辑器与 Git 选项、SQLite（HTTP/Host/翻译/历史/草稿/收藏、`t_func_content`、`t_quick_note`/`t_json_beauty`）及 Java/Electron 磁盘 `quick-notes`/`json-vault`（含 `attachments/`）；`showRecent`、侧栏宽度、隐藏工具、`layoutPaneSizes` 等写入 `workspace.json`；留言板/图片/PDF 草稿持久化；随手记快速替换保留选区。Compose 全量 pane 与子面板逐项镜像仍可能有差异；不含 Electron 附件路径双向实时同步。

## 0.8 交付结论

**对齐范围（已验收）**：26 个入口与 Electron `toolRegistry` 一致；各工具**功能入口、面板顺序与主要操作语义**见上表「当前原生实现」列。工作台横切（语言、侧栏/搜索、自定义分组、`layoutPaneSizes`、迁移、托盘、关闭主窗口、GitHub 更新、Vault Git、备份/草稿恢复等）已写入 `workspace.json` 或设置 UI，并与 [data-migration.md](data-migration.md) 描述一致。

**「尚未覆盖 / 明确边界」列**：记录与 Electron 的**能力差距**及 0.8 **有意不交付**项（Monaco、附件双向实时同步、完整 cronstrue、HTTP 全量 cURL、安装包静默安装等），不是 0.8 待办清单。关联 workspace 文档（[quick-note-workspace.md](quick-note-workspace.md)、[json-engine.md](json-engine.md)、[reformat-workspace.md](reformat-workspace.md)、[text-diff-workspace.md](text-diff-workspace.md)、[note-attachments.md](note-attachments.md)）与上表同步。

**发布与门禁**：根 [README.md](../../README.md) 与 [release-notes/0.7.0.md](../release-notes/0.7.0.md)、[0.8.0.md](../release-notes/0.8.0.md) 已标注 **0.8.0**；[`./scripts/check-core.sh`](../scripts/check-core.sh) **59 组 0 失败**、[`./scripts/smoke.sh`](../scripts/smoke.sh) 全绿见 [verification-0.8.0.md](verification-0.8.0.md)（2026-09-17）。

## 验证口径

核心测试覆盖工具输入输出、格式错误、溢出、加密认证、日期/Cron、隔离存储和进程/HTTP 边界；文档库新增循环层级、名称冲突、跨工具移动、检索/排序、批量导入原子性、路径别名/符号链接、旧工作区兼容等用例。原生验收操作实际 NSTextView，检查快速切换、光标/滚动恢复、撤销隔离、多窗口编辑及草稿找回，再通过新进程验证重启恢复。真实窗口截图检查双主题、文档检索、分栏与窄窗口布局。

## 文档库布局参照

| 区域 | Electron 参照 | 当前原生版 |
| --- | --- | --- |
| JSON 文档库 | `JsonVaultPanel.tsx`：搜索、正文检索/排序、操作栏、树、底部路径 | 保留相同面板顺序；原生列表、上下文菜单与移动对话框 |
| 随手记文档库 | `QuickNoteTool.tsx` / `QuickNoteTree.tsx`：搜索、检索/三种排序、新建/展开、树 | 保留相同操作和位置；补批量导入到更多菜单 |
| 随手记编辑区 | 三种视图、文档颜色/语法/字体/字号/行距/换行、格式化/列表、查找/保存/快速替换/删除 | 沿用上述顺序；原生菜单/分栏/状态栏，窄编辑区工具栏换为两行、替换面板弹出。文档库开关位于文档标题行；图片按钮位于保存之后，Git/Finder 仍列为差异 |
| 文件/文件夹操作 | 新建、重命名、移动、复制、删除；拖动到文件夹或根目录 | 支持等价操作；采用稳定 ID 的虚拟层级，不依赖 Electron 文件路径 |

原生文档库保存在独立 `workspace.json`，导入复制用户选中文件的 UTF-8 内容，后续修改不回写原始文件；JSON / 随手记会镜像到 `json-vault/`、`quick-notes/` 并支持 FSEvents 监听、Finder 定位与「从磁盘刷新」。文件夹最多 64 层，一次导入最多 500 份/32 MB、每份最多 10 MB。同名文件增加数字后缀，目录合并；层级或文件错误会中止整批导入，校验提示支持 zh-CN / en-US / ja-JP（`vault.import.error.*`）。布局与功能的剩余差异会继续按对应 Electron 页面补齐。

0.4.0 将 JSON 编辑区改为主编辑器和可折叠检查器，格式化直接更新正文，查询/输出转换使用结果弹窗；路径选择器提供树和预览。窄窗口使用原生弹出面板和更多菜单。布局、语义及解析差异详见 [JSON 工作区与解析边界](json-engine.md)。

0.5.0 补齐随手记逐文档编辑设置、24 项快速替换及查找替换栏，新增 Markdown 表格和任务列表。颜色用于文档树分类，字体/字号/行距/语法直接作用于编辑器；切换文档恢复各自设置，切换预览保留当前撤销栈。相关实现和剩余差异见 [随手记工作区与边界](quick-note-workspace.md)。

0.6.0 新增随手记图片附件，沿用 Electron 的选区、换行、剪贴板和多图顺序语义；原生版采用独立、按内容命名的文件，不在删除引用时立即清理图片，以保留撤销与副本。导出与备份差异、格式和大小限制见 [图片附件与备份](note-attachments.md)。

0.7.0 将格式化页改为 Electron 对应的文本 / 文件工作区，内嵌原生版独立的 Prettier、Java/XML 插件及 Nginx 格式化器；文本原位更新，文件原文与结果并列。旧原生 JSON/XML 草稿与历史继续可用。详见 [格式化工作区与边界](reformat-workspace.md)。

0.8.0 将文本对比页改为 Electron 对应的双编辑器和可选统一视图，提供字符/行差异、忽略空白、导航、同步滚动、交换、复制和历史。边界与验收见 [文本对比工作区](text-diff-workspace.md)。同版本还包括：zh-CN / en-US / ja-JP 界面与 Core/worker 错误三语、菜单栏托盘、设置内 Electron `mootool-next.json` / SQLite / 磁盘目录合并迁移、Vault Git 与文档库 FSEvents、工作台 `layoutPaneSizes` / 侧栏与隐藏工具、关闭主窗口行为与 GitHub 更新检查；发布说明见 [release-notes/0.8.0.md](../release-notes/0.8.0.md)，自动化验收见 [verification-0.8.0.md](verification-0.8.0.md)。

## cURL 导入范围

按 [curl 官方参数说明](https://curl.se/docs/manpage.html) 实现单条 HTTP/HTTPS 请求的常用子集：`-X`、`-H`、`-b`、`-u`、`-A`、`-e`、`-m`、`-G`、`-I`、`-L`、`--url`、`--data`、`--data-raw`、`--data-binary`、`--data-urlencode`、`--json`。支持单/双引号、空参数、短选项紧邻值、长选项 `=` 和反斜杠续行。`-s/-S/-i/-v/--compressed/--globoff` 等输出或传输显示选项不改变请求编辑内容。

不执行 shell、不展开变量/命令替换、不读取 `@文件`，也不静默忽略未知请求选项。TLS 跳过校验、multipart、多 URL、代理与证书文件等暂不导入；遇到这些选项会说明原因。导入只填充编辑器，由用户点击发送后才访问网络。普通请求默认跟随重定向；导入的 cURL 按是否包含 `-L` 设置此开关。

运行时安装、外部网络服务、系统语言模型下载、屏幕录制权限、各 macOS 版本/硬件、签名公证和所有工具按钮操作仍需对应环境的交互验收。不能把“26 个入口可打开”理解为 Electron 全部功能已达成一致。
