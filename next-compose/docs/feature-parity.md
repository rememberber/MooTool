# 逐工具功能与兼容规格

> 全部条目均为待开发要求。当前没有 Compose 已实现功能。每项开发前需读取实际页面、算法、服务和测试，枚举细粒度控件；本文不是省略源码审计的理由。

## 1. 工具注册与参考入口

保留 **26 个入口 = 首页 + 25 个工具**。分组顺序 text/dev/network/encode/daily/system；ID 是稳定持久键，显示名可本地化。

表内 Electron 路径相对 `next/src/features/`；Java Form 相对 `src/main/java/com/luoboduner/moo/tool/ui/form/func/`，首页例外。

| 编号 | Tool ID | 名称 / 分组 | Electron 页面 | Java 参考 |
| --- | --- | --- | --- | --- |
| F00 | `mootool` | 首页 / home | `home/HomePage.tsx` | `../AboutForm.java` |
| F01 | `quickNote` | 随手记 / text | `quickNote/QuickNoteTool.tsx` | `QuickNoteForm.java` |
| F02 | `textDiff` | 文本对比 / text | `diff/TextDiffTool.tsx` | `TextDiffForm.java` |
| F03 | `reformat` | 格式化 / text | `reformat/ReformatTool.tsx` | `FileReformattingForm.java` |
| F04 | `json` | JSON / dev | `json/JsonTool.tsx` | `JsonBeautyForm.java` |
| F05 | `java` | 代码运行 / dev | `runtime/RuntimeTool.tsx` | `JavaConsoleForm.java` |
| F06 | `ymlProperties` | 配置转换 / dev | `config/ConfigConvertTool.tsx` | `YmlPropertiesForm.java` |
| F07 | `protobuf` | Protobuf / dev | `protobuf/ProtobufTool.tsx` | `ProtoBufForm.java` |
| F08 | `variables` | 环境变量 / dev | `variables/VariablesTool.tsx` | `VariablesForm.java` |
| F09 | `http` | HTTP 请求 / network | `http/HttpTool.tsx` | `HttpRequestForm.java` |
| F10 | `host` | Host / network | `host/HostTool.tsx` | `HostForm.java` |
| F11 | `net` | 网络/IP / network | `net/NetTool.tsx` | `NetForm.java` |
| F12 | `uaParse` | UA 分析 / network | `ua/UaParseTool.tsx` | `UaParseForm.java` |
| F13 | `encode` | 编码解码 / encode | `encode/EncodeTool.tsx` | `EnCodeForm.java` |
| F14 | `crypto` | 加解密/随机 / encode | `crypto/CryptoTool.tsx` | `CryptoForm.java` |
| F15 | `regex` | 正则 / encode | `regex/RegexTool.tsx` | `RegexForm.java` |
| F16 | `cron` | Cron / encode | `cron/CronTool.tsx` | `CronForm.java` |
| F17 | `qrCode` | 二维码 / encode | `qrcode/QrCodeTool.tsx` | `QrCodeForm.java` |
| F18 | `timeConvert` | 时间转换 / daily | `time/TimeConvertTool.tsx` | `TimeConvertForm.java`、`ClockForm.java` |
| F19 | `messageBoard` | 留言板 / daily | `messageBoard/MessageBoardTool.tsx` | 以 Electron 为准 |
| F20 | `translation` | 翻译 / daily | `translation/TranslationTool.tsx` | `TranslationForm.java` |
| F21 | `calculator` | 计算器 / daily | `calculator/CalculatorTool.tsx` | `CalculatorForm.java` |
| F22 | `colorBoard` | 调色板 / daily | `color/ColorBoardTool.tsx` | `ColorBoardForm.java`、`ColorPickerForm.java` |
| F23 | `image` | 图片助手 / daily | `image/ImageTool.tsx` | `ImageForm.java` |
| F24 | `pdf` | PDF / daily | `pdf/PdfTool.tsx` | `PdfForm.java` |
| F25 | `hardware` | 系统信息 / system | `hardware/HardwareTool.tsx` | `HardwareInfoForm.java` |

所有工具适用的共同行为：切页/窗口转移保留输入输出、选项、Tab、选区与滚动；正确处理空态、执行、成功、失败和取消；输出可选择复制；错误保留输入与标明的上次有效结果。不要自动把每次按键记为一次历史。

注册表中 `mootool/hardware/messageBoard` 不声明通用历史，其余声明支持。通用收藏仅 `regex/cron/colorBoard`；请求集合、单词本、Vault 有各自数据模型。

## 2. 逐工具规格

### F00 首页

布局：限宽居中内容，品牌/产品版本、介绍、贡献者、赞赏、源码/帮助和其他作品；可纵向滚动。

- 所有身份、版本、关于信息均来自 Compose 产品，不显示 Electron 1.1.4 或 Java 1.8.6 冒充本版本。
- 图标/赞赏图等有权资源复制到本目录，并保留来源及许可证；离线主体可展示。
- 官网、GitHub/Gitee、Issues 和作品链接使用真实去向，通过系统浏览器打开。
- 验收：离线显示、三语言、窄窗口/深色、外部链接和本产品版本正确。

### F01 随手记

布局：左 Vault 树，中编辑/预览，右可折叠快速替换；顶部紧凑工具栏，底部文档路径、保存与统计。

- 文档库搜索、正文检索、排序、新建文件/目录、重命名、复制、移动、删除、导入导出、打开目录、拖放；树展开/选中/排序重启恢复。
- 名称/路径/正文检索分清用途；创建/修改时间排序；`.gitignore` 过滤与默认展开策略。全文搜索后台索引，不在输入每个字时同步扫描整个文件夹。
- 文档元数据包括语法、字体字号/颜色、换行等适用属性。按 `quickNoteVaultRepository.ts`/Java frontmatter 规则建立导入适配，自有元数据不能作为正文露出。
- 编辑/分栏/预览三模式；Markdown 标题、列表、表格、任务、链接、图片和代码块；不执行脚本。模式切换不清空 undo。
- 撤销/重做、查找替换、选择、字体/语法、列表相关动作、复制、保存、统计；实际按钮顺序逐项对照工具栏。
- **列编辑必做**：矩形选择、多行输入/删除/粘贴、短行补齐、一次操作一次撤销。中文、emoji、Tab、软换行语义按 EditorHost 规范验证。
- 图片剪贴板、外部图片拖入、插入本地图片保存到自有附件目录，写相对 Markdown 引用；连续粘贴保持顺序；文本粘贴不被图片处理器抢走。
- 附件移动/导出、引用一致性、孤立附件清理；清理先扫描引用，有引用附件不能删除。失败不能产生失效引用或覆盖已有文件。
- 自动保存按文件顺序落盘，不重设文本/光标；切换文档时保存失败可见；外部修改冲突不得后写覆盖。
- Git 状态、diff、提交、拉取、推送、历史、冲突继续/中止和自动策略见数据文档；没有 Git 时可正常本地编辑。

快速替换共 24 项，选区优先，否则全文；每次为可撤销事务。locale 排序、换行归一、数字精度应固定 fixtures。

| ID | 语义 |
| --- | --- |
| trim | 每行去首尾空白 |
| removeBlankLines | 删除空白行 |
| removeTabs | 移除 Tab |
| scientificToNormal / normalToScientific | 科学计数与普通数字互转 |
| thousandsToNormal / normalToThousands | 移除/添加千分位 |
| underscoreToCamel / camelToUnderscore | 命名转换 |
| uppercase / lowercase | 大小写转换 |
| linesToComma | 多行转逗号列表 |
| linesToSingleQuoted / linesToDoubleQuoted | 转单/双引号包裹列表 |
| commaToLines / tabsToLines | 分隔符转行 |
| clearNewlines | 移除换行 |
| deduplicateLines / deduplicateWithCount | 行去重/去重并计数 |
| escape / unescape | 字符串转义/还原 |
| reverseLines | 行序反转 |
| sortAscending / sortDescending | 行排序 |

验收：5 MiB 文档、连续图片粘贴、列编辑 undo/redo、双击选词后自动保存、分栏切换/分离收回后 undo、磁盘满、外部修改冲突、重启恢复。

### F02 文本对比

布局：上模式/选项、左右文本输入/结果，Unified 模式保留原有差异区关系。

- 真实行级/字符级 diff、三种高亮模式、忽略空白、同步滚动、上/下差异、差异计数、导入/复制/清空、历史。
- 重复行、插入/删除连续段、末尾换行、CRLF/LF、Unicode 偏移均需处理；不能只按相同行号比较字符串。
- 大内容异步比较并按 revision 应用；切换模式不丢左右内容；无差异和空输入可区分。
- 验收：定位到真实差异、同步滚动不会相互抖动、统一差异可复制；fixture 和新输入覆盖旧计算场景通过。

### F03 格式化

布局：文本/文件 Tab；Nginx、Java、XML、HTML 类型及缩进选择，保持输入/操作/结果关系。

- 文本格式化、文件选择、输出复制/保存、清空、历史；键盘格式化与按钮走同一服务。
- Nginx 识别引号/注释/转义/块；XML/HTML 保留文本节点语义；Java 使用真正语法处理器（JavaParser）。与 Electron Prettier 的差异见 [DIFF-005](diff/005-reformat-jvm.md)。
- 不以通用花括号换行代替多语言 formatter，不要求普通使用者安装 Node。
- 原文件默认不覆盖；语法错误保留输入并定位；已支持语言要有幂等样本。
- 验收：复杂字符串、注释、Unicode、格式化幂等、文件写入失败与再次修改后重试。

### F04 JSON

布局：左 JSON Vault，中主编辑器，右格式/转换/JSONPath 检查器；窄窗口优先折叠辅助面板。

工具栏以 `JsonToolbar.tsx` 为参照：格式化、压缩、字体、换行、复制、查找、导入、导出、历史、更多、清空。保留主要动作位置与方向。

1. 校验、2/4 空格格式化、压缩、错误定位与结构摘要。
2. 递归 key 排序、忽略大小写、重复 key 检测；**检测必须在普通 Map 丢弃重复项之前**完成。
3. JSON ↔ XML、JSON ↔ JavaBean；类名、嵌套、数组、类型推断及结果弹层。JavaBean 支持边界以 fixtures 固定，不宣称编译任意 Java 项目。
4. Key/Value 互换、JSON 字符串转义/还原、普通/Java 字符串相关转义动作，明确各按钮语义。
5. JSONPath 输入、查询结果、路径树选择、路径/值预览及双击行为；递归、数组索引/切片、联合、filter、路径转义需真实可用。
6. 查找/替换：大小写、全词、正则、计数、前后导航；字体、换行、复制、文件操作与历史恢复。
7. Vault 文件/目录 CRUD、重命名、复制、移动/拖放、排序、忽略文件、树状态、当前文件恢复、外部监视。
8. Git 工作流与冲突，P6 补齐后才可将完整 F04 标记通过。

JSON 文本编辑保留大整数与小数字面量；不能先转 Double 再声称无损。格式化与转换分别定义数值策略。JSONPath 不执行任意 JS；不支持的语法明确错误，不能只做 `$.a.b` 冒充全部功能。

验收：中文/emoji/嵌套数组/重复 key、9007199254740993、转义路径、真实筛选、错误行列、3 MiB 输入、Vault 移动后重启、保存冲突、窗口转移的选区与 undo。

### F05 代码运行

布局：Java/Groovy、Python、Node.js 三个主 Tab；首个 Tab 内选择 Java 或 Groovy；上编辑下输出。

- 稳定 Tool ID 仍为 `java`；每种 runtime 独立草稿、代码语言、格式化、参数、工作目录和执行路径；检测安装版本并能手动配置。
- 真实运行/停止，stdout/stderr 流、退出码、耗时和命令摘要，历史恢复。
- 源代码上限 1 MiB，输出累计上限 2 MiB；到限截断有提示。timeout 统一配置并显示，不让无限循环占住进程。
- 子进程 argv 传参，不拼 shell；中文空格路径、缺少编译器、非零退出、启动失败、停止全部后代和临时文件清理都需处理。
- 自带 app JVM 不代表含编译用户 Java 的 javac；Java/Groovy 运行方案在设置中清楚展示。
- 验收：四种实际运行环境各输出 42，参数和目录生效；无限循环可停；切页/窗口转移不启动第二次；关闭应用时进程收尾可验证。

### F06 配置文件转换

布局：Properties ↔ YAML 的左右输入输出与中间动作，保留 YAML 校验/格式化。

- 点路径、数组路径、嵌套、字符串/布尔/数字/null、转义、排序和默认值按 `configTools.ts` 及 Java 工具冻结。键冲突见 [DIFF-006](diff/006-config-snakeyaml.md)。
- 键冲突、数组/对象冲突明确报错或提供有记录的规则，不能静默丢值。
- 不宣称注释、锚点或自定义 tag 全部无损往返；未知类型要说明限制。
- 验收：嵌套列表、含点键/特殊字符、空值与冲突、导入导出/历史、格式化幂等及失败保留原文。

### F07 Protobuf

布局与 Tab：JSON/Binary、Wire、Hex/Base64；schema 编辑器 + message 名，相关输入输出。

- 临时 `.proto` 动态处理、JSON ↔ bytes、Hex/Base64 切换、定义格式化、历史。
- Wire 输出字段号、wire type、varint/64-bit/length-delimited/32-bit 内容，处理 group、截断、越界与非法 tag。
- nested、repeated、enum、map、oneof、int64/uint64、bytes、默认字段、未知字段及 import 支持范围逐项建样本。
- Electron `keepCase` 和默认/long/enum/bytes 映射作为兼容参照；Java JsonFormat 不同部分通过适配或明确差异解决。
- 动态解析具体走 [架构](architecture.md) 的 protoc + Descriptor 路线；不能只带生成式 protobuf runtime 或几种写死 message。JSON 映射与 protobufjs 差异见 [DIFF-007](diff/007-protobuf-jsonformat.md)。
- 验收：用户粘贴全新 message 无须重编译即可往返；输入错误具体；恶意二进制有上限；确定性样本按 bytes 比较，map 顺序差异按协议结构比较。

### F08 环境变量

布局：作用域/信息 Tab、搜索/刷新/复制/导出、变量表。

- 用户/系统变量查看、新增、编辑、删除；当前进程环境和应用 JVM/OS 属性只读。将 Electron 进程属性替换为 JVM 对应信息，记录技术等价差异。
- 修改必须持久作用于标明的作用域；修改当前进程 Map 不能冒充修改系统设置。新进程何时生效要说明。
- Windows 注册表与 Unix 环境文件方案区分实现；Unix 没有单一通用全局环境存储，显示所支持的配置文件/会话范围，保留未知配置。
- 改动前呈现差异和备份；系统作用域按需受限提权；不要求常驻管理员启动。
- 验收：隔离用户/VM 中新增→新进程读取→修改→删除；拒绝权限保持原内容；刷新信息不产生写入。用户文件为本产品 `data/environment`，不写 `~/.MooTool`；不改写当前 JVM `System.getenv`。见 [DIFF-016](diff/016-environment-compose-namespace.md)。

### F09 HTTP 请求

布局见 UI 文档：左请求集合，右 Method+URL+发送/取消，请求上/响应下分栏。

- Method：GET/POST/PUT/PATCH/DELETE/HEAD/OPTIONS；请求 Tab：Params/Headers/Cookies/Body；响应 Tab：Body/Headers/Cookies。
- 当前 Body MIME：`application/json`、`text/plain`、`application/xml`、`text/xml`、`text/html`、`application/javascript`；正文格式化、语法高亮。
- Params/Headers/Cookies 支持行开关、增删、顺序与空值；Cookies 保留原数据字段。Query/表单重复键不能被 Map 吞掉。
- **冻结请求语义**：GET/HEAD/OPTIONS 将 Params 追加到 URL，忽略 body；其他方法有非空 body 时发正文，无正文时 Params 编码为 URL 表单。原 URL 自带参数保留。若后续调整为更明确的参数模型，需单独记录差异与迁移。
- Header 按协议处理；Electron 的同名 Header 合并有局限，Compose 应声明重复字段策略；不静默合并 Set-Cookie 破坏语义。
- 默认超时 30 秒（服从设置），重定向、代理与代理认证、取消、耗时/状态码/响应地址、大小、复制/导出；HTTP 4xx/5xx 仍是可查看的真实响应。
- 解压后响应限制 10 MiB；二进制或无法解码时给出字节/下载视图，不强制乱码。连续请求按 requestId 归属，旧结果不覆盖新编辑。
- 集合新建、命名、搜索、保存覆盖、删除及历史回填；请求所有字段、选项和必要响应快照完整持久化。
- cURL 导入/导出是文本解析/生成，支持范围逐项列明；**绝不执行粘贴命令**，未知参数不能悄悄丢失认证/正文。
- 验收：本地服务器核对 method/query/body/重复参数/headers/cookies、重定向、超时、取消、超限响应；重启集合恢复；无网不能显示成功示例。

### F10 Host

布局：左方案列表，右 hosts 编辑与操作，系统 hosts 独立查看入口。

- 方案新增、复制、命名、保存、删除、导入/导出、查找替换；读取系统、应用方案与 DNS 刷新。
- “保存方案”只写 Compose 数据；“应用到系统”才修改全局 hosts。先 diff、备份，提交时校验系统文件未被别人改动。
- 校验 NUL、无效条目、编码/换行；提权失败/拒绝不能标成成功；备份与恢复真实可用。
- 验收：临时文件模拟错误；隔离 VM 实测应用/恢复/DNS 结果；应用关闭不自动恢复旧方案或覆盖其他程序变更。

### F11 网络/IP

布局：左命令结果、右功能区；IPv4 ↔ Long、ping、DNS/地址解析、WHOIS、本机地址。

- 输出可选择/复制，命令流式展示可停止；主机名与参数以 argv 传递。
- IPv4 0/最大值/越界/非法段正确；IPv6 在 DNS/本机地址结果保留，不强行进入 IPv4 数值转换。
- 命令/WHOIS 服务不存在或离线时显示真实原因，不填示例 IP。
- 验收：localhost、受控 DNS、本机地址、停止 ping、无网/超时/非法主机名与中文系统输出。命令经 argv 启动，DNS 用 `InetAddress`，子进程编码见 [DIFF-015](diff/015-net-process-charset.md)。

### F12 UA 分析

布局：原文输入、样例/预设、解析操作、浏览器/引擎/OS/设备等结构化结果。

- 真实维护的 UA 规则，移动/Bot 分类、原文和结果复制、历史；未知字段显示未知。
- 不只检测 Chrome 字符串；不同库名称归一由 fixtures 定义。引擎字段见 [DIFF-002](diff/002-ua-engine-inference.md)。
- 验收：Chrome/Safari/Firefox、iPhone/Android、bot、空/未知 UA；版本与设备字段有证据。

### F13 编码解码

布局：保留 Unicode、URL、UTF-8 Hex、ASCII 等操作分区/顺序与输入→输出关系。

- Unicode 转义/反转义；URL UTF-8/GB2312；UTF-8 bytes Hex；ASCII 十进制/十六进制。
- URL 百分号、加号/空格与字符集规则按 `encodeTools.ts` 测试固定；非 GB2312 字符不静默变成问号。
- 明确 code point、UTF-16 code unit、UTF-8 byte 不同；非法 Hex、截断编码、坏转义报错保留原文。
- Base64/Base32 的既有入口在 Crypto 保留，不因为名称调整而挪走。
- 验收：中文、emoji、换行、空内容、百分号、空格、损坏字节与跨实现输出。

### F14 加解密与随机

Tab 顺序：对称、非对称、摘要、Base64/Base32、随机。

- 对称 AES/DES/SM4；非对称 RSA/SM2，密钥生成、加/解密、签名/验签，以及已有 RSA 私钥操作/公钥还原。
- MD5、SHA-1/256/384/512、SM3；文本和文件摘要。Base64/Base32 文本编解码。
- UUID、随机数字/字符串/密码，长度与历史；使用安全随机源；参数边界和密码类别规则固定。
- 当前对称基线 ECB + PKCS#7、Hex 密文，key 经源码字符截断/补字符 `0` 后 UTF-8 编码；DES 长度基线 8，AES/SM4 16。**不能换成 provider 默认模式**。
- 非 ASCII key 的字符与字节长度不一致；必须建立成功/拒绝 fixtures。可选择严格拒绝非法 key，但在 UI 明示，不伪称全部兼容。
- RSA DER/Base64 类型、PKCS#1 v1.5 加密、SHA-256 签名；SM2 密钥/密文编码、C1C3C2、DER 签名和 user ID/hash 参数按源测试冻结。
- 随机化加密结果不要求相同密文，应跨实现解密/验签；固定对称/摘要可比字节。
- 验收：Electron/Java 导出样本在 Compose 消费及反向消费；错误 key、坏编码、空内容、大文件取消。仅“自己加密自己解密”不合格。
- 工具兼容算法清楚标注模式，不将这条路径用于产品自身凭据存储。非 ASCII 密钥拒绝规则见 [DIFF-008](diff/008-crypto-key-bytes.md)。

### F15 正则

布局：表达式/flags、测试文本、匹配/捕获结果，常用/收藏及历史入口。

- 保留 global/ignoreCase/multiline/dotAll 操作与 21 个常用模式；常用模式源自 `regexTools.ts`，复制名称/语义/样例并本地化。
- 默认引擎 Java Pattern，显式显示引擎；JS/Java 差异见 [DIFF-003](diff/003-regex-java-pattern.md)；global 是遍历策略，不假作 Java flag。
- 匹配范围和捕获分组正确；空匹配能前进，emoji 不错位；坏表达式保留原文。
- 灾难性回溯必须可终止：独立 worker、时间和结果数上限；仅 coroutine timeout 不够。
- 验收：捕获/零长度/命名组、中文 emoji、非法模式、超时取消、收藏增删与重启恢复。

### F16 Cron

布局：秒、分、时、日、月、周、年构建器，表达式反向解析、预设、时区、解释、未来 10 次执行与收藏。

- 默认接受 6/7 字段；空 year 输出 6 字段。若增加 5 字段 Unix，作为明确模式，不自动误判。
- `?`、范围、步长、L/#、周名称/数字和 year 过滤以实际 Electron/Java 可用样本建兼容表；不宣称当前 Electron 是完整 Quartz。与 Electron `cron-parser` 的差异见 [DIFF-004](diff/004-cron-quartz.md)。
- 使用 IANA 时区、闰年/DST 规则；year 判断必须在选定时区，不能错误使用运行机器默认年份。
- 反向解析再生成保持含义；自然语言不能给不支持语法编造解释；搜索有上限，无下一次运行有明确结果。
- 验收：每分钟/工作日/闰年/跨年时区/DST、非法字段和无未来结果、收藏重启；输出是计算而非当前时间递增。

### F17 二维码

Tab：生成、识别。参数、预览、保存/复制/历史关系保留。

- 内容、尺寸、纠错 L/M/Q/H、Logo、生成预览、PNG 保存和图片复制；默认尺寸 300、纠错 M 接入设置。
- 文件/系统图片剪贴板识别，适用的普通/纯码处理；内容显示复制，坏图有实际错误。
- 真正 QR 编解码，实际像素尺寸与 DPI 区分；透明/Logo 不破坏输出。
- 验收：生成→保存→文件识别、生成→剪贴板→识别、中文 URL/Logo/透明图、权限或剪贴板失败。历史不保存 PNG 本体，见 [DIFF-009](diff/009-qr-history-png.md)。

### F18 时间转换

布局：秒/毫秒与日期时间双向、时区、当前/快捷时间及全屏时钟。

- 显式单位优先，不以字符串长度覆盖用户选择；负时间戳、严格日期校验、闰年与 DST。与 Electron 长度启发式的差异见 [DIFF-001](diff/001-time-explicit-unit.md)。
- 历史保存输入/选项；全屏时钟退出恢复原窗口状态；刷新任务仅在需要时保持。
- 验收：epoch/负值/毫秒、不同 IANA 时区对应同一瞬间、DST 缺失/重复时刻有明确解释、非法日期不自动进位。

### F19 留言板

布局：左控制、右大字预览，演示隐藏控制；本地展示牌，无在线发布。

- 输入长度基线 80（Electron 使用 JS length），8 预设、6 主题、左/居中、字号比例 70–130 和自动适配。
- 文案、主题、对齐、比例持久化。若 Compose 改为字素计数，记录 Unicode 差异并更新界面长度提示。
- 演示保持屏幕唤醒，按持有者 token 管理；退出、关闭、异常释放，不影响别的演示会话。
- Esc 先退出演示；按钮和文本不能被拖动窗口区域遮挡。
- 验收：长中文/多行、窗口缩小、预设/重启恢复、唤醒 token 释放和显示器切换。演示唤醒使用本机 `caffeinate` / `systemd-inhibit` / Windows `SetThreadExecutionState` 进程，而非 Electron `powerSaveBlocker`，见 [DIFF-011](diff/011-message-board-wake.md)。

### F20 翻译

Tab：翻译、单词本、历史；源/目标语言、Google/Bing、交换、自动/手动、复制/清空。

- 自动翻译约 500 ms debounce，手动立即发；过期响应不覆盖；历史/单词本回填不重复触发请求。
- 代理、默认 15 秒超时、取消、长文分段、服务 fallback；实际使用服务及 fallback 可辨。
- 单词本新增/编辑/删除/搜索/重译；历史持久化；离线能看本地数据。
- 服务端适配在实施时核验实际可用接口及要求，不把测试 stub 当线上实现。
- 验收：受控 HTTP 服务验证分段/并发顺序/取消/失败；另做注明日期/服务的真实联网检查；无网报告失败。

### F21 计算器

布局：表达式输入和等号按钮，进制、GCD/LCM、排列组合及本地记录。

- 等号显示 `=`，与输入框等高，这是 Electron 1.1.4 的具体布局参照。
- 首版表达式按已有四则/小数/括号/一元符号语义，长度上限 500；不擅自声称支持三角函数等扩展。
- 2/10/16 进制、GCD/LCM、排列/组合使用大整数；表达式精度/舍入单独定义，选择 BigDecimal 时记录与 Electron 14 位精度展示差异。
- 不能 eval/shell 执行字符串；除零、无穷、非法整数和组合参数报错。
- 验收：`2*(3+4)=14`、负数、优先级、大整数、零值和非法表达式，记录恢复与结果复制。

### F22 调色板

布局：主色/对比色、主题/标准色、格式、取色、运算和收藏/历史。

- HEX 大/小写及 RGB，当前 7 主题、10 标准色以 `colorTools.ts` 为依据。
- 运算五种：invert、intersect、add、difference、average；语义按函数逐通道实现，保留边界截断规则。
- 屏幕取色显示放大像素/位置/颜色，Esc 取消还原；本身遮罩不影响采样；多屏负坐标与 DPI 处理。
- 输出颜色空间明确，初版用 sRGB；ICC/广色域影响需实测，不直接把所有屏幕原始值当一致色彩。
- 验收：颜色格式往返、上下界/运算、收藏、真实屏幕取色/拒绝权限/多屏。屏幕取色使用 AWT Robot 冻结截图而非 Electron `desktopCapturer`，见 [DIFF-010](diff/010-color-screen-picker.md)。

### F23 图片助手

布局：顶部完整操作、左图片库、中画布、底部缩放；列表折叠、多选批量。

- 文件、剪贴板、Base64 导入导出、截图/区域截取；缩略图、名称/像素/大小、重命名/删除、复制/保存/批量导出、缩放/适配。
- 压缩和水印真实生成；水印文字/透明度/颜色/位置/字体尺寸/倾斜，保留原件或覆盖的输出方式清楚区分。
- **SVG 真矢量化**：预设 poster/photo/bw、颜色数 2–64（bw 不用）、detail low/medium/high、speckle 0–128；默认 poster/16/medium/8，批量与导出。
- 更换 tracer 可产生合理视觉差异，但参数必须映射到真实处理；输出含矢量 path，不在 SVG 内嵌原 bitmap 冒充。
- EXIF 方向、透明度、色彩、最大像素/内存、批量限制、取消及临时文件清理；持久图片库属于 Compose。
- 不添加 OCR。截图/取色按平台权限实现，尤其不能把黑图当截取成功。
- 验收：重启图片库、批量重命名/导出、水印可见、压缩改变文件、PNG alpha、截图多屏、SVG path/渲染、写失败不损坏原件。矢量化使用本产品内嵌 ImageTracer.java 1.1.2，而非 Electron `@visioncortex/vtracer`，见 [DIFF-013](diff/013-imagetracer-svg.md)。

### F24 PDF

Tab 顺序拆分、合并；任务表、文件、页数/大小、页码/规则、状态、最近输出。

- 同批最多 20 项；奇/偶/自定义拆分；按所选文件和页码范围顺序合并。
- 范围/分隔符/去重与排序语义读取 `next/src/shared/utils/pageRanges.ts`、`next/electron/main/pdfService.ts`。当前按 token 输入顺序展开范围并去重；拆分 custom 取候选页与自定义页的交集，保留候选顺序。不得自行排序破坏指定顺序。
- 真 PDF 页面操作，保留内容，不把页面截图后重造 PDF；表单/书签/签名等特殊对象的保留范围单独实测记录。
- 加密/损坏/越界/空范围/输出冲突、写失败明确错误；进度、取消与完成文件数准确。
- 验收：已知多页文档拆分→合并，检查页数/顺序/文本可提取；取消无半成品，打开输出路径真实存在。页面复制使用 PDFBox 3.0.4 `importPage`，而非 Electron `pdf-lib`，见 [DIFF-012](diff/012-pdfbox-import-page.md)。

### F25 系统信息

Tab：系统、CPU、内存、存储、网络；结构化字段/表格、刷新、复制、敏感字段脱敏。

- 使用真实 API/采集库；没有权限或平台不提供时显示不可用，不填 0 或固定数据。
- 容量单位、采样窗口、CPU 百分比、刷新周期定义明确；失活暂停无用轮询。
- JVM/应用信息和 OS 信息区分，序列号默认遮蔽，复制敏感值需明确动作。
- 验收：与本机基本信息核对、真实刷新、权限失败、页切换释放轮询和大表滚动。采集使用 OSHI 6.8.2，而非 Electron `systeminformation`，见 [DIFF-014](diff/014-oshi-system-info.md)。

## 3. 应用级功能

### A01 设置

保留 11 类和次序，自有 schema。所有有效设置必须真正影响运行结果；未支持项显示状态与原因，不保留假开关。

| 分类 | 字段/行为 |
| --- | --- |
| general | 中文/英文/日文、自动检查/下载更新、启动最大化、关闭 ask/hide/quit、托盘 |
| appearance | modern/quiet/hero/smartisan/miui-v5/claude 六风格、system/light/dark、六强调色、字体/字号、统一背景 |
| layout | 最近、紧凑、分隔线、隐藏标题、classic/card/grouped、自定义组/隐藏工具、面板尺寸 |
| editor | SQL 方言、JSON/随手记字体字号、软换行；适用操作实际读取这些设置 |
| network | 代理开关/地址/端口/用户名/密码、HTTP/翻译超时 |
| data | 实际数据路径、打开目录、备份/恢复、显式导入、报告 |
| vault | JSON/随手记路径、Git remote/用户名/token、自动提交/拉取、忽略规则、树展开策略 |
| runtime | Java/Groovy/Python/Node 路径、检测、草稿/参数/工作目录 |
| tools | QR 尺寸/纠错、随机长度、导出目录、默认翻译器与语言 |
| shortcuts | 搜索/设置键绑定、冲突检查、完整平台快捷键帮助 |
| about | Compose 产品名/版本、更新状态/下载校验、版本说明、许可和帮助 |

默认值：中文、system、modern、blue、UI 13、编辑器 14、classic、最近隐藏、软换行、关闭 ask、托盘开启（不可用时提示）、HTTP 30000 ms、翻译 15000 ms、QR 300/M、随机长度 16。Vault 自动行为先配置库再生效，不能对未选择目录启动后台 Git。

### A02 历史、收藏、命令搜索

- 通用历史按工具最多 200 条；支持搜索/详情/恢复输入选项/删除/清空。HTTP 和翻译专用历史单独定义，不误用全库 200 条裁剪。
- Regex/Cron/颜色收藏真实持久化，命名/分组/查询/恢复/删除按源页面清单实现；HTTP 集合和单词本不混入通用收藏。
- 历史不得默认存储私钥、密码、认证 Header；保留行为需有产品内明确选择和遮蔽策略，作为 Compose 隐私改进记录。
- 搜索本地名称/ID/关键词，完整键盘流程；导航隐藏或窗口分离不影响可搜索性。

### A03 桌面、存储、备份、Git、更新

全部遵循 [数据、平台与发布](data-platform-release.md)。这些不额外计入 25 工具，但缺少它们不能把项目标成完整产品。

## 4. 兼容样本格式

每项至少记录：caseId、toolId、sourceProduct/sourceCommit/sourceFile、输入、选项、期望结果/错误、比较方法、Compose 差异编号。保存到本产品 fixtures；CI 不启动其他版本来动态生成“期望值”。

比较方法：确定输出比文本/bytes；结构允许差异比 AST/协议内容；随机化加密/QR/PDF 用跨实现消费；随机数/系统数据比不变量；窗口/输入法比实际交互和截图。

源实现错误时保留 `sourceObserved` 与 `composeExpected` 及理由，不能覆盖原记录让测试迎合错误实现。原版未有的改进要标为设计决定，不回写成源事实。
