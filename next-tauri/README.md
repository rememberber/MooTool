# MooTool Next Tauri

MooTool Next Tauri 是基于 Tauri 2、Rust、React 和 TypeScript 的独立桌面产品线。它与 MooTool Java、MooTool Next Electron 独立安装、独立存储、独立发布和独立演进。

## 本地开发

前置条件：

- Node.js 20+
- Rust stable
- Tauri 对应平台的系统依赖

```bash
npm install
npm run dev
```

常用检查：

```bash
npm run typecheck
npm test
npm run check
npm run build:desktop
```

Calculator 已作为首个独立工具 WebView 接入：从首页或导航打开后，可在工具栏中分离到独立原生窗口、收回、关闭/重启，并执行 100 次状态保持压力测试。

JSON 已作为首个正式工作台接入独立工具 WebView，提供 CodeMirror 编辑、校验、格式化、压缩、键排序、JSONPath、结构统计、复制和字符串转义/还原。

JSON 工作台现已提供 Tauri 独立 Vault：用户显式选择根目录后，可浏览、新建、读取、冲突保护保存及可恢复删除 `.json` 文件；Rust 会拒绝绝对路径、路径穿越和符号链接逃逸，并对外部文件变更去抖通知。Vault Git 只暴露 init、pull、commit、push 固定操作，禁用 shell，具备超时、取消和未保存编辑保护；完整备份 v2 会携带 Vault 快照，恢复到 Tauri 自有导入目录而不覆盖原目录。

文本对比已作为正式独立工具 WebView 接入，提供双 CodeMirror 编辑器、Myers/Patience 差异计算、Unicode 词级高亮、忽略空白/大小写、上下文折叠、统一 Diff 复制，以及大文档降级和渲染上限。

格式化、编码解码和 Regex 已接入同一套 Tauri-owned 工具 WebView 生命周期。格式化覆盖 Nginx、Java、XML、HTML；编码解码覆盖 Unicode、URL（UTF-8/GB2312）、Base64、Hex 和 ASCII/Unicode 码点；Regex 提供五类标志、捕获组/命名组、常用表达式库及替换预览。

YAML/Properties、Cron、时间转换和 UA 分析同样作为正式独立工具交付：配置转换支持嵌套对象、数组路径和 Java Properties 转义；Cron 支持 Quartz 6/7 字段、自然语言说明、IANA 时区及未来执行时间；时间转换支持秒/毫秒、ISO/RFC 和 DST；UA 分析使用 MIT 许可的解析器识别浏览器、引擎、系统、设备和 Bot。

加解密/随机、二维码、Protobuf 和调色板已完成下一批正式交付：密码学工作台提供摘要/HMAC、带认证的 AES-256-GCM、PBKDF2 和系统安全随机；二维码支持 SVG 生成、样式配置、图片识别与原生文件导出；Protobuf 支持 proto2/proto3、JSON 与 Base64/Hex 双向转换、64 位整数保真和 Wire 检查；调色板支持常用格式、色阶及 WCAG 对比度。

随手记和留言板已经进入正式工具序列。随手记采用 Rust 管理的独立 SQLite 数据层，支持 650 ms 自动保存、全文搜索、查找替换、编辑/分栏/预览、置顶、复制、统计以及原生 Markdown/文本导入导出；它不复用 Electron 的外部文件树或 Git 工作区。留言板提供全屏文字演示、预设、主题、对齐、自动缩放和演示期间防休眠，并复用 SQLite 中的常用消息。开发期浏览器预览使用独立 localStorage 回退。工具收藏进入版本化设置并在侧栏实时呈现。

环境变量、硬件与系统、网络/IP 已作为系统侧批次交付：环境变量只读且由 Rust 在 IPC 返回前默认脱敏；系统快照提供 OS、Kernel、CPU、内存、进程和运行时长；网络工具提供 IPv4/CIDR 子网信息、地址分类、二进制与无符号整数转换。

Host、HTTP 和代码运行已作为原生能力批次交付：Host 提供 SQLite 配置档案、DNS 解析、系统 hosts 读取，以及带内容冲突检查和自动备份的显式写入；HTTP 客户端由 Rust 执行请求，支持参数、Header/Cookie、正文类型、1–120 秒超时、cURL 导入导出、重定向、流式进度、响应上限和取消，收藏、响应快照及最近 500 条历史进入独立 SQLite；代码运行器支持检测并调用本机 Java、Groovy、Python、Node.js，在无 shell 的受控子进程中流式返回输出、限制执行时长并支持取消。

翻译、图片和 PDF 已完成 1.0 正式工具闭环：翻译由 Rust 调用 Google/Bing 并自动故障切换，单词本和历史进入 SQLite；图片资产保存在 Tauri 自有目录，支持原生路径拖放、系统图片剪贴板、多显示器捕获与区域选择、压缩、水印、Base64 和原生文件/目录导出，批量导出不会覆盖已有同名文件；调色板可从隐藏窗口后的显示器快照中可视化取色；PDF 在本地 WebView 内完成页码选择、拆分和排序合并，生成结果以 512 KiB 分块写入 Rust 管理的临时文件，整批成功后再通过原生文件/目录选择提交，不上传用户文件。

工作台现已提供统一操作历史，并按设置上限在 SQLite 中自动裁剪；用户可创建、重命名、排序和删除 Tauri 自有工具分组。设置窗口可创建包含数据库、设置、图片库和 JSON Vault 的完整备份，也可从同产品备份恢复。Java/Electron 数据仅在用户明确选择来源目录并确认后只读扫描；导入前自动备份当前 Tauri 数据，在事务中写入独立数据库，跳过凭据并生成迁移报告，不修改来源产品。

独立 CI/发布流水线覆盖 macOS x64/arm64、Windows x64、Linux x64，使用 `next-tauri-v*` 标签和 Draft Pre-release，不修改 Java/Electron 发布。

桌面生命周期由 Rust Core 统一管理：主窗口几何和最大化状态写入 Tauri 独立配置文件；原生应用菜单与系统托盘支持显示主窗口、打开设置、隐藏到托盘和退出；关闭主窗口可选择每次询问、隐藏到托盘或直接退出。登录启动使用 Tauri 自有启动项并以隐藏模式进入托盘，不读取或覆盖 Java/Electron 的窗口与启动设置。

可失败的 Tauri Command 统一返回带错误码和重试语义的结构化错误，前端会归一化并去重上报。Rust Core 按日写入 Tauri 独立日志目录中的 JSON 日志并保留 14 天；设置窗口可以导出经过主机名、凭据和用户目录脱敏的诊断快照、窗口状态及受大小限制的日志，不会读取其他产品的数据或日志。

“设置 → 关于与更新”只读取根清单中的 `products.next-tauri`，再进入 Tauri 专属静态更新通道；不会读取 Electron 更新节点。更新下载提供进度与取消，安装包必须先通过 Tauri updater 签名验证。发布流水线为 macOS x64/arm64、Windows x64、Linux x64 生成独立安装包、更新产物、签名和 `latest.json`，人工发布 Pre-release 后才提升稳定通道并只激活根清单的 Tauri 节点。

设置基础设施由 Rust Core 独立实现：版本化 Schema、原子保存、损坏文件保留恢复、单实例设置窗口，以及主题、强调色、密度、三语言和编辑器默认值的跨 WebView 实时同步。设置写入 Tauri 应用 ID 对应的独立目录，不读取 Java/Electron 配置。

工具级三语言采用各功能独立的强类型消息目录，目录测试会校验中/英/日键集合、空文案和插值参数一致性；算法层以稳定错误码返回可本地化异常。25 个正式工具均已完成动态状态、确认提示、操作历史、示例内容及宿主加载态迁移，共享历史面板和原生 WebView 宿主也会跟随语言切换；覆盖测试会阻止新增正式工具遗漏三语目录，本地化边界检查会拒绝正式功能绕过目录写入 CJK 文案，且用户数据只保存稳定领域值，不写入某一种语言的展示文案。

“系统工具 → CodeMirror 实验台”提供第二个独立 WebView，用于验证系统 WebView 下的 Unicode 选区、查找、中文拼音 IME 和 reparent 状态保持；“WebView 实验台”继续提供低层会话探针。这些实现属于 Tauri 产品自己的纵向切片，不引用 Electron 工具源码。

正式 1.0 功能范围见 [`doc/feature-baseline.md`](doc/feature-baseline.md)，实现边界和分阶段计划见 [`doc/independent-product-implementation-plan.md`](doc/independent-product-implementation-plan.md)，原生桌面差异见 [`doc/adr/010-native-desktop-experience.md`](doc/adr/010-native-desktop-experience.md)，随手记数据模型见 [`doc/adr/011-quick-note-data-model.md`](doc/adr/011-quick-note-data-model.md)，发布操作见 [`doc/release-runbook.md`](doc/release-runbook.md)，兼容性实测记录见 [`doc/p0-validation.md`](doc/p0-validation.md)。
