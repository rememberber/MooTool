# MooTool Next Tauri 与 Next Electron 体验对照

> 更新日期：2026-09-17  
> 参照基线：`next-electron` 当前注册表（首页 + 25 工具）  
> 级别定义见 [`independent-product-implementation-plan.md`](./independent-product-implementation-plan.md) §3.2

本文记录 **用户可观察** 的差异与补齐状态，不以 Electron 源码或 IPC 为验收依据。

## 工具与核心流程（A/B）

| 域 | 状态 | 说明 |
| --- | --- | --- |
| 27 个正式工具 | 已对齐 | 注册表包含文件预览和批量重命名，见 [`feature-baseline.md`](./feature-baseline.md) |
| 工作台：首页、分组、搜索、最近、收藏、自定义分组 | 已对齐 | Tauri 使用 Rust 设置持久化。命令面板在工具之外提供 Electron 的 13 个动作：UUID、时间戳、JSON 格式化与压缩、Base64、URL、按行去重、剪贴板文本和 HTML、富文本转 Markdown、表格转 JSON。常用和最近执行单独保存为 `pinnedActionIds` 与 `recentActionIds`，最近最多 5 条，不写入设置文档 |
| 通用历史、HTTP/翻译历史、JSON Vault、备份/导入 | 已对齐 | 数据目录与格式独立，支持只读 Java/Electron 导入（含布局/隐藏工具 ID 映射）。操作历史固定保留最近 500 条，不再单独保存 `historyLimit`；旧值读取时忽略 |
| 工具 WebView 停靠/分离 | 已对齐 | Rust-owned 生命周期 |
| 代码运行、网络、代理、取消 | 代理改为 Electron 的开关、主机和字符串端口，默认关闭且端口为空。关闭时 HTTP 不走代理，翻译仍跟随系统环境；以前的手动代理按开启读取，未开启时的默认端口 8080 按空端口读取。HTTP 与翻译超时按毫秒保存，默认 30000 和 15000；以前的秒数会换算成毫秒。代码运行使用同一 HTTP 超时，不再单独保存秒数 | Rust 后端 |
| 截图/取色/图片剪贴板/防休眠 | 已对齐（含批准差异） | 见 [ADR-010](./adr/010-native-desktop-experience.md) |

## 布局与样式（B/C）

| 项目 | Electron | Tauri | 状态 |
| --- | --- | --- | --- |
| 导航样式 classic / card / grouped | 有，新安装默认 classic、显示分隔线、不显示最近使用 | 已补齐，新安装默认相同。编辑器 JSON 与随手记默认等宽 14px，翻译超时 15 秒，自动下载更新默认打开。按 Esc 隐藏窗口默认开启；弹窗、菜单和查找先处理 Esc，分离的工具窗口会收回主窗口。关闭主窗口的保存值是询问、隐藏或退出；以前保存的 minimizeToTray 会按隐藏读取。分栏宽度按 Electron 保存为比例数组，例如 `[0.2, 0.6, 0.2]`，键名使用 `http-workspace`、`runtime-editor-output`、`reformat-file`、`quick-note-tree-no-replace`、`json-two-pane` 和 `json-three-pane`。JSON 检查器默认关闭，只有打开文件后再展开才显示；两栏默认 190 和 800，最小 150px 和 360px，三栏默认 190、520 和 280，最小 150px、360px 和 220px。窗口窄于 1321px 时检查器会收起；以前的单个像素值会忽略，旧键名仍能读出。侧栏收起保存为 `hideNavigationTitles`，隐藏的工具保存为 `hiddenNavigationToolIds`；以前的 `sidebarCompact` 和 `hiddenTools` 会按这两个字段读取 | 设置 → 布局 |
| 紧凑导航间距、分组分隔线 | 有 | 已补齐 | 设置 → 布局 |
| 侧栏图标模式 | `hideNavigationTitles` / 紧凑 | `sidebarCompact` | 已对齐（等价交互） |
| 主题/强调色 | 有 | 已对齐。不再单独保存界面缩放或界面密度；紧凑间距只由 `compactNavigation` 控制，旧的 `uiScale` 和 `density` 读取时忽略 | 含 Electron 六色 + Tauri 扩展 indigo/teal/orange |
| 导航 classic/card/grouped + 紧凑/分隔 | `navigationStyle` 等 | 同上 | 2026-09-17 起与 Electron 同级选项 |
| 界面风格 modern / quiet / hero / claude / smartisan / miui-v5、统一背景 | 有 | 设置 → 外观 | 已对齐 |
| 界面字号 12–18 | 字号滑杆 | 设置 → 外观 | 已对齐 |
| 设置页分栏可调宽度 | `ResizableColumns` | 已补齐。键名 `settings-page`，默认 220 和 780，最小 180px 和 420px | 设置窗口 |
| 设置分类（布局/网络/Vault/快捷键/关于） | 12 类 | 已对齐信息架构。快捷键保存为应用内搜索、设置、全局开关和全局组合；以前只保存的 `globalSearch` 会按应用内搜索读取，全局组合默认 `CommandOrControl+Shift+Space` 且关闭。全局组合独立保存并立即注册，设置页显示启用、冲突、无效或注册失败状态，可恢复默认和重试 | 2026-09-17 拆分 |
| macOS 毛玻璃强度 | Chromium 半透明 | 原生标题栏 | **C 级**，ADR-010 |
| 沉浸式工具壳层类名 | `app-shell--immersive-tool` | 已补齐 | 工具页限制标题栏拖拽区域至侧栏宽度 |

## 功能差异（明确不做或后续）

| 项目 | 说明 |
| --- | --- |
| AI 集成（MCP/Skill 安装） | Electron 独有 | **批准差异**（[ADR-012](./adr/012-ai-integration-scope.md)） |
| 随手记 Vault 文件树 + Git 工作区 | Electron 文件模型；Tauri 桌面端使用同一 frontmatter 文件树，树节点路径是库内相对路径（如 `Work/API ideas.md`），图片写入 `attachments/`。Git 差异用左右只读编辑器显示原文，并同步滚动、按行和字符高亮。Git 面板对 `quick-notes` 目录执行 init、提交、获取、拉取、推送、丢弃和冲突处理，[ADR-011](./adr/011-quick-note-data-model.md)。笔记字体只保存 `font_name`，行距只保存 `line_spacing`；工具栏不再另给默认/等宽/衬线一档。以前的 `editor_font` 和 `line_height` 只在缺少字体名或行距时换算，之后不再写回文件。分栏预览按 `quick-note-editor-preview` 保存宽度，默认对半，最小都是 260px。打开快速替换后，文件树、编辑区和替换栏按 `quick-note-tree-replace` 保存三栏，默认约 220、560 和 218，最小 170px、320px 和 170px。编辑区可以隐藏笔记侧栏。侧栏打开且没有快速替换时按 `quick-note-tree-no-replace`，默认 220 和 780，最小 170px 和 320px。侧栏隐藏且打开快速替换时按 `quick-note-no-tree-replace`，默认 780 和 218，最小 320px 和 170px。侧栏隐藏且没有替换时只显示编辑区 |
| Vault Git 远程/自动拉取等完整设置页 | 设置 → Vault 含远程地址、用户名、令牌（系统钥匙串）、自动提交空闲/离席、自动拉取和隐藏 gitignore。自动提交在空闲或窗口失焦达到设置秒数后各执行一次；编辑器仍有未保存内容时跳过，失败会重试。随手记检查点会在需要时初始化仓库、跳过进行中的合并、提交变更，并在已配置远程时推送。JSON Vault 使用同一空闲调度，有变更才提交，已配置远程时再推送；合并、变基或冲突进行中时跳过检查点。初始化会写入同一份 `.gitignore` 并提交 “Initial MooTool Vault setup”；手动提交默认 “MooTool JSON checkpoint”，合并或冲突中拒绝提交，拉取前会先保存未写入磁盘的编辑，推送使用 `push -u origin HEAD`。合并或冲突时可以中止，冲突清完后可以继续；单个文件可以丢弃，冲突文件可以选择本地或远程版本。选择工作区文件或一次提交后，用左右只读编辑器显示修改前和修改后的文本；二进制文件和超过 512 KiB 的文件不展示正文。更多菜单可以重命名当前条目、导出选中 JSON（只写文件名，不覆盖已有文件）、刷新列表，并在文件管理器中打开 Vault 目录。文件树右键菜单可以复制片段、导出、重命名、移动、删除，并在文件管理器中定位该条目。条目可以拖到其他文件夹或列表空白处；不能拖进当前目录、自身或子目录。单击、Command/Ctrl 和 Shift 的多选规则与 Electron 相同；多选时可以一起复制或导出，重命名、移动、删除和定位只在选中一项时可用。复制会自动命名为「原名 Copy.json」，已存在时再加「 (2)」。JSON 列表和随手记一样跳过点开头的条目；设置里的「隐藏 .gitignore 匹配文件」默认打开，并同时作用于两边的文件树。JSON 和随手记的 Git 状态、提交文件列表都按 Electron 使用 `-z`，重命名和路径里的换行不会被拆开。JSON Git 侧栏只保留带变更数量的「打开 Git 面板」。面板内分成「未提交变更」和「提交历史」，远程地址在面板里保存或删除，提交说明写在变更标签中。自动拉取在编辑器有未保存内容、无远程、工作区不干净、正在合并或存在冲突时不执行。随手记和 JSON Vault 记住「展开全部 / 折叠全部」，默认展开全部；折叠时仍展开当前文件的上级目录。随手记可按最近修改、创建时间或文件名排序，搜索默认包含正文和路径，并可定位当前文件。随手记文件树使用同一套单击、Command/Ctrl 和 Shift 多选；多项时可以一起复制或导出，重命名、移动、删除和定位只在一项时可用。复制笔记的标题固定为「原标题 Copy」，文件名冲突时使用「 Copy (2)」，并沿用原来的语法扩展名。把笔记移到已有同名文件的文件夹时保留原文件名并改成「 (2)」，不会覆盖；目标里已有同名文件夹则拒绝移动。重命名保留原来的扩展名，标题可以含有会被文件名替换掉的字符；目标已有同名条目时拒绝重命名。删除文件夹会连同其中的笔记一起删除，不再把笔记移到根目录。删除笔记或从正文里去掉图片后，不再被任何笔记引用的附件文件也会删除。新建笔记会先询问名称，再在当前文件夹写成纯文本文件；标题里的斜杠只替换文件名，标题本身保留。新建文件夹同样只询问名称，并建在当前文件夹下；父目录不存在或同名条目已存在时会失败，不再用一条路径一次建出多层。移动改为从已有文件夹里选择目标，也可以选 Vault 根目录；重命名文件夹只改最后一级名称，斜杠会变成下划线。文档信息改成对话框，列出路径、创建时间、修改时间、行数、词数和字符数；空正文的行数是 0，字符按 UTF-16 码元计数。保存已有笔记时保留原来的文件名，改标题不会改名；只有语法变化才改扩展名，目标已存在则拒绝保存。没有路径的旧笔记会按所在文件夹和当前语法补上相对路径。导出选中笔记时先选择一个目录，按相对路径把斜杠换成连字符，只写正文为 `.txt`，不覆盖已有文件。工具栏「导出」也走同一条目录导出，不再把当前草稿另存成单个 `.md`。编辑器状态栏显示路径、是否已保存，以及「行数 · 字符数」。已打开的笔记在停止输入 250 毫秒后静默保存。选中文件夹会先保存当前笔记，再清空编辑区。没有打开笔记时，颜色、语法、字体、字号、行距、换行、格式化、列表、标签、附件、文档信息和快捷替换动作都会禁用。Ctrl 或 Command 加 F 或 R 打开查找替换。查找支持区分大小写、全词匹配和正则，可以在匹配项之间前后跳转并选中正文；替换当前项或全部，正则替换会展开 `\n`、`\t` 和捕获组。JSON 编辑器同样改用这套查找替换，不再打开 CodeMirror 自带搜索面板，并在正文里高亮匹配。编辑器底部状态栏显示当前文件路径和未保存圆点，右侧是校验图标和结果；操作提示显示在检查器的结果区，继续编辑正文后会清掉。JSON 解析失败时状态栏和操作提示直接显示 JSON.parse 的原文，不再改写成行号列号。文件预览的文本查找也改成可开关的查找栏，支持区分大小写和全词匹配，不提供正则或替换。导入文本会直接写成当前文件夹里的一条笔记。JSON 文件列表默认按名称排序，也可按修改时间；搜索匹配路径，勾选搜索正文后才读取不超过 20 MiB 的文件内容，并可定位当前文件。随手记语法保存为 Electron 的 MIME 值，例如 `text/plain`、`text/markdown` 和 `text/sql`；以前的短名称会按对应 MIME 读取 |
| SQL 方言等编辑器扩展项 | 设置 → 编辑器选择 Electron 的 10 种 SQL 方言；随手记 SQL 格式化使用该方言。JSON 与随手记编辑器字体保存为字体名，默认 `ui-monospace`；以前的 `mono` 读成 `ui-monospace`，`system` 读成 `system-ui`。界面字体同样保存为字体名，默认 `system-ui`。编辑器自动换行保存为 `softWrap`；以前的 `wordWrap` 会按它读取，通用字号和缩进宽度不再单独保存。共用编辑器不再挂上 CodeMirror 自带查找面板，查找仍由 JSON、随手记、Host、HTTP 和文件预览自己的查找栏处理 |
| 文本对比 | 左右原文编辑器按 Electron 的字符区间高亮，可切换整行/字符/双层，并排或三行上下文统一差异；草稿键 `mootool:text-diff:draft`。左右编辑器按 `text-diff` 对半分栏，最小各 240px |
| Regex | 草稿键 `mootool.regex.draft`。原文和匹配结果按 `regex-test` 保存分栏，默认约 710 比 290，最小 320px 和 220px。输入后 300ms 自动匹配，原文编辑器高亮匹配并支持上一个/下一个跳转。JavaScript 引擎在 Worker 中匹配，2 秒超时后终止。Java 引擎使用与 Electron 相同的 `MooToolRegex` 程序（stdin Base64、5000 条上限、5 秒超时）；浏览器预览无法启动 JDK |
| Cron | 字段支持每个值、范围、间隔、多选、最近工作日、月末、月末前 N 天（`L-n`）和每月第 N 个星期；表达式变化后 180ms 刷新说明和执行时间，描述语言可单独切换。编辑区和执行时间按 `cron-builder` 保存分栏，默认约 660 比 340，最小 460px 和 260px |
| 翻译 | Google / Bing 输入后自动翻译；DeepL 需在设置 → 工具默认值保存 API Key，并点击「翻译」发送，失败不切换引擎。粤语和文言文对 DeepL 不可选。标识符拆分与英语命名格式与 Electron 相同。原文和译文按 `translation-editor` 对半分栏，最小各 280px；词库按 `translation-words` 保存分栏，默认左栏约 220px。词条和历史使用整数 `id`、`YYYY-MM-DD HH:mm:ss` 时间；相同原文和语言对会更新原词条，搜索使用 SQL `LIKE` |
| 加解密 | 对称加密为 AES / DES / SM4，密文是十六进制；AES 密钥必须正好 16、24 或 32 个 UTF-8 字节。输入、操作和输出按 `crypto-symmetric` 保存三栏。非对称为 RSA（PKCS#1 v1.5、SHA-256 签名）和 SM2，公钥和私钥按 `crypto-key-pair` 对半分栏，最小都是 260px；明文和结果按 `crypto-asymmetric-data` 对半分栏，操作在下方。Base64 / Base32 按 `crypto-base` 保存三栏。对称和 Base 默认约 1 比 0.28 比 1，最小 240px、110px 和 240px。随机串规则与 Electron 相同。随机页同时显示 UUID、随机数字、随机字符串和随机密码。摘要页为 MD5 / SHA-1 / SHA-256 / SHA-384 / SHA-512 / SM3 文本摘要，结果单独一行；文件摘要在桌面端支持 MD5 / SHA-1 / SHA-256 / SHA-384 / SHA-512 / SM3 |
| 计算器 | 进制转换支持整数和可精确表示的小数（如 A.F → 10.9375），循环小数拒绝转换。IEEE 754 支持 32/64 位、大端/小端，HEX → DEC。运算区和结果按 `calculator-panels` 对半分栏，最小 360px 和 320px |
| 批量重命名 | 规则、预览冲突、硬链接改名和 `file-operations/batch-rename-journal.json` 撤销记录与 Electron 相同。选择普通文件，不包含目录和符号链接。浏览器预览不能改名 |
| 文件预览 | 文件类型识别、文本编码和 JSON 格式化与 Electron 相同：HTML/SVG 按文本打开，JSON 保留大整数和重复键，JSONC 允许注释。查找是字面量，不解释正则。图片可适应窗口、100% 显示，并以 1.25 倍在 10%–400% 之间缩放。文本可以开关自动换行。工具栏可以上一个、下一个文件，左右方向键同样切换；同一批文件里来回切换时，各自的编码、换行、查找、图片缩放和 PDF 页码会保留。PDF 在预览里按页渲染，可翻页、适应宽度，并以 1.25 倍在 25%–300% 之间缩放；加密 PDF 不预览 |
| 环境变量 | 页签为系统环境变量和 Electron 运行属性。范围是用户、系统、当前进程。用户变量写入 `~/.MooTool/environment`，赋值格式和四段导出与 Electron 相同。浏览器预览把同一份文本放在本地存储里 |
| 系统信息 | 页签为系统、处理器、内存、存储、网络。复制当前页签时使用 `========== 分组 ==========` 和 `字段: 值`。能读到的序列号会按 Electron 同样遮住中间字符，并显示制造商、型号、时区、CPU 供应商、性能核、能效核、活跃内存、磁盘类型和 MTU。读不到的字段不显示占位值 |
| 网络 | 工作区与 Electron 相同：左侧是 ifconfig / netstat 命令输出，右侧是 IPv4 与 Long 互转、PING、IP 段、端口扫描、域名解析、WHOIS、刷新 DNS 和本机地址。左右按 `network-workspace` 保存分栏，默认约 1.15 比 0.85，最小 340px 和 300px。`127.0.0.1` 为 `2130706433`。端口说明留空时使用常用端口表，支持 `22,80,3306` 和 `1-1024`（最多 4096 个），`80-1` 判定为无效。IP 段按三段前缀 Ping `.1`–`.254`，只列出有响应的地址；浏览器预览不能执行系统 ping 或端口扫描 |
| PDF | 页码按 Electron 的书写顺序保留，逗号和中文逗号都当作分隔符。空范围、`1;;2` 和倒序范围会报错。拆分文件名会把主文件名转成小写并加上 `_split.pdf` |
| 配置转换 | YAML 转 Properties 与 Electron 相同：纯量数组用逗号拼成一项，对象数组仍按下标展开。Properties 转 YAML 使用 4 空格缩进。左侧 Properties、中间操作、右侧 YAML 按 `config-convert` 保存三栏，默认约 1 比 0.28 比 1，最小 240px、110px 和 240px。另有 YAML 校验页，键名 `config-validate`，默认约 1、0.32、0.6，最小 240px、110px 和 220px；校验和格式化作用于独立的 YAML，不改转换页里的内容 |
| 代码格式化 | Java 使用 `prettier-plugin-java`，缩进、行宽 120 与 Electron 相同。代码运行里的 Java 也走同一格式化器；Groovy 和 Python 只整理空白。格式化原文和结果按 `reformat-file` 对半分栏，最小各 260px。代码运行的编辑区和输出按 `runtime-editor-output` 对半分栏，最小各 300px |
| 取色 | 当前颜色和色板按 `color-board` 保存分栏，默认约 0.34 比 0.66，最小 240px 和 420px。主题色阶和标准色与 Electron 的 Java 色表相同。颜色运算为反色、交集、相加、差值、平均；交集按通道 `floor(a * b / 255)` |
| 留言板 | 八条预设文案和主题与 Electron 相同，默认留言是「离开一会，马上回来」。字号范围 70–130，偏好存在 `mootool.message-board.preferences.v1` |
| Protobuf | 工作区分成 JSON / Binary、Wire 解析和 Hex / Base64。`.proto` 定义和 JSON/二进制转换按 `protobuf-json` 保存分栏，默认约 0.34 比 0.66，最小 240px 和 420px。Wire 解析和 Hex / Base64 各自是三栏，键名是 `protobuf-wire` 和 `protobuf-convert`，默认约 1、0.28、1，最小 240px、110px 和 240px。Wire 解析输出与 Electron 相同：`#1  field=1  wire_type=0 (Varint)  value=…`，可打印 UTF-8 显示为字符串。`.proto` 可按两空格缩进格式化 |
| User-Agent | 预设与 Electron 相同：Chrome Windows/macOS 131、Firefox Windows 133、Safari iPhone iOS 18.2、Chrome Android Pixel 8、curl。选择预设只填入文本，点击「解析」后才更新结果。输入和结果按 `ua-parser` 保存分栏，默认约 0.9 比 1.1，最小各 300px。结果行是浏览器、版本、引擎、引擎版本、系统、系统版本、设备类型、品牌、型号、移动端和爬虫，不显示 CPU。无法识别的客户端显示未知 |
| 编码 | 页签为 Native / Unicode、URL 转码、Native / 16 进制、Native / ASCII。左右编辑区和中间操作按 `encode-panes` 保存三栏，默认约 1 比 0.32 比 1，最小 240px、120px 和 240px。非 ASCII 转为 `\uXXXX`，补充平面字符写成代理项，不识别 `\u{...}`。ASCII 十六进制码点不带 `0x`，例如 `A中` 为 `41 4E2D`。无效的 `%` 序列按字面量保留。空的 Hex 输入会报错 |
| HTTP | 请求体格式化与 Electron 相同：JSON、XML、HTML、JavaScript 用 Prettier（JSON 缩进 2），纯文本只去掉行尾空白。复制 cURL 按地址原文导出，不把参数表写进 URL。收藏和历史使用 `t_msg_http` / `t_http_request_history`：整数 id、sqlite 时间、按名称更新、收藏只按名称 LIKE、历史保留最近 500 条并显示原始状态、耗时和创建时间。响应区查找栏支持区分大小写、全词和正则，可前后跳转并高亮，不提供替换；请求体和响应都自动换行，Shift+Ctrl/Cmd+F 格式化请求体。地址栏可以「发送并打开响应窗口」。已有响应时也可以打开独立响应窗口，桌面端是 1000×760 的新窗口，最小 560×400；窗口里可以切换响应体、响应头和 Cookie，并查找、复制当前页。收藏和编辑区按 `http-workspace` 保存分栏，默认 230 和 770，最小 180px 和 420px |
| 二维码 | 页签为生成、识别、历史。点击「生成二维码」后才输出 PNG：边距 4、前景 `#111111`、背景 `#FFFFFF`，尺寸限制在 120–2000。生成页按 `qrcode-generate` 对半分栏，识别页按 `qrcode-recognize` 对半分栏，最小都是 300px 和 280px。识别先尝试 TRY_HARDER，失败后再按纯条码解码。历史记录可回到对应页签 |
| 时间转换 | 快捷时区与 Electron 相同（UTC、+8、+9、-5、-8、+1、+3）。当前时间带可打开本工具历史和大屏时钟；大屏显示时区、时刻和日期，Esc 或关闭按钮退出 |
| Host 查找替换 | 与 Electron 相同：不区分大小写、区分大小写、全词、正则、上一处/下一处循环，以及当前替换和全部替换（正则替换支持 `$1` 与 `\n`）。配置列表和编辑器按 `host-workspace` 保存分栏比例，默认 220 和 780，最小 170px 和 360px。配置使用整数 `id`、`YYYY-MM-DD HH:mm:ss` 时间和按名称更新；列表按修改时间倒序，搜索使用 SQL `LIKE`。右键可重命名、导出和删除 |
| JSON 字符串转义 | JSON 字符串转义会包上引号；字符串转义只转义 `\ " \n \r \t \b \f`，反转义允许原文里的裸引号和 `\u` |
| 图片调整尺寸 | 与 Electron 相同：等比适应、裁剪填充、拉伸，可只缩小，缺一边时按比例计算，单边不超过 16384、总像素不超过 3200 万。图片库和预览按 `image-library` 保存分栏，默认左栏约 230px，最小 180px 和 360px。工具栏可以显示或隐藏图片列表；隐藏后预览占满工作区，已保存的列表宽度保持不变。压缩、水印和调整尺寸都可以保留原图或覆盖；格式变化导致文件名改变时，覆盖会删除原文件，重名新文件自动编号。macOS 13 及以上的「截图识字」先框选区域，再用本机 Vision 识别，语言为自动、简体、繁体、英文和日文；浏览器预览和其他系统显示不可用 |
| 自动更新 UI 历史版本链 | 公开 RC 边界见 [`platform-acceptance.md`](./platform-acceptance.md) |

## 平台与验收（D / 人工）

| 项目 | 状态 |
| --- | --- |
| 四平台 CI + 25 工具视觉门禁 | 自动化已建立 |
| Windows/Linux 高 DPI、IME、权限人工矩阵 | 多数为 **未验证**，见 platform-acceptance |
| Linux Wayland Portal 截图/取色 | **批准降级** |
| 跨显示器单选区 | **批准降级** |

## 维护约定

- 新增 C/D 级差异必须先更新本文或 ADR，再改产品行为。  
- 不以 Electron `toolRegistry.status` 字段作为 Tauri 交付依据；以本文与 `feature-baseline.md` 为准。

## 仓库内验证命令（2026-09-17）

在 `next-tauri/` 目录执行：

| 门禁 | 命令 | 证明范围 |
| --- | --- | --- |
| 功能清单 | `npm test -- src/app/toolCatalog.test.ts` | 25 正式工具与 lab 隔离 |
| Parity 元数据 | `npm run check:parity` | 对照文档、Schema 一致、布局字段 |
| 单元/契约 | `npm test` | 前端 151+ 项 |
| 视觉/布局 | `npm run test:e2e -- tests/e2e/visual.spec.ts` | 25 工具 × 视觉矩阵 + 设置/壳层 |
| Electron 导入 | `cargo test parses_only_safe_settings_and_skips_secrets --manifest-path src-tauri/Cargo.toml` | 布局与工具 ID 映射 |
| Rust 全量 | `cargo test --manifest-path src-tauri/Cargo.toml` | 原生契约与命令 |

**未纳入自动化**：Windows/Linux 高 DPI、IME、Wayland 人工矩阵（见 `platform-acceptance.md`）；Electron 等价 AI/MCP 安装（见 ADR-012）。

## 对齐完成定义（产品验收口径）

在 **不复制 Electron 源码** 的前提下，下列项视为与 Electron **体验对齐** 的完成标准：

1. [`feature-baseline.md`](./feature-baseline.md) 中正式工具均为 `ready`，且 `toolCatalog.test.ts` 通过。
2. 本文 **A/B 级** 表格中标记为「已对齐 / 已补齐」的条目在代码与设置 Schema 中可核对。
3. **C/D 级** 与 **ADR-012 AI** 差异已书面批准，不记为 Tauri 1.0 阻塞。
4. 上节验证命令在本仓库当前分支可重复执行并通过（e2e 允许 Playwright 配置的重试，但不得长期依赖 flaky）。

| 日期 | 提交/工作区 | `check:parity` | `npm test` | `test:e2e visual` | 备注 |
| --- | --- | --- | --- | --- | --- |
| 2026-09-17 | 工作区 | pass | 151 passed | 107 passed ×2（无 flaky） | e2e 等待 theme/QR/system 就绪；浏览器预览 uptime 固定 |
