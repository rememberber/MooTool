# MooTool Next：Java 版完整对齐迁移计划

> 状态：P1-P7 实现完成；Java 人工视觉签收、远端跨平台包与正式签名待验收
> 更新日期：2026-07-16
> 目标技术栈：TypeScript + React + Vite + Electron  
> 功能基线：当前 Java 版 MooTool 代码、README 功能树与实际可运行行为

## 1. 项目目标

MooTool Next 不再以“精选 MVP”作为当前阶段目标，而是以 Java 旧版的完整功能和主要交互布局为基线，完成 TypeScript、React、Electron 的全量重写。

本阶段必须同时满足以下目标：

1. **功能一致**：除本文明确批准的对齐例外外，Java 版已经可用的功能、子功能、历史记录、收藏、导入导出、快捷键和设置项都需要在 Next 中提供。
2. **布局尽量一致**：保留旧版工具页的信息层级、主要分区、操作顺序、Tab、工具栏、历史区和弹窗流程。
3. **桌面体验现代化**：使用当前已经确认的 Electron 左侧栏、macOS 风格窗口、明暗主题和品牌视觉，不机械复制 Swing 控件外观。
4. **数据可迁移**：旧版用户数据、SQLite 历史、配置、Vault 和收藏应尽可能无损迁移或直接兼容。
5. **跨平台一致**：macOS、Windows、Linux 的核心功能一致；平台能力差异必须有清晰降级和提示。
6. **国际化完整**：至少保持简体中文、英文、日文三种语言，新增界面不得硬编码用户可见文案。

旧版代码或 README 中明确标记为 TODO 且实际尚不可用的能力，不计入首轮 parity；它们进入对齐完成后的增强列表。例如当前配置转换中的 JSON/YAML 双向转换 TODO。

已确认的对齐例外：**图片助手中的 OCR 不迁移到 Next**。Next 不建设 OCR 引擎、Provider、模型下载或相关设置；截图、压缩、水印和图片导入导出等其余图片助手能力仍按 Java 版对齐。未来如重新评估 OCR，应作为独立增强需求立项，不作为本轮 parity 和发布阻塞项。

## 2. 对齐原则

### 2.1 功能真源

功能判断按以下优先级执行：

1. Java 版当前实际可运行行为。
2. `README.md` 中的 MooTool feature map。
3. `FuncTabCatalog.java`、各 `*Form.java`、Listener、Dialog、Service、Util 和数据库脚本。
4. 旧版截图、菜单、快捷键和 i18n 文案。

若 README 与代码不一致，以实际可运行代码为准，并在 parity 矩阵中记录差异。

### 2.2 布局对齐

- 当前已确认的主窗口结构继续使用：左侧工具导航、主工作区、顶部透明拖拽区。
- 左侧导航需恢复 Java 版 24 个工具、6 个内置分组、搜索、最近使用和可配置显示方式。
- 每个功能页内部以 Java 版布局为起点：分区、工具栏顺序、Tab 顺序、输入输出关系和弹窗层级尽量一致。
- 允许用 React 控件替代 Swing 控件，但不得因“视觉现代化”删除操作、合并关键流程或隐藏高级能力。
- 对旧版明显拥挤的布局，可以做响应式重排；桌面宽屏下仍应保持旧版的操作心智模型。
- 每个工具都需要保存旧版与 Next 的对比截图，作为布局验收证据。

### 2.3 行为对齐

- 相同输入应得到相同或等价输出。
- 校验规则、默认值、错误状态、复制、清空、导入、导出和历史写入时机应保持一致。
- 快捷键按平台映射：macOS 使用 `Command`，Windows/Linux 使用 `Ctrl`。
- 涉及系统权限的能力必须在主进程执行，并给出与旧版等价的失败提示。

## 3. 当前状态

### 3.1 已完成

- Electron + Vite + React + TypeScript 工程骨架。
- 隐藏式标题栏和全窗口顶部拖拽热区。
- 左侧导航与主工作区基础布局。
- 简体中文、英文、日文 i18n 基础设施。
- 明暗主题跟随系统。
- MooTool 正式 AppIcon、favicon 和页面品牌图标。
- Tooltip、Toast 通用反馈基础设施。
- JSON 格式化、压缩、换行、复制、清空、转义、还原和基础校验原型。
- 主页 + 24 个旧版工具的 `toolRegistry`、6 个内置分组和统一懒加载入口。
- Zustand 工作区状态、全局工具搜索、最近 5 个工具和最后页面持久化。
- 版本化 `AppSettings`、`electron-store`、typed preload IPC 和跨窗口设置广播。
- 设置模态子窗口及常规、外观、布局、编辑器、网络、数据、Vault、运行时、工具默认值、快捷键、关于分类。
- Java、Groovy、Python、Node.js 本机运行时检测和自定义路径设置。
- `safeStorage` 敏感信息存储，代理密码和 Git Token 不以明文进入普通配置。
- 应用菜单、`Command/Ctrl + K`、设置快捷键、窗口状态、关闭策略和系统托盘。
- Vitest 单元测试、Electron Playwright E2E 和 React Doctor 质量门槛。
- 基于 `node:sqlite` 的 Java 兼容 `t_func_history` Repository，支持搜索、应用、复制、删除、清空和单工具 200 条上限。
- JSON 排序、忽略大小写、重复 Key、Key/Value 互换、JSON/XML、JavaBean、JSONPath 和可视化路径选择。
- JSON 查找、导入导出、历史记录和响应式高级工具面板。
- 受限文件边界的 JSON Vault，支持嵌套路径、打开、新建、保存、删除和未保存保护。
- JSON Vault Git 面板，支持状态、初始化、远程配置、提交、fetch/pull/push、历史和 diff；Token 经 `safeStorage` + `GIT_ASKPASS` 注入。
- JSON 的 1440 明暗、1080 最小窗口、JSONPath 弹窗和 Git 面板视觉基线。
- P3 八个纯本地工具已完成 TS/React 实现：时间转换、编码解码、UA 分析、计算器、正则、Cron、文本 Diff、Properties/YAML 配置转换。
- P3 已接入通用 Tool Page、Tab、历史、SQLite 收藏、Tooltip、Toast、i18n 和系统明暗主题，并完成单元、Electron E2E 与视觉回归验证。
- P4 七个文件、媒体与密码学工具已完成：格式化、加解密/随机、二维码、调色板、图片助手、PDF、Protobuf。
- P4 已接入图片仓库、截图/剪贴板、文件摘要、PDF 处理等 typed Electron IPC，并完成三语言、明暗主题、最小窗口和视觉基线。
- P5 六个网络与系统工具已完成：HTTP 请求、翻译、Host、网络/IP、环境变量、系统信息。
- P5 已接入统一代理/超时/取消、Java 兼容 SQLite 表、平台 Hosts 提权、白名单系统命令和结构化硬件采集，并完成三语言、明暗主题、最小窗口和视觉基线。
- P6 Quick Note 已完成 Java 兼容文本 Vault、Markdown、图片、搜索、树操作、导入导出、快速替换、Git、自动保存和文件监视闭环。
- P6 代码运行已完成 Java/Groovy、Python、Node.js 独立草稿、运行参数、工作目录、检测、格式化、执行、停止、流式输出和历史闭环。
- P6 已在设置中接入本地完整/分类备份、存储位置和运行时路径，完成 schema v3 迁移、三语言、明暗主题与最小窗口验证。
- P7 已补齐 JSON Vault/Quick Note 高级树操作、文件监视及 Git 冲突、撤销、中止合并和自动化。
- P7 已完成 Java SQLite、配置、Quick Note、JSON Vault、收藏、HTTP 和翻译数据的只读扫描、备份、事务迁移、幂等与回滚。
- P7 已完成更新检查、关于页面、托盘和关闭行为验收；Next 首个正式版本重置为 `1.0.0`，Java 基线仍为 `1.7.8`。
- P7 已完成 CodeMirror 大 JSON 优化、打包 App 性能门槛、macOS ARM64 DMG/ZIP 生成与启动验证。
- P7 已建立 macOS Apple Silicon/Intel、Windows x64、Linux x64 手动 CI 安装包矩阵。

### 3.2 尚未完成

- 24 个工具仍需用 Java 版同尺寸截图完成最终人工布局签收；Next 明暗和最小窗口自动视觉基线已具备。
- macOS Intel、Windows x64、Linux x64 安装包矩阵已定义，仍需在 GitHub Runner 实际执行并安装验收。
- Apple Developer ID、公证/stapling 与 Windows 代码签名依赖发布凭证，当前本机包仅用于内部候选验收。
- 翻译、代理和远程 Git 需要发布环境真实凭证与网络条件做最终在线验收。

## 4. 目标架构

```mermaid
flowchart LR
  UI[React Renderer] --> Store[Zustand App Store]
  UI --> Registry[Tool Registry]
  UI --> IPC[Typed Preload API]
  IPC --> Main[Electron Main]
  Main --> Settings[Settings Service]
  Main --> DB[SQLite Repositories]
  Main --> FS[File and Vault Services]
  Main --> OS[OS / Shell / Tray / Clipboard]
  Main --> Network[HTTP / Translation / Update]
  Settings --> Prefs[electron-store]
  Settings --> Secrets[Electron safeStorage]
  DB --> Legacy[Legacy MooTool.db Migration]
```

### 4.1 Renderer

建议目录：

```text
src/
├── app/
│   ├── App.tsx
│   ├── toolRegistry.ts
│   ├── appStore.ts
│   └── shortcuts.ts
├── features/
│   ├── settings/
│   ├── search/
│   ├── history/
│   ├── json/
│   └── <tool-id>/
└── shared/
    ├── components/
    ├── editor/
    ├── feedback/
    ├── i18n/
    ├── layout/
    └── theme/
```

### 4.2 工具注册中心

`toolRegistry` 是导航、搜索、首页快捷入口、最近记录、快捷键和页面渲染的唯一数据源。

每个工具至少包含：

```ts
type ToolDefinition = {
  id: ToolId
  groupId: ToolGroupId
  titleKey: MessageKey
  keywords: Record<Language, string[]>
  icon: LucideIcon
  component: LazyExoticComponent<ComponentType>
  status: 'placeholder' | 'in-progress' | 'parity-review' | 'complete'
  supportsHistory: boolean
  supportsFavorites: boolean
  settingsSection?: string
}
```

工具页面通过注册表加载，不再在 `Workbench` 中增加 `activeTool === ...` 条件分支。

### 4.3 状态边界

- React 本地状态：输入框、临时选中项、尚未保存的表单。
- Zustand：当前工具、搜索状态、最近访问、窗口级 UI 状态。
- `electron-store`：用户设置、最近工具、窗口尺寸、布局偏好和轻量状态。
- SQLite：历史、收藏、HTTP 请求、翻译记录、单词本等结构化业务数据。
- 文件系统：Quick Note Vault、JSON Vault、导入导出文件、图片、PDF 和备份。
- `safeStorage`：代理密码、Git Token 等敏感数据；不得以明文放进 renderer 或普通配置文件。

### 4.4 IPC 约束

- Renderer 不直接访问 Node.js、文件系统、数据库和系统命令。
- Preload 只暴露明确的类型安全 API，不暴露通用 `invoke(channel, payload)`。
- IPC 按域划分：`settings`、`history`、`files`、`vault`、`network`、`runtime`、`media`、`system`、`update`、`window`。
- 所有入参在主进程校验；路径、命令、URL 和文件扩展名必须做边界检查。
- 长耗时任务支持进度、取消和错误码，不依赖无法取消的单次 Promise。

## 5. 设置中心设计

设置不是迁移末尾的附属页面，而是所有工具和平台能力的基础依赖，应在第一阶段建立完整模型。

### 5.1 展示方式

- 保持 Java 版“设置对话框”的交互心智，使用单实例、模态的 Electron 子窗口。
- 设置窗口内部采用左侧分类或锚点导航 + 右侧滚动表单；分类顺序尽量对应旧版设置分区。
- 主窗口底部设置按钮、应用菜单和快捷键打开同一个设置窗口。
- 设置窗口复用同一套 i18n、主题、控件和 Toast，但拥有独立窗口尺寸与拖拽区。

### 5.2 设置分类

| 分类 | 必须对齐的设置 | 应用方式 |
| --- | --- | --- |
| 常规 | 语言、启动时检查更新、默认最大化、关闭窗口行为、系统托盘 | 部分即时，部分重启 |
| 外观 | 跟随系统主题、主题、强调色、跟随系统强调色、全局字体、全局字号、统一背景 | 尽量即时预览 |
| 布局与习惯 | 菜单/工具栏位置、功能导航位置、显示最近使用、紧凑、隐藏标题、分割线、经典/卡片/分组导航 | 即时应用 |
| 编辑器 | SQL 方言、Quick Note 字体字号、JSON 字体字号、软换行等 | 即时应用 |
| 网络 | HTTP 代理开关、Host、端口、用户名、密码 | 显式保存并测试 |
| 数据与备份 | 数据目录、导入导出、SQLite 迁移、Git 同步、自动同步 | 危险操作需确认 |
| Vault | Quick Note Vault、JSON Vault、远程仓库、自动提交、自动拉取、忽略文件、展开方式 | 显式保存 |
| 运行环境 | 自动探测 Java、Python、Node.js，显示路径与版本，允许覆盖可执行文件路径并测试 | 显式保存并测试 |
| 工具默认值 | QR 尺寸/纠错级别/Logo、随机字符串长度、导出目录等 | 即时或显式保存 |
| 快捷键 | 全局工具搜索、设置、页面操作、工具特定快捷键 | 冲突检测后保存 |
| 关于与更新 | 版本、更新检查、项目链接、许可证 | 即时操作 |

### 5.3 设置 Schema

设置必须使用带版本号的统一 Schema，例如：

```ts
type AppSettings = {
  schemaVersion: number
  general: GeneralSettings
  appearance: AppearanceSettings
  layout: LayoutSettings
  editor: EditorSettings
  network: NetworkSettings
  runtime: RuntimeSettings
  data: DataSettings
  shortcuts: ShortcutSettings
  tools: ToolSettings
}
```

要求：

- 每个字段有默认值、类型、合法范围和是否需要重启的元数据。
- 每次 Schema 变更提供迁移函数，不直接丢弃旧设置。
- Renderer 只持有脱敏设置；密码和 Token 通过单独 API 写入 `safeStorage`。
- 数据目录切换必须先备份、验证目标目录、事务性迁移，再提示重启。
- 设置 UI 明确区分：即时生效、点击保存、需要重启、危险操作。

### 5.4 旧版设置迁移

- 扫描旧版配置目录 `~/.MooTool` 和自定义数据目录。
- 将旧配置键映射到新 Schema，未知键保留在迁移报告中。
- 首次迁移前创建完整备份，不修改原 Java 版数据。
- 显示迁移预览：发现的数据库、Vault、配置、历史和导出目录。
- 迁移过程可取消、可重试、可查看日志。

## 6. 数据与兼容策略

### 6.1 数据分层

| 数据类型 | Next 存储 | 兼容要求 |
| --- | --- | --- |
| 用户偏好 | `electron-store` | 映射 Java 配置键 |
| 敏感凭证 | `safeStorage` | 迁移后不保留明文 |
| 通用工具历史 | SQLite | 兼容 `t_func_history` 语义 |
| HTTP 历史 | SQLite | 保留请求、参数、Header、Cookie、Body |
| 翻译历史/单词本 | SQLite | 保留语言、翻译器、备注和时间 |
| 收藏 | SQLite | 保留颜色、Regex、Cron 等收藏 |
| Quick Note/JSON Vault | 文件系统 + Git | 保持目录和 Git 仓库兼容 |
| 窗口/导航状态 | `electron-store` | 最近工具和窗口状态 |

### 6.2 SQLite 技术验证

在正式迁移历史功能前完成一个独立技术验证：

1. 比较 Electron 当前 Node 运行时的 `node:sqlite` 与 `better-sqlite3`。
2. 验证 macOS ARM64/x64、Windows x64、Linux x64 打包与 CI。
3. 直接读取 Java 版 `MooTool.db` 的只读副本。
4. 对现有 schema 和 upgrade SQL 建立迁移测试。
5. 确认事务、备份、并发访问和损坏恢复策略后记录 ADR。

在验证完成前，不允许多个功能各自选择存储方案。

## 7. 完整功能迁移矩阵

状态定义：`未开始`、`骨架`、`开发中`、`待对齐验收`、`已对齐`。

| 分组 | Java ID | 工具 | 主要布局基线 | 建议阶段 | 当前状态 |
| --- | --- | --- | --- | --- | --- |
| 首页 | `mootool` | 首页/关于 | 首页、品牌、版本与入口 | P1 | 待对齐验收 |
| 文本 | `quickNote` | 随手记 | 文件树、编辑器、预览、操作栏、替换侧栏 | P6 | 待对齐验收 |
| 文本 | `textDiff` | 文本对比 | 左右输入、同步滚动、Unified Tab、操作栏 | P3 | 待对齐验收 |
| 文本 | `reformat` | 格式化 | 字符串/文件 Tab、输入输出与格式类型 | P4 | 待对齐验收 |
| 开发 | `json` | JSON | Vault/列表、编辑器、工具栏、结果与弹窗 | P2 | 待对齐验收 |
| 开发 | `java` | 代码运行 | Java/Python/Node.js 三个 Tab、编辑器、格式化、运行结果、历史；Java Tab 兼容旧版 Java/Groovy | P6 | 待对齐验收 |
| 开发 | `ymlProperties` | 配置转换 | 转换 Tab、校验、历史 | P3 | 待对齐验收 |
| 开发 | `protobuf` | Protobuf | JSON/二进制、Wire、Hex/Base64 三个 Tab | P4 | 待对齐验收 |
| 开发 | `variables` | 环境变量 | 系统变量/Java Properties Tab、表格、导出 | P5 | 待对齐验收 |
| 网络 | `http` | HTTP 请求 | 请求列表、请求编辑、响应 Body/Header/Cookie、历史 | P5 | 待对齐验收 |
| 网络 | `host` | Host | Host 列表、编辑器、查找替换、导入导出 | P5 | 待对齐验收 |
| 网络 | `net` | 网络/IP | 功能选择、参数区、结果区 | P5 | 待对齐验收 |
| 网络 | `uaParse` | UA 分析 | 输入、解析结果、预设、历史 | P3 | 待对齐验收 |
| 编码 | `encode` | 编码解码 | 多转换 Tab、上下输入输出、历史 | P3 | 待对齐验收 |
| 编码 | `crypto` | 加解密/随机 | 对称/非对称/摘要/Base64/随机 Tab | P4 | 待对齐验收 |
| 编码 | `regex` | 正则 | 匹配测试/常用正则 Tab、收藏、历史 | P3 | 待对齐验收 |
| 编码 | `cron` | Cron | 秒到年构建器、解析结果、运行时间、收藏 | P3 | 待对齐验收 |
| 编码 | `qrCode` | 二维码 | 生成/识别/历史 Tab、预览与参数 | P4 | 待对齐验收 |
| 日常 | `timeConvert` | 时间转换 | 双向转换、时区、快捷时间、历史、全屏钟 | P3 | 待对齐验收 |
| 日常 | `translation` | 翻译 | 翻译/单词本/历史 Tab、语言与翻译器 | P5 | 待对齐验收 |
| 日常 | `calculator` | 计算器 | 表达式、进制/GCD/LCM/排列组合、历史 | P3 | 待对齐验收 |
| 日常 | `colorBoard` | 调色板 | 主题色、取色、转换、运算、收藏、历史 | P4 | 待对齐验收 |
| 日常 | `image` | 图片助手 | 图像画布、缩放栏、导入导出、处理弹窗 | P4 | 待对齐验收 |
| 日常 | `pdf` | PDF | 拆分/合并 Tab、文件队列和输出设置 | P4 | 待对齐验收 |
| 系统 | `hardware` | 系统信息 | 系统/CPU/内存/存储/网络 Tab | P5 | 待对齐验收 |

## 8. 分阶段实施计划

### P0：基线冻结与验收样本

- 冻结 Java 版功能基线，不删除旧代码。
- 为 24 个工具建立逐项 parity checklist。
- 保存每个工具的浅色/深色截图和关键弹窗截图。
- 为纯转换能力收集输入/输出 golden fixtures。
- 记录快捷键、默认值、错误提示和历史写入时机。
- 标记平台差异、外部依赖和旧版已知问题。

**出口条件**：每个工具都有可追踪的功能清单和布局参考。

### P1：导航内核、设置骨架与桌面基础

**状态：已完成（2026-07-15）**

- 建立 24 工具、6 分组的 `toolRegistry`。
- 使用 Zustand 管理当前工具、搜索、最近访问和窗口 UI 状态。
- 实现工具搜索与 `Command/Ctrl + K`。
- 最近使用改为真实记录，数量与旧版 `RECENT_LIMIT = 5` 对齐。
- 恢复最后打开的工具。
- 建立设置 Schema、`electron-store` 服务和类型安全 IPC。
- 创建设置模态子窗口与全部设置分类骨架。
- 实现应用菜单、快捷键注册、窗口尺寸保存、关闭策略和系统托盘骨架。
- 清理现有按钮类型与编辑器可访问性警告。

**出口条件**：所有工具可通过注册表打开占位页；搜索、最近、设置、持久化形成闭环。

### P2：共享工作台能力与 JSON 完整对齐

**状态：实现完成，待 Java 人工对照验收（2026-07-16）**

- 抽取统一 Tool Page、Toolbar、Split Pane、Tabs、Editor、History、Favorite、File Picker。
- 完成 SQLite 技术验证和 Repository 层。
- 完成通用历史面板：搜索、应用、复制输入/输出、删除、清空。
- JSON 对齐：排序、忽略大小写、重复键、键值交换、JSON/XML、JavaBean、JSON Path、可视化选择器、导入导出、查找、字体字号。
- JSON Vault、Git 面板和相关设置达到旧版能力。

**出口条件**：JSON 通过完整功能、布局、数据和快捷键 parity 验收。

已完成 JSON 主功能、历史、Vault/Git 高级操作、响应式布局、Java 旧数据迁移、大文本虚拟编辑器、性能门槛和端到端测试。出口前仅保留 Java 同尺寸截图与快捷键人工签收。

### P3：纯本地计算工具

**状态：实现完成，待 Java 对照验收（2026-07-15）**

- 时间转换。
- 编码解码。
- UA 分析。
- 计算器。
- Regex。
- Cron。
- 文本 Diff。
- Properties/YAML 配置转换。

这些工具优先复用通用输入输出、历史、收藏和 Tab 组件，并建立大量单元测试。

已完成八个工具的功能实现、三语言文案、明暗主题、响应式布局、历史/收藏持久化和 Electron 工作流测试。`1440` 宽屏、`1080 x 720` 最小窗口及代表性深色页面均已建立截图基线；详细证据见 `doc/parity/p3-local-tools.md`。

**出口结果**：P3 实现范围已完成，类型检查、42 项单元/集成测试、6 项 Electron E2E、生产构建和 React Doctor 全部通过。Java 同尺寸截图逐项签收与旧版收藏数据迁移作为共享 parity 验收工作继续跟踪，不阻塞 P3 实现阶段收口。

### P4：文件、媒体与密码学工具

**状态：实现完成，待 Java 对照验收（2026-07-16）**

- QR Code。
- Crypto/Random，包括 SM2/SM3/SM4。
- Color Board 与屏幕取色。
- Image Assistant，包括截图、压缩、水印和图片导入导出；OCR 已列为明确的对齐例外，不进入本轮实现。
- Reformat。
- PDF 拆分/合并。
- Protobuf。

该阶段需要重点验证剪贴板、文件拖放、原生对话框、长任务取消和平台权限。

已完成七个工具的核心功能、三语言文案、明暗主题、响应式布局、历史/收藏、图片仓库和 Electron 文件/剪贴板/PDF 服务。`1440 x 920` 浅色与 `1080 x 720` 深色共 14 张页面视觉基线已建立；详细证据见 `doc/parity/p4-file-media-crypto.md`。

**出口结果**：P4 实现范围已完成，类型检查、72 项单元/集成测试、8 项 Electron E2E、生产构建、14 页视觉检查和 React Doctor 全部通过。OCR 按已批准差异排除；Java 同尺寸截图签收、跨平台安装包复测和大文件压力/取消测试继续作为共享发布验收跟踪。

### P5：网络与系统工具

**状态：实现完成，待 Java 对照验收（2026-07-16）**

- HTTP Request。
- Translation、单词本与翻译器 fallback。
- Host 管理与提权流程。
- Network/IP、WHOIS、ping、netstat、DNS flush。
- Environment。
- System Info。

该阶段同时完成代理设置、离线状态、超时、取消、权限和错误码设计。

已完成六个工具的功能实现、Java 兼容业务表、三语言文案、明暗主题和响应式布局。HTTP 与翻译接入统一代理、超时、取消和错误码；Hosts 与系统命令置于主进程白名单边界；`1440 x 920` 浅色与 `1080 x 720` 深色共 12 张页面视觉基线已建立。详细证据见 `doc/parity/p5-network-system.md`。

**出口结果**：P5 实现范围已完成，类型检查、83 项单元/集成测试、10 项 Electron E2E、生产构建、12 页视觉检查和 React Doctor 全部通过。Java 同尺寸截图签收、第三方翻译/代理实测、系统权限和跨平台安装包复测继续作为共享发布验收跟踪。

### P6：复杂编辑器、Vault 与运行时

**状态：实现完成，待 Java 对照验收（2026-07-16）**

- Quick Note 全量能力、Markdown 预览、图片、搜索、导出、快速替换和 Git Vault。
- 将旧版 Java/Groovy 工具升级为“代码运行”，页面提供 Java、Python、Node.js 三个主 Tab；Java Tab 保留旧版 Java/Groovy 的格式化与运行能力。
- 每个 Tab 独立保存代码模板、运行参数、工作目录和历史，切换 Tab 不丢失未提交输入。
- 默认探测用户系统的 `java`/`javac`、`groovy`、`python`/`python3`、`node`；设置中展示实际路径、版本和检测状态，并允许用户覆盖路径。
- 未检测到对应运行时时，仅禁用该 Tab 的运行操作并给出配置提示，不影响其他 Tab 使用；首个版本不强制捆绑 JDK、Python 或 Node.js。
- 所有代码都通过主进程受控子进程执行，使用临时工作目录、环境变量白名单、超时、输出上限、取消和进程树回收。
- 同步与备份闭环。

**运行时出口条件**：三个 Tab 均能完成检测、编辑、执行、停止、输出和历史闭环；Java/Groovy 旧版用例全部通过 parity 验收。

已完成 Quick Note 的 Java 兼容 `.txt` + YAML frontmatter Vault、嵌套文件树、搜索排序、编辑/分栏/预览、图片附件、导入导出、查找替换、快速替换、Git 和自动化；已完成 Java/Groovy、Python、Node.js 受控运行台及本地完整/分类备份。详细功能、Java 对照依据和保留验收项见 `doc/parity/p6-complex-editors-runtime.md`。

**出口结果**：P6 实现范围已完成，类型检查、33 个测试文件共 101 项单元/集成测试、13 项 Electron E2E、生产构建、6 页明暗/宽窄视觉审计和 React Doctor 全部通过。Quick Note 树拖拽、Git 高级处理和真实旧数据迁移已在 P7 完成；四种外部运行时及 Windows/Linux 安装包实机复测继续作为发布验收项目。

### P7：全量对齐、迁移与发布

**状态：实现与本机候选包完成，待外部发布验收（2026-07-16）**

- 逐工具执行 parity checklist。
- 执行旧版数据迁移和回滚演练。
- 完成 macOS、Windows、Linux 安装包与签名流程。
- 完成更新、托盘、关闭行为、设置和快捷键全量验收。
- 完成性能、内存、启动速度和大文件压力测试。
- 旧 Java 版继续保留，直到 Next 全量验收和迁移稳定后再决定维护策略。

已完成高级 Vault/Git、旧数据迁移、完整备份、更新检查、托盘与关闭策略、大文件性能、macOS ARM64 DMG/ZIP 和四平台 CI 矩阵。详细迁移边界、回滚、性能数据、包校验和发布阻塞项见 `doc/parity/p7-release-migration.md`。

**出口结果**：35 个测试文件共 114 项 Vitest、16 项 Electron E2E、生产构建、P7 四张视觉审计、React Doctor 100/100、打包 App 性能与 macOS ARM64 产物验证通过。远端 Intel/Windows/Linux Runner、正式签名/公证、真实在线服务及 Java 人工布局签收继续作为公开发布门槛。

## 9. 测试与验收

### 9.1 测试层级

- 单元测试：转换、解析、校验、排序、时间、Cron、Regex、Crypto 等纯逻辑。
- 组件测试：工具栏、Tabs、历史、收藏、设置表单、快捷键冲突。
- IPC 契约测试：权限、入参校验、错误码、取消和进度事件。
- 数据测试：旧 SQLite 样本、配置迁移、Vault、备份与回滚。
- Electron E2E：窗口、菜单、托盘、剪贴板、文件选择、设置子窗口。
- 视觉回归：Java 版参考图与 Next 对比，覆盖浅色、深色和三种语言。

### 9.2 基准视口

- 主要基准：`1440 x 920`。
- 最小窗口：`1080 x 720`。
- 额外检查：宽屏、系统缩放、中文/英文/日文最长文案。
- 页面不得出现工具栏溢出、文字截断、控件重叠或不可访问操作。

### 9.3 单工具 Definition of Done

一个工具只有同时满足以下条件才能标记为“已对齐”：

- Java 版可用功能 checklist 全部通过。
- 默认值、输入输出和错误行为对齐。
- 历史、收藏、导入导出和快捷键已对齐。
- 三种语言完成，无用户可见硬编码文案。
- 明暗主题与最小窗口布局通过。
- 与 Java 版主要布局对比已确认。
- 单元测试与关键 E2E 通过。
- 不绕过 preload 直接使用 Node API。
- 迁移后的旧版数据可以读取和继续使用。
- 文档中附有验收证据和已知差异。

## 10. 重点风险

| 风险 | 处理策略 |
| --- | --- |
| SQLite 原生模块与 Electron 打包 | P1/P2 先做跨平台技术验证和 ADR |
| Java/Python/Node.js 运行时差异 | 采用三个独立 Tab；自动探测系统运行时并允许在设置中覆盖路径，逐运行时验证版本；统一使用受控子进程、临时目录、超时、取消和输出限制 |
| Host、DNS、系统命令权限 | 主进程白名单 API、平台实现、明确提权流程 |
| SM2/SM3/SM4 正确性 | 选择成熟库，使用标准测试向量验证 |
| 翻译服务变化 | 保持 Google/Bing 轻量双实现，提供超时、fallback 和代理支持；不引入重型 Provider 接口层 |
| 大文件与长任务卡顿 | Worker/子进程、流式处理、进度和取消 |
| 旧数据迁移损坏 | 只读扫描、自动备份、事务迁移、可回滚 |
| 布局现代化导致功能遗漏 | 每个工具维护 Java/Next 双栏 parity checklist |
| 设置项扩散 | 所有设置先进入统一 Schema，再接 UI 和存储 |

## 11. 近期执行清单

P1-P7 的代码实现和本机候选验收已完成。当前只继续处理公开发布所需的外部验收项。

- [x] 建立 24 工具和 6 分组的 TypeScript 注册表。
- [x] 将左侧导航、首页入口和搜索改为注册表驱动。
- [x] 建立 Zustand 工作台 Store。
- [x] 实现真实工具搜索和 `Command/Ctrl + K`。
- [x] 实现最近 5 个工具与最后打开工具持久化。
- [x] 定义版本化 `AppSettings` Schema。
- [x] 建立 `electron-store` 主进程服务和 typed preload API。
- [x] 创建设置模态子窗口和全部分类骨架。
- [x] 接通语言、主题、强调色、字体字号、显示最近和布局设置。
- [x] 建立代理、数据目录、Vault、快捷键、更新和托盘设置域。
- [x] 建立 Java、Groovy、Python、Node.js 运行时设置与自动检测。
- [x] 建立 P0 parity checklist 模板并填写 JSON 样例。
- [x] 补齐当前基础可访问性和按钮类型问题。
- [x] 增加基础单元测试和 Electron E2E。

P5 完成情况：

- [x] 审计 HTTP、翻译、Host、网络/IP、环境变量和系统信息的 Java 行为与布局。
- [x] 建立统一网络请求、代理、超时、取消和错误码边界。
- [x] 迁移 HTTP 请求编辑、响应、Cookie/Header 和历史数据模型。
- [x] 迁移 Host 与系统命令能力，按平台设计权限和降级路径。
- [x] 迁移环境变量、系统信息、翻译/单词本并补齐 Electron E2E 与视觉回归。

P6 完成情况：

- [x] 审计 Quick Note、Java/Groovy 控制台和同步备份的 Java 实际行为与文件格式。
- [x] 迁移 Quick Note 文件树、编辑器、Markdown、图片、快速替换、Git 和 Vault 自动化。
- [x] 建立 Java/Groovy、Python、Node.js 受控运行台与独立持久化草稿、参数、工作目录和历史。
- [x] 建立本地完整/分类备份、存储位置展示和安全路径边界。
- [x] 完成 101 项 Vitest、13 项 Electron E2E、6 页视觉审计与 React Doctor 验证。

P7 已按逐工具 Java/Next parity、旧数据迁移、跨平台安装包、更新流程和性能压力验证推进，未扩展本轮已批准范围之外的新功能。

P7 完成情况：

- [x] 补齐 JSON Vault、Quick Note 和 Git 高级对齐能力。
- [x] 完成 Java 数据扫描、完整备份、事务迁移、幂等和回滚演练。
- [x] 完成更新检查、关于页面、托盘和关闭行为验收。
- [x] 完成打包 App 的启动、3 MB JSON、5 MB Quick Note 和内存压力门槛。
- [x] 生成并验证 macOS ARM64 App、DMG 和 ZIP。
- [x] 建立 macOS Apple Silicon/Intel、Windows x64、Linux x64 CI 打包矩阵。
- [ ] 在远端 Runner 实跑并安装验收 Intel、Windows、Linux 产物。
- [ ] 配置正式签名/公证并完成公开发布包验收。
- [ ] 完成 24 个 Java/Next 同尺寸截图与快捷键人工签收。
