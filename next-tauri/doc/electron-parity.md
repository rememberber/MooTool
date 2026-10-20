# MooTool Next Tauri 与 Next Electron 体验对照

> 更新日期：2026-09-17  
> 参照基线：`next-electron` 当前注册表（首页 + 25 工具）  
> 级别定义见 [`independent-product-implementation-plan.md`](./independent-product-implementation-plan.md) §3.2

本文记录 **用户可观察** 的差异与补齐状态，不以 Electron 源码或 IPC 为验收依据。

## 工具与核心流程（A/B）

| 域 | 状态 | 说明 |
| --- | --- | --- |
| 25 个正式工具 | 已对齐 | 注册表一致，见 [`feature-baseline.md`](./feature-baseline.md) |
| 工作台：首页、分组、搜索、最近、收藏、自定义分组 | 已对齐 | Tauri 使用 Rust 设置持久化 |
| 通用历史、HTTP/翻译历史、JSON Vault、备份/导入 | 已对齐 | 数据目录与格式独立，支持只读 Java/Electron 导入（含布局/隐藏工具 ID 映射） |
| 工具 WebView 停靠/分离 | 已对齐 | Rust-owned 生命周期 |
| 代码运行、网络、代理、取消 | 已对齐 | Rust 后端 |
| 截图/取色/图片剪贴板/防休眠 | 已对齐（含批准差异） | 见 [ADR-010](./adr/010-native-desktop-experience.md) |

## 布局与样式（B/C）

| 项目 | Electron | Tauri | 状态 |
| --- | --- | --- | --- |
| 导航样式 classic / card / grouped | 有 | 已补齐 | 设置 → 布局 |
| 紧凑导航间距、分组分隔线 | 有 | 已补齐 | 设置 → 布局 |
| 侧栏图标模式 | `hideNavigationTitles` / 紧凑 | `sidebarCompact` | 已对齐（等价交互） |
| 界面密度、UI 缩放、主题/强调色 | 有 | 已对齐 | 含 Electron 六色 + Tauri 扩展 indigo/teal/orange |
| 导航 classic/card/grouped + 紧凑/分隔 | `navigationStyle` 等 | 同上 | 2026-09-17 起与 Electron 同级选项 |
| 界面风格 modern / quiet / hero / claude / smartisan / miui-v5、统一背景 | 有 | 设置 → 外观 | 已对齐 |
| 界面字号 12–18 | 字号滑杆 | 设置 → 外观 | 已对齐 |
| 设置页分栏可调宽度 | `ResizableColumns` | 已补齐 | 设置窗口 |
| 设置分类（布局/网络/Vault/快捷键/关于） | 12 类 | 已对齐信息架构 | 2026-09-17 拆分 |
| macOS 毛玻璃强度 | Chromium 半透明 | 原生标题栏 | **C 级**，ADR-010 |
| 沉浸式工具壳层类名 | `app-shell--immersive-tool` | 已补齐 | 工具页限制标题栏拖拽区域至侧栏宽度 |

## 功能差异（明确不做或后续）

| 项目 | 说明 |
| --- | --- |
| AI 集成（MCP/Skill 安装） | Electron 独有 | **批准差异**（[ADR-012](./adr/012-ai-integration-scope.md)） |
| 随手记 Vault 文件树 + Git 工作区 | Electron 文件模型；Tauri 桌面端使用同一 frontmatter 文件树，树节点路径是库内相对路径（如 `Work/API ideas.md`），图片写入 `attachments/`。Git 差异用左右只读编辑器显示原文，并同步滚动、按行和字符高亮。Git 面板对 `quick-notes` 目录执行 init、提交、获取、拉取、推送、丢弃和冲突处理，[ADR-011](./adr/011-quick-note-data-model.md) |
| Vault Git 远程/自动拉取等完整设置页 | 设置 → Vault 含远程地址、用户名、令牌（系统钥匙串）、自动提交空闲/离席、自动拉取和隐藏 gitignore |
| SQL 方言等编辑器扩展项 | 设置 → 编辑器选择 Electron 的 10 种 SQL 方言；随手记 SQL 格式化使用该方言 |
| 文本对比 | 左右原文编辑器按 Electron 的字符区间高亮，可切换整行/字符/双层，并排或三行上下文统一差异；草稿键 `mootool:text-diff:draft` |
| Regex | 草稿键 `mootool.regex.draft`；输入后 300ms 自动匹配，原文编辑器高亮匹配并支持上一个/下一个跳转。JavaScript 引擎在 Worker 中匹配，2 秒超时后终止。Java 引擎使用与 Electron 相同的 `MooToolRegex` 程序（stdin Base64、5000 条上限、5 秒超时）；浏览器预览无法启动 JDK |
| Cron | 字段支持每个值、范围、间隔、多选、最近工作日、月末、月末前 N 天（`L-n`）和每月第 N 个星期；表达式变化后 180ms 刷新说明和执行时间，描述语言可单独切换 |
| 翻译 | Google / Bing 输入后自动翻译；DeepL 需在设置 → 工具默认值保存 API Key，并点击「翻译」发送，失败不切换引擎。粤语和文言文对 DeepL 不可选。标识符拆分与英语命名格式与 Electron 相同 |
| 加解密 | 对称加密为 AES / DES / SM4，密文是十六进制；AES 密钥必须正好 16、24 或 32 个 UTF-8 字节。非对称为 RSA（PKCS#1 v1.5、SHA-256 签名）和 SM2。Base64 / Base32 与随机串规则与 Electron 相同。随机页同时显示 UUID、随机数字、随机字符串和随机密码。摘要页为 MD5 / SHA-1 / SHA-256 / SHA-384 / SHA-512 / SM3 文本摘要，结果单独一行；文件摘要在桌面端支持 MD5 / SHA-1 / SHA-256 / SHA-384 / SHA-512 / SM3 |
| 计算器 | 进制转换支持整数和可精确表示的小数（如 A.F → 10.9375），循环小数拒绝转换。IEEE 754 支持 32/64 位、大端/小端，HEX → DEC |
| 文件预览 | 文件类型识别、文本编码和 JSON 格式化与 Electron 相同：HTML/SVG 按文本打开，JSON 保留大整数和重复键，JSONC 允许注释。查找是字面量，不解释正则。图片可适应窗口、100% 显示，并以 1.25 倍在 10%–400% 之间缩放。PDF 页面渲染仍走 PDF 工具 |
| 系统信息 | 页签为系统、处理器、内存、存储、网络。复制当前页签时使用 `========== 分组 ==========` 和 `字段: 值`。序列号、制造商、性能核等 Electron 才采集的字段这里没有对应数据 |
| 网络 | 工作区与 Electron 相同：左侧是 ifconfig / netstat 命令输出，右侧是 IPv4 与 Long 互转、PING、IP 段、端口扫描、域名解析、WHOIS、刷新 DNS 和本机地址。`127.0.0.1` 为 `2130706433`。端口说明留空时使用常用端口表，`80-1` 判定为无效。IP 段按三段前缀展开为 .1–.254；浏览器预览不能执行系统 ping |
| PDF | 页码按 Electron 的书写顺序保留，逗号和中文逗号都当作分隔符。空范围、`1;;2` 和倒序范围会报错。拆分文件名会把主文件名转成小写并加上 `_split.pdf` |
| 配置转换 | YAML 转 Properties 与 Electron 相同：纯量数组用逗号拼成一项，对象数组仍按下标展开。Properties 转 YAML 使用 4 空格缩进 |
| 代码格式化 | Java 使用 `prettier-plugin-java`，缩进、行宽 120 与 Electron 相同。代码运行里的 Java 也走同一格式化器；Groovy 和 Python 只整理空白 |
| 取色 | 主题色阶和标准色与 Electron 的 Java 色表相同。颜色运算为反色、交集、相加、差值、平均；交集按通道 `floor(a * b / 255)` |
| 留言板 | 八条预设文案和主题与 Electron 相同，默认留言是「离开一会，马上回来」。字号范围 70–130，偏好存在 `mootool.message-board.preferences.v1` |
| Protobuf | Wire 解析输出与 Electron 相同：`#1  field=1  wire_type=0 (Varint)  value=…`，可打印 UTF-8 显示为字符串。`.proto` 可按两空格缩进格式化 |
| User-Agent | 预设与 Electron 相同：Chrome Windows/macOS 131、Firefox Windows 133、Safari iPhone iOS 18.2、Chrome Android Pixel 8、curl。选择预设只填入文本，点击「解析」后才更新结果。结果行是浏览器、版本、引擎、引擎版本、系统、系统版本、设备类型、品牌、型号、移动端和爬虫，不显示 CPU。无法识别的客户端显示未知 |
| 编码 | 页签为 Native / Unicode、URL 转码、Native / 16 进制、Native / ASCII。非 ASCII 转为 `\uXXXX`，补充平面字符写成代理项，不识别 `\u{...}`。ASCII 十六进制码点不带 `0x`，例如 `A中` 为 `41 4E2D`。无效的 `%` 序列按字面量保留。空的 Hex 输入会报错 |
| HTTP | 请求体格式化与 Electron 相同：JSON、XML、HTML、JavaScript 用 Prettier（JSON 缩进 2），纯文本只去掉行尾空白。复制 cURL 按地址原文导出，不把参数表写进 URL |
| 二维码 | 页签为生成、识别、历史。点击「生成二维码」后才输出 PNG：边距 4、前景 `#111111`、背景 `#FFFFFF`，尺寸限制在 120–2000。识别先尝试 TRY_HARDER，失败后再按纯条码解码。历史记录可回到对应页签 |
| 时间转换 | 快捷时区与 Electron 相同（UTC、+8、+9、-5、-8、+1、+3）。当前时间带可打开本工具历史和大屏时钟；大屏显示时区、时刻和日期，Esc 或关闭按钮退出 |
| Host 查找替换 | 与 Electron 相同：不区分大小写、区分大小写、全词、正则、上一处/下一处循环，以及当前替换和全部替换（正则替换支持 `$1` 与 `\n`）。配置列表可只搜名称或连正文一起搜，右键可重命名、导出和删除 |
| JSON 字符串转义 | JSON 字符串转义会包上引号；字符串转义只转义 `\ " \n \r \t \b \f`，反转义允许原文里的裸引号和 `\u` |
| 图片调整尺寸 | 与 Electron 相同：等比适应、裁剪填充、拉伸，可只缩小，缺一边时按比例计算，单边不超过 16384、总像素不超过 3200 万。压缩、水印和调整尺寸都可以保留原图或覆盖；格式变化导致文件名改变时，覆盖会删除原文件，重名新文件自动编号 |
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

1. [`feature-baseline.md`](./feature-baseline.md) 中 25 个正式工具均为 `ready`，且 `toolCatalog.test.ts` 通过。
2. 本文 **A/B 级** 表格中标记为「已对齐 / 已补齐」的条目在代码与设置 Schema 中可核对。
3. **C/D 级** 与 **ADR-012 AI** 差异已书面批准，不记为 Tauri 1.0 阻塞。
4. 上节验证命令在本仓库当前分支可重复执行并通过（e2e 允许 Playwright 配置的重试，但不得长期依赖 flaky）。

| 日期 | 提交/工作区 | `check:parity` | `npm test` | `test:e2e visual` | 备注 |
| --- | --- | --- | --- | --- | --- |
| 2026-09-17 | 工作区 | pass | 151 passed | 107 passed ×2（无 flaky） | e2e 等待 theme/QR/system 就绪；浏览器预览 uptime 固定 |
