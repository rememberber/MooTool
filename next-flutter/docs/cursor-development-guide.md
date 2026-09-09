# MooTool Next Flutter：Cursor 编码开发主指南

> 文档日期：2026-09-08  
> 产品 ID：`next-flutter`  
> 产品名称：MooTool Next Flutter  
> 状态：P0–P6 功能与桌面通道已在代码侧落地（见 `docs/evidence/`）；P7 更新通道与未签名打包脚本已写，本机未产出安装包、未改 `update-manifest.json`。  
> 功能参考：`next/` Electron 版 1.1.4，仓库基线以本机 `git rev-parse HEAD` 为准。

## 1. 目标与范围

交付一个可独立安装使用的 Flutter 桌面开发者工具箱。用户从 Electron 切换过来后，应能凭原有记忆找到工具、辨认输入输出和主要操作、继续相同工作流；同时获得更清晰的层级、稳定的窗口适配、完整键盘操作和更好的编辑体验。

目标平台为 macOS、Windows、Linux。首轮在开发机平台完成纵向闭环，同时建立三平台构建检查；正式发行范围必须由实际平台测试决定。移动端和 Web 不属于本轮。

优先级从高到低：

1. 数据可靠、产品隔离、真实可用。
2. 工具入口、布局关系、操作顺序、算法语义与 Electron 尽量一致。
3. 桌面交互：编辑、快捷键、窗口、拖放、原生文件及剪贴板。
4. 现代 UI：信息层级、间距、色彩、焦点、密度和响应式。
5. 次要外观预设及平台专属装饰。

不要将目标缩减为“26 个菜单可以打开”。一个只有若干通用输入框、其余按钮显示成功提示的原型，不满足本项目要求。允许分阶段交付，但必须明确哪些流程仍未完成。

本轮不默认增加账户、云同步、AI 对话、OCR、插件市场或遥测。Electron 的旧 AI 规划不能自动成为 Flutter 需求。OCR 在 Electron 既有对齐文档中已明确排除。

配套文档：

- [逐工具功能规格](feature-parity.md)：所有工具的实际范围。
- [验收与交付标准](acceptance.md)：可执行的检查及交付记录。
- [Cursor 提示词](cursor-prompts.md)：开始、续接和审查的方法。

## 2. 基线与证据优先级

### 2.1 先确认当前行为

后续开发前记录 `git rev-parse HEAD`、`git status --short`、Electron 版本和相关工作树差异。用户已有未提交修改不能覆盖。首次冻结的基线保持稳定，后续 Electron 变化可按单项吸收，不要求追随每次提交。

遇到矛盾时依次采用：

1. 用户对产品的明确要求。
2. 本次冻结的 Electron 实际运行行为、当前对应源码和有效测试；三者矛盾时建立复现并记录。
3. 本文及配套规格中明确写出的 Flutter 设计决定。
4. Electron 同提交、同设置、同尺寸的截图。
5. 旧截图、`next/doc/parity/`、旧迁移规划及 README。

Flutter 可以修正 Electron 的缺陷。必须记录原行为、预期行为、理由、受影响数据和验收；不能将“不好实现”直接写成兼容差异并关闭任务。

### 2.2 已核实的重要事实

| 项目 | 当前源码事实 | 对 Flutter 的影响 |
| --- | --- | --- |
| 入口数 | `toolRegistry.ts` 共 26 项：首页 + 25 个工具 | 功能计数不得写成 26 个工具或使用旧文档中的 24 项 |
| 布局 | 所有功能工具采用沉浸式工作区，首页例外 | 不照搬旧截图中的大标题、宽外边距及整页圆角卡片 |
| 默认外观 | `modern`、系统主题、蓝色强调色、界面 13、编辑器 14 | 以此冻结默认视觉参照 |
| 导航 | `classic/card/grouped` 三种，支持隐藏标题、自定义组和隐藏工具 | 默认 classic，不擅自改为卡片仪表盘 |
| 导航宽度 | 基础 CSS 为展开 248、折叠 84 | Flutter 以此作初始值，响应式变化见第 5 节 |
| 最近使用 | 最近 5 项，默认不展示 | 导航隐藏设置不应导致工具无法搜索 |
| 设置入口 | `Workbench.tsx` 内渲染 `SettingsPage` | 默认主工作区设置页；文件名 SettingsWindow 不代表必须独立窗口 |
| 编辑器 | 公共 CodeMirror 编辑基础，随手记有列编辑 | 不能以普通多行 TextField 代替完整编辑器 |
| 窗口 | 工具可分离、收回，状态保持，关闭分离窗口会收回 | 需会话所有权与生命周期设计 |
| 图片 | 当前含转 SVG 功能 | 旧 P4 图片清单不完整，必须纳入 |
| 留言板 | 本地展示牌，非在线讨论区 | 不引入发布留言服务器 |

### 2.3 必读源码地图

以下路径以仓库根目录为起点，只用于阅读或一次性提取测试样本，不允许成为 Flutter 构建依赖。

| 范围 | 参考路径 |
| --- | --- |
| 注册及会话 | `next/src/app/toolRegistry.ts`、`appStore.ts`、`next/src/shared/contracts/app.ts` |
| 桌面壳 | `next/src/features/workbench/Workbench.tsx`、`ToolWindow.tsx`、`immersiveTools.ts` |
| 布局与样式 | `next/src/shared/styles/global.css`，必须阅读后面的覆盖规则；`shared/components/ToolPage.tsx` |
| 默认设置及语言 | `next/src/shared/contracts/settings.ts`、`shared/i18n/messages.ts`、`features/settings/SettingsWindow.tsx` |
| 通用编辑 | `next/src/shared/components/TextCodeEditor.tsx`、`findReplace.ts`、`codeEditorViewState.ts` |
| 工具业务 | `next/src/features/<feature>/`，详情见功能规格 |
| 本机能力 | `next/electron/main/` 和 `next/src/shared/contracts/` |
| 桌面回归 | `next/tests/electron/app.spec.ts`、`tool-windows.spec.ts`、`immersive-tools.spec.ts` |
| 发布 | `RELEASE_CONVENTIONS.md`、`next/doc/update-products-and-assets.md` |
| 旧视觉辅助 | `next/doc/screenshots/json/`、`p3/`、`p4-*`、`p5-*`、`p6-*`、`p7-*` |

历史截图只帮助理解左右/上下关系和功能位置。正式开始 UI 编码时，要用独立测试数据目录重新捕获当前 Electron 默认主题，至少包含首页、JSON、随手记、HTTP、设置和独立窗口的明暗两套状态。

## 3. 产品线独立性：硬性边界

### 3.1 产品关系

Java、Electron、Tauri、macOS Native、Flutter 是同一仓库中的独立产品。Electron 是首版体验参考，不是 Flutter 的运行时、源码上游或永久规格控制方。

允许：

- 在 Flutter 目录内重新实现相同算法、状态模型、测试和平台辅助代码。
- 一次性复制有权使用的图标、文案、样本、算法思路，保留许可证和来源记录。
- 维护 Flutter 自己的版本、依赖升级、数据 schema、缺陷修复和路线图。
- 自有格式的显式导入导出，或为指定版本提供只读导入适配器。

不允许：

- `path: ../next` 等跨产品源码依赖、符号链接、运行时资源引用。
- 构建前执行其他产品的 npm/Maven/Cargo/Swift 构建。
- 启动 Electron、Java 版或 Tauri 后端来实现 Flutter 工具。
- 把通用库抽到仓库公共目录后要求所有产品一起迁移。
- 默认共用数据库、设置、Vault、密钥、更新下载目录、单实例锁或后台任务。

“独立构建”的定义：仅取出 `next-flutter/`，具备已声明的 Flutter、平台 SDK 和自有构建依赖后即可构建；首次正常下载第三方依赖可以，依赖相邻产品不可。Node/Python/JDK/Groovy 只可作为用户主动使用代码运行功能时的外部运行环境。

### 3.2 固定身份

以下为 Flutter 产品设计值，不表示已经注册到根清单。

| 项目 | 规定值 |
| --- | --- |
| Product ID | `next-flutter` |
| UI/安装显示名 | `MooTool Next Flutter`；工作区内品牌可简写 MooTool，关于页必须显示完整产品名 |
| Dart package | `mootool_next_flutter` |
| Bundle ID / Windows AppUserModelID / Linux application ID | `com.rememberber.mootool.next.flutter` |
| macOS 安装名 | `MooTool Next Flutter.app` |
| 可执行文件 | macOS/Windows：`MooToolNextFlutter`；Linux：`mootool-next-flutter` |
| Linux 包名、desktop 文件 | `mootool-next-flutter`、`com.rememberber.mootool.next.flutter.desktop` |
| 版本权威来源 | `next-flutter/pubspec.yaml` 的 `version`；初始建议 `0.1.0+1` |
| Tag | `next-flutter-v{semver}`，不含 Flutter build number |
| Release 标题 | `MooTool Next Flutter {semver}` |
| 配置/锁/凭据命名空间 | `com.rememberber.mootool.next.flutter` |
| 自定义 URL scheme | 仅在需要时启用 `mootool-next-flutter`，不争用其他产品 scheme |

Windows 安装器 AppId/UpgradeCode 必须生成一次并稳定保存到本产品配置，不复制 Electron 或 Tauri 的值。版本中的 build number 映射到平台内部版本，不另设手工维护的第二版本源。

### 3.3 默认数据路径

通过统一 `AppPaths` 解析平台基础目录并验证最后得到的实际路径；不要以为改 Bundle ID 就会自动隔离所有插件的数据。

| 平台 | 数据根目录设计 |
| --- | --- |
| macOS 直接分发 | `~/Library/Application Support/com.rememberber.mootool.next.flutter/` |
| Windows | `%APPDATA%/MooToolNextFlutter/` |
| Linux | `$XDG_DATA_HOME/mootool-next-flutter/`，变量缺省时为 `~/.local/share/mootool-next-flutter/` |

缓存、日志及临时文件使用平台对应 cache/state/temp 基础目录下的 Flutter 专属子目录；不得写在安装目录。Windows 缓存使用 `%LOCALAPPDATA%` 专属子目录。若采用沙盒发行，按容器规则调整并记录实际路径，不把容器路径硬编码为上述路径。

`path_provider` 可提供平台常用位置，但不同平台/方法支持不同，实际结果仍需本产品统一检查。参见 [path_provider 说明](https://pub.dev/packages/path_provider)。

开发、测试和正式数据分别使用专属 profile。测试必须提供 `--data-dir=<临时目录>` 等受控启动参数；测试配置不能成为发布包的隐式数据覆盖入口。

## 4. 技术路线与工程结构

### 4.1 默认路线

采用 Flutter 桌面、Dart、Material 3 的主题/语义/基础控件能力，加本产品自有桌面组件。界面以 Flutter Widget 实现。不要把 Electron 页面整体放进 WebView，也不要让业务依赖 Node sidecar。

Flutter 桌面支持及平台构建依赖以 [Flutter 桌面文档](https://docs.flutter.dev/platform-integration/desktop) 为准。P0 记录准确的 stable SDK、Dart、平台 SDK 及插件版本并锁定；本文不把随时间变化的 latest 写成长期版本约束。

工程分层：

```text
Flutter View → Controller / 应用用例 → 领域模型与服务接口
                                      ↓
                  Repository / Dart IO / 平台插件 / FFI
```

UI 负责展示和意图，业务层负责操作语义，仓库负责存储，平台适配器负责 OS 能力。采用按 feature 组织的简单分层即可，避免每个按钮都建立一整套抽象工厂。此方向参考 [Flutter 架构指南](https://docs.flutter.dev/app-architecture/guide)，具体目录和会话协议是本项目决定。

### 4.2 依赖选择与验证

| 范围 | 默认方向 | 开发前必须验证 |
| --- | --- | --- |
| 状态及依赖注入 | [flutter_riverpod](https://pub.dev/packages/flutter_riverpod) | 工具会话不会因切页 autoDispose 丢失；跨 engine 不依赖同一个 ProviderContainer |
| SQLite | [drift](https://pub.dev/packages/drift) | 后台连接、事务迁移、备份快照、故障恢复；桌面动态库正确打包 |
| 文件选择 | [file_selector](https://pub.dev/packages/file_selector) | 打开/保存/目录/取消，Unicode 路径，多窗口 owner |
| 凭据 | [flutter_secure_storage](https://pub.dev/packages/flutter_secure_storage) | 各平台命名空间及依赖；无 keyring 时清楚报错，不能回退明文 |
| 单窗口管理 | [window_manager](https://pub.dev/packages/window_manager) | DPI、拖动、标题栏、close/hide、主窗口及子窗口行为 |
| 多窗口候选 | [desktop_multi_window](https://pub.dev/packages/desktop_multi_window) | 各窗口独立 engine 和插件注册；与 window_manager 的组合先实测 |
| 编辑器候选 | [re_editor](https://pub.dev/packages/re_editor) | IME、查找替换、矩形选区、撤销、大文本、辅助技术，不能只看演示截图 |
| HTTP | Dart HttpClient 或经验证的 Dart 客户端适配器 | 代理认证、重复 Header、重复 Query、流式取消、响应解压大小限制 |
| 图像处理 | 本产品内的 Dart/原生处理服务 | 格式支持、透明通道、色彩/方向、批处理、资源释放 |
| PDF 变换 | 能处理已有 PDF 的库或随包 native helper | 真正页提取/合并、文本保留、异常文档及架构支持 |
| Protobuf | 动态 schema 解析及 descriptor 编解码服务 | 运行时粘贴任意受支持 proto，不能只支持编译时生成的消息 |
| 格式化与国密 | Dart 库优先；不足时使用随本产品打包的 FFI/helper | 许可证、平台产物、离线可用、测试向量和取消 |

此表是选型建议，除本产品已规定的架构边界外，具体插件在 P0 通过样例后固定。每项需记录能力、许可证、准确版本、目标平台、替代路径。不要同时引入多个 UI 框架并让各页面呈现不同视觉语言。

两个不能跳过的技术事实：

- [Dart pdf 包](https://pub.dev/packages/pdf) 主打 PDF 生成；引入它不等于已具备已有 PDF 拆分/合并能力。禁止将每页栅格化截图再生成 PDF 当作等价处理。
- [Dart protobuf 包](https://pub.dev/packages/protobuf) 是生成代码的运行时支持；引入它不等于具备用户粘贴 proto 后的动态解析。需要独立证明动态 schema 路径。

### 4.3 目录规划

以下目录由后续开发创建；本文编写时只有文档和 Cursor 规则。

```text
next-flutter/
  pubspec.yaml
  pubspec.lock
  analysis_options.yaml
  l10n.yaml
  lib/
    main.dart
    app/                 # bootstrap、AppPaths、tool_registry、session、router
    design/              # ThemeExtension、tokens、基础桌面组件
    core/
      editor/            # 编辑文档、选区、undo、搜索、语言、编辑器适配
      storage/           # SQLite、设置、原子文件、迁移、备份
      platform/          # 强类型本机服务、capabilities、窗口
      jobs/              # 任务、事件、取消
      security/          # secret、路径/参数校验、诊断脱敏
    features/
      home/ quick_note/ text_diff/ reformat/ json/ runtime/
      config_convert/ protobuf/ variables/ http/ host/ net/ ua/
      encode/ crypto/ regex/ cron/ qr_code/ time_convert/
      message_board/ translation/ calculator/ color_board/ image/ pdf/
      hardware/ settings/
    l10n/
  assets/                # 产品自有资源副本及许可证记录
  packages/              # 确有需要时的本产品编辑器/原生插件
  native/                # 自有 helper 源码和构建配置，按需
  macos/ windows/ linux/
  test/fixtures/         # 固定算法样本与兼容期望，无跨产品依赖
  test/unit/ test/widget/ test/integration/
  integration_test/
  scripts/               # 检查、打包、截图、并存验收
  docs/adr/ docs/evidence/
  release-notes/
```

### 4.4 会话与任务契约

定义稳定的 ToolId，与功能规格表一致；Dart 命名可采用 snake_case，但序列化 ID 保持表中值。注册信息至少包含分组、排序、三语言键、关键词、图标、构建器、历史/收藏能力及平台能力要求。

通用会话至少保存：

| 数据 | 内容及所有者 |
| --- | --- |
| ToolSession | sessionId、toolId、schemaVersion、revision、activeTab、业务草稿、结果摘要 |
| EditorDocument | documentId、文本、dirty、savedRevision、换行/编码、选区、滚动、undo/redo |
| ToolViewState | 面板尺寸、展开项、搜索、当前文档、工具内焦点 |
| JobHandle | jobId、ownerSessionId、状态、进度、取消入口、完成/失败结果 |
| AppError | code、messageKey、参数、可恢复动作、脱敏诊断，不依赖解析异常字符串 |

状态按生命周期区分：临时 hover 不持久化；切页要保留输入、输出、Tab 和滚动；跨重启恢复草稿、文档和布局；undo 默认仅在本次会话内保存。切换文档不能串用 undo。

长任务统一状态机：

```text
idle → running → succeeded
               → failed
               → cancelling → cancelled
```

携带 requestId / revision 处理结果，丢弃过期结果，避免旧格式化覆盖新输入。取消必须真正中止请求、worker 或进程并清理资源；仅隐藏 loading 不算取消。

CPU 密集格式化、Diff、摘要、图像、PDF 和复杂匹配应移至 isolate 或本产品 helper；IO 使用异步。平台通道的线程和 isolate 支持依赖实际插件，不能把所有插件调用直接丢进 worker。参见 [Flutter isolates](https://docs.flutter.dev/perf/isolates) 及 [平台通道](https://docs.flutter.dev/platform-integration/platform-channels)。

## 5. 布局与现代 UI 规范

### 5.1 保持桌面工作台结构

```text
┌──────────────────┬────────────────────────────────────────────┐
│ 原生窗口控制安全区 │ 工具栏 / 工具 Tab / 可用拖动区域              │
│ 折叠 搜索 分组管理  ├───────────┬─────────────────┬──────────────┤
│ 首页              │ 文档/集合  │ 主编辑或主要结果 │ 检查器/选项   │
│ 自定义组（如有）   │ 可折叠     │ 占主要空间       │ 可折叠        │
│ 内置工具导航       │            │                 │              │
│                  ├───────────┴─────────────────┴──────────────┤
│ 最近（可选）       │ 当前工具状态栏 / 保存状态 / 任务状态          │
│ 品牌 语言 设置     │                                            │
└──────────────────┴────────────────────────────────────────────┘
```

这是一种工作区模板，不能强迫所有工具都三栏。JSON、随手记、HTTP、Diff、图片、PDF 各自遵循功能规格。功能页没有额外的大页面标题、说明段、面包屑或整页浮起卡片；名称通过侧栏、窗口标题和 Semantics 保持可辨认。

默认沿用 classic 导航，分组和顺序不变；grouped 模式再显示完整组标题。自定义组位于内置工具之前；允许隐藏内置导航项，命令搜索仍可打开工具。首页保留品牌、简介、贡献者、赞赏、源码、帮助与其他作品内容，不改造成陌生工具商店。

### 5.2 尺寸、密度与响应式

以下均为 Flutter 逻辑像素，属于 Flutter 初版产品值；实际需与冻结基线比较，兼顾操作空间。

| 元素 | 初始值/范围 |
| --- | --- |
| 默认窗口 | 1440 × 920；屏幕较小时限制在可见工作区内 |
| 核心验收窗口 | 1440 × 920、1080 × 720；额外 1920 × 1080 |
| 主窗口最小建议 | 960 × 640，必须通过精简布局检查后再设为实际最小值 |
| 导航 | 展开 248，可调 220–280；折叠 84 |
| 工具标题栏/首行 | 44–48；以原生按钮安全区为约束，不盲目覆盖 |
| 紧凑按钮 | 高 32；常规高 36；图标 16–18 |
| 导航行 | 常规 36，紧凑 32 |
| 文档树 | 默认 220，宽屏范围 180–320 |
| JSON 检查器 | 默认 280，范围 240–360 |
| 状态栏 | 24–28 |
| 分隔线 | 1；拖动热区至少 6–8，显示 resize cursor |
| 间距刻度 | 4、8、12、16、24、32 |
| 内容行内边距 | 8–12；功能组间距 16–24 |
| 控件圆角 | 6–8；弹窗 12；连续工作区外边界 0 |

响应式必须使用当前容器约束计算，不能根据操作系统或显示器全屏尺寸猜布局。这符合 [Flutter 自适应建议](https://docs.flutter.dev/ui/adaptive-responsive/best-practices)。

- 内容可用宽度满足各列最小宽度之和时才并排；JSON 三栏至少为 180 + 360 + 240 + 分隔热区。
- 1080 宽主窗口默认保留导航与主编辑区；检查器转按需抽屉或覆盖面板。用户折叠导航后空间足够时允许重新并排。
- 文档库默认在可能时保持左侧；再缩小时用明确的“文档库”切换按钮显示 overlay。关闭 overlay 恢复编辑焦点和原选区。
- 工具栏按重要度溢出到“更多”，顺序稳定；格式化/发送/运行等主动作不能被裁切。先收纳低频项，再采用两行，不能让整页横向滚动。
- Diff 两栏最小空间不足时切可切换单栏/统一视图，保留两份内容及对比状态；不能缩到每栏不可编辑。
- 表格可在表格区域内部水平滚动；状态、主要操作和窗口导航不得随之消失。
- 字体 125%/150% 后重新计算断点；对下拉选项按本地化内容测量宽度，不能用固定中文宽度截断英文和日文。
- 面板尺寸保存为受边界约束的值；切窗口尺寸时重新 clamp，避免恢复时负宽度。

### 5.3 主题与视觉 token

采用低饱和中性色、清晰文字和克制强调色。不要用满屏渐变、玻璃模糊或厚阴影表现“现代”。

| Token | Light 建议 | Dark 建议 |
| --- | --- | --- |
| workspace | `#FAFBFC` | `#17191D` |
| surface | `#FFFFFF` | `#1F2228` |
| sidebar | `#F1F3F6` | `#1B1E23` |
| toolbar | `#F6F7F9` | `#24272E` |
| borderDecorative | `#E1E5EB` | `#363B45` |
| textPrimary | `#20242C` | `#E8EBF1` |
| textSecondary | `#586174` | `#ADB6C6` |
| accentPrimary | `#356CB8` | `#8AB8F8` |
| onAccent | `#FFFFFF` | `#101B2B` |
| focus | `#2468C8` | `#8AB8F8` |

此表是设计起点，不能凭 token 表宣称已通过对比度验证。为输入边界、禁用、选中、错误、告警和成功定义完整语义色；装饰分隔线不承担唯一识别控件的责任。语法主题分别配置属性、字符串、数字、注释和错误，不直接反转浅色。

原有六种强调色仍可选择；每种均计算 foreground、container 和 focus，黄色不能直接配白色小字。样式预设保留 `modern/quiet/hero/smartisan/miui-v5/claude` 的选项与含义，但实现为本产品主题配置，默认优先完善 modern。其他预设未完成时按待交付记录，不能选择后实际仍是 modern。

主执行按钮统一高度、图标、文案和状态。每个操作区保留一个明确主动作；转换区可有两个方向，但不要让复制、清空和删除与主要操作争抢视觉权重。清空/删除与主动作保持间距，且可撤销或确认。

### 5.4 字体、反馈、键盘与无障碍

- UI 默认系统字体 13，编辑器默认等宽 14；中文/日文/emoji 有 fallback。字号设置是真实布局变化，不只整体缩放位图。
- 按钮具有 normal、hover、pressed、focused、disabled、busy 六类状态；点击后及时反馈，长任务显示进度/取消。
- 反馈分层：即时复制用 toast；校验错误靠近输入并保留原文；持续任务在工具区域展示；破坏操作才使用确认弹窗。
- 普通文本按至少 4.5:1、大文本按至少 3:1 的对比目标验收；参考 [W3C 对比度说明](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html)。这是产品可访问性目标，不声明已取得任何合规认证。
- 所有交互热区默认至少 32 × 32；面向触控时至少 44 × 44；密集编辑器内小视觉图标仍有足够热区。参考 [W3C 目标尺寸说明](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html)。
- 采用 FocusTraversalGroup、Shortcuts/Actions、Semantics 和 Tooltip；图标按钮要有工具名和动作名，不能只报“按钮”。状态不能只用颜色表达。
- 焦点框建议 2 px；弹窗限制焦点范围，关闭返回原触发点。Tab、Shift+Tab、方向键、Enter、Space 都有合理作用。
- IME composition 优先处理 Enter/Esc；先关闭候选/菜单/弹窗/演示模式，再处理窗口级 Esc，防止输入中文时误隐藏应用。
- 动效建议 120–180 ms，遵循系统减少动态效果；编辑器切换、格式化和保存不能用动画干扰光标。
- macOS 用 Command，Windows/Linux 用 Ctrl；菜单及 tooltip 按平台展示，不能在 Windows 固定显示 ⌘。

| 默认快捷键 | 行为 |
| --- | --- |
| Cmd/Ctrl+K | 工具搜索；中英日名称、ID、关键词 |
| Cmd/Ctrl+, | 设置页；再次打开聚焦已有设置 |
| Cmd/Ctrl+F | 当前编辑器查找 |
| Cmd/Ctrl+Shift+F | 适用工具格式化；不与全局动作冲突 |
| Cmd/Ctrl+S | 保存当前文档；无文档时触发明确的保存流程 |
| Cmd/Ctrl+Z / 平台重做键 | 当前文档 undo/redo |
| Cmd/Ctrl+Enter | 当前工具主要操作；输入法组合期间不触发 |
| Esc | 从最内层临时 UI 逐级关闭；窗口策略以能力和当前状态处理 |

列编辑与多光标快捷键在 P0 按所选编辑器及平台修饰键确定，须记录并展示帮助。不能占用 macOS 系统级常用动作或破坏普通 Option/Alt 文本选择。

## 6. 编辑器：首先完成的基础能力

JSON、随手记、Diff、HTTP、Host、运行时等都依赖可靠编辑。先完成可复用编辑基础，再逐工具接业务；不要开发后期再把普通 TextField 全部替换。

统一编辑器适配接口至少提供：

- 加载/切换 documentId；获取文本、修改事务及 dirty/saved 状态。
- 单选区、多选区、矩形选区；按 Unicode 文本边界正确处理用户输入。
- undo/redo；格式化、快速替换、插入附件均形成可撤销事务。
- 查找、计数、前后跳转、替换、全部替换；大小写、全词和正则选项。
- 行号、自动换行、滚动、语法高亮、字体、字号、只读及只读结果复制。
- 保存/恢复光标、选择、滚动、折叠与当前查找状态。
- 键盘、鼠标、触控板、拖放、系统剪贴板、中文/日文输入法。

P0 编辑器样例必须真实演示：连续输入中文 → 选择词语 → undo/redo → 矩形选择多行并输入 → 格式化后撤销 → 开启/关闭随手记分栏预览 → 切文档 → 分离/收回窗口。不得发生选区丢失、候选窗错位、重复字符或 undo 串文档。

超大文本策略：

1. 视口渲染，避免把几十万行一次性创建为 Widget。
2. 超过约 1 MiB 可关闭完整语法树/高亮，但要显示原因；编辑、查找、复制、保存依然可用。
3. 全量解析和转换在 worker 执行，使用 document revision 防止覆盖新编辑。
4. 搜索与正则有取消或硬超时；灾难性回溯不能锁死 UI。
5. 非法 JSON 不影响文本保存和切换工具；错误状态不能清空原文。
6. 程序性更新内容不得无条件重置 TextEditingValue、选区和 composing。

如果候选编辑器缺少矩形选区或 undo 转移能力，在本产品 `packages/` 内补足或更换实现。不能因依赖不支持就将这些基线功能静默删除。WebView 不是默认后备路线；采用局部其他编辑技术必须记录边界，并保持整个产品的 Flutter UI 和独立打包目标。

## 7. 多窗口与桌面生命周期

### 7.1 用户可见行为

主工作区和分离窗口指向同一个逻辑 ToolSession：

- 从侧栏行或工具窗口入口分离工具；折叠导航也能通过右键/菜单操作。
- 工具被分离后，主区域显示“已在独立窗口打开”，提供聚焦和收回。
- 关闭分离窗口等于收回，不等于删除会话、清空编辑内容或退出应用。
- 多个不同工具可以独立分离；重复打开同一工具默认聚焦现有会话。
- 主窗口切换工具不重启仍在进行的 HTTP/代码任务。
- 设置、搜索、分组管理、更新说明显示在正确层级，不能被工具视图遮住。
- 所有窗口语言/主题同步；各窗口的焦点、位置和内容保持独立。
- 主窗口关闭按 ask/hide/quit 设置执行；托盘不可用时不能把应用隐藏到无法找回。

### 7.2 实现约束

Flutter Widget、编辑器 controller 和 Riverpod 内存对象不能直接从一个 engine 搬到另一个 engine。`desktop_multi_window` 明确说明各窗口使用独立 Flutter engine，子窗口需要单独注册插件。参见 [多窗口插件说明](https://pub.dev/packages/desktop_multi_window)。

建议主进程保留应用服务所有者（AppCoordinator）：

- 统一拥有数据库、Vault watcher、Git 调度、下载、运行子进程和网络任务。
- 分离窗口通过受控消息请求操作；带 sessionId、windowId、requestId、revision。
- 大文本/图片避免在消息间反复 base64 全量复制；采用受控文档快照、增量或自有临时资产引用。
- 各窗口只存在一个当前编辑写入所有者；主题设置可广播，业务写入仍走统一调度。

工具转移协议：

```text
准备目标窗口（隐藏）
  → 源编辑器结束 composition、提交当前事务、取得会话转移锁
  → 生成文本/选区/滚动/undo 状态 + revision
  → 目标加载并 ACK 同一 revision
  → 切换唯一写入 owner、显示目标、源改占位
  → 失败则撤销转移并恢复源编辑器
```

正在运行的任务留在 AppCoordinator 中，窗口只更换订阅者，不能重新执行。编辑 undo 若不能由插件导出，应由本产品的文档事务模型持有；不能仅保存字符串却宣称与 Electron 无损分离等价。

进程退出、崩溃及窗口转移失败必须释放单写者锁。macOS 关闭主窗口但仍有工具窗口时，应用服务继续存在；真正退出时提示/保存 dirty 文档、停止子进程、结束任务、关闭数据库。

### 7.3 系统适配矩阵

| 能力 | macOS | Windows | Linux |
| --- | --- | --- | --- |
| 窗口控制 | 原生 traffic lights 与安全区 | 原生按钮/系统命中测试，保留系统窗口操作 | 优先原生装饰，按桌面环境验收 |
| 侧栏材质 | 可选原生半透明，仅侧栏 | 默认不透明；可选平台材质 | 默认不透明 |
| 托盘 | 本产品图标/菜单 | 本产品托盘/快捷方式 | 检测托盘能力；不可用则保留可见窗口 |
| 截图/取色 | 系统权限及原生捕获流程 | 多屏/DPI/负坐标原生捕获 | 区分 X11/Wayland；Wayland 优先 Portal |
| Hosts/环境写入 | 受限提权 helper | 受限提权 helper | 受限 helper，检测系统配置方式 |
| 防显示休眠 | 按会话 token 持有/释放 | 同左 | 同左，检测桌面可用性 |

表格是实现要求，不是“插件安装后自动支持”的声明。P0 冻结最低 OS 和架构：初始测试候选为 macOS 13+、Windows 10 22H2+/11、Ubuntu 22.04/24.04 LTS，结合选定 SDK、依赖与实机结果确定最终支持范围。

## 8. 数据、Vault、导入、备份

### 8.1 本产品存储布局

```text
<dataRoot>/
  product.json                # productId、schema、创建信息
  settings.json               # 版本化、原子写入；不含 secret
  database/mootool_flutter.sqlite
  workspace/                  # 草稿/恢复快照，按 session/document 划分
  vaults/json/
  vaults/quick-note/
  images/
  imports/<importId>/
  migrations/                 # 导入及 schema 迁移报告
```

数据库自行设计表名，不必为复用旧表而绑定 Java schema。建议含 histories、favorites、http_requests、http_history、host_profiles、translation_words、translation_history、image_assets、migration_runs、migration_items。Vault 正文以文件为真实来源，SQLite 只维护索引/辅助数据；避免同时把文件正文和数据库正文当权威源。

数据库连接由一个应用级服务管理，启用事务及合理 busy timeout；保存 settings 时写临时文件后原子替换，保留上一个可恢复版本。多个窗口同时改设置采用分区 patch 和 revision，不能把旧整份设置覆盖新状态。

Flutter schema 从自己的版本起步，不直接沿用 Electron 当前 schema 12。读取失败时保留原文件、停止会覆盖损坏数据的自动保存并给出恢复入口；不能将解析失败视为“没有数据”写回空默认值。

### 8.2 文档库与外部目录

- JSON/随手记默认各有本产品专属 Vault；附件位于所属 Vault 的受控相对目录。
- 用户导入文件时默认复制到本产品目录；不因导入而取得持续改写来源目录的隐式授权。
- 指定自有外部 Vault 可编辑，但先检查 realpath、目录重叠、符号链接和单写者标记。
- 检测属于其他 MooTool 产品的目录时，引导复制导入，不直接绑定其活动 Vault。仅有本产品锁无法约束旧产品写入，不能以此宣称跨产品安全共享。
- 用户自行在不同产品里选择同一个非托管目录，或主动应用系统 Hosts/环境变量，会改变共享系统资源；隔离承诺指应用默认行为和产品私有数据，不应谎称 OS 全局资源也能彼此隔离。
- watcher 收到外部变化时比较 savedRevision/content hash：无 dirty 可更新；有 dirty 提供比较、另存或保留本地，禁止无提示覆盖。
- 保存采用同目录临时文件和原子替换；移动/复制/删除需维护附件引用，避免误清理仍被其他笔记引用的附件。

### 8.3 Git

读取当前 Electron `vaultGitService.ts`、`vaultGitCheckpointScheduler.ts` 和契约。首版保留 init、remote、status、commit、fetch/pull/push、history、diff、冲突 ours/theirs、discard、abort、continue。

系统 Git 是独立可选外部工具，缺失只影响 Git 功能。操作采用固定 action + 参数数组 + 固定工作目录，不执行输入框拼接的任意 shell。

自动提交继承 Electron 默认含义：开启，空闲 30 秒、失活 120 秒，自动拉取默认 0（关闭）；须先保存完成，再提交对应 revision，不能在冲突/合并时继续调度。每个真实 Vault 只有一个调度器，分离窗口不启动副本。

Git token 在本产品 secret store；不能写入远程 URL、日志或普通设置。丢弃修改和冲突覆盖需先展示范围，可保留恢复副本；没有真实结果不得显示提交/推送成功。

### 8.4 显式跨产品导入

作为数据可携带能力，提供用户主动启动的 Java/Electron 导入；Tauri/Native 适配器可后续按格式建设。不要启动时自动扫描、接管或搬走旧数据。首屏可以提示“从其他版本导入”，但不执行。

流水线：

1. 用户选择来源，检测产品和受支持版本/schema。
2. 只读扫描；展示类别、数量、目标位置、冲突策略和将跳过的项。
3. 来源运行中无法可靠取快照时提示关闭来源或先导出；不直接复制活跃 DB 文件冒充一致快照。
4. 记录来源指纹；用户确认后再次核对变化。
5. 为 Flutter 当前数据制作可恢复快照。
6. 数据库写入事务 + Vault/图片暂存区 + 文件清单；成功后切换，失败时按日志回滚。
7. 来源内容只读，凭据不迁移；默认同名增后缀，禁止静默覆盖。
8. 用来源产品、来源记录键/路径和指纹保证幂等；生成导入/跳过/冲突/失败报告。

不承诺全部旧版数据库都通用兼容。每个适配器记录支持的来源版本，未知 schema 拒绝写入。不能简单复制 Electron settings 中的绝对路径到 Flutter 配置。

### 8.5 备份与恢复

完整备份覆盖设置、数据库一致快照、工作区草稿、图片、JSON Vault、随手记及附件，附带 manifest：productId、appVersion、schemaVersion、UTC 时间、文件清单、长度与校验值。默认不包含密钥。

不能把运行中 SQLite 的 db/WAL/SHM 文件随意逐个复制来保证一致性。使用受支持的在线备份/一致快照，或暂停写入并正确 checkpoint、关闭连接后备份；同时协调 Vault 保存使数据集有明确快照点。

备份目标不能位于任一被备份源目录内部。解包校验总大小、路径、重复项和符号链接；恢复先验证产品/schema/完整性，再建立当前数据回滚副本，暂停后台写入，切换完整数据集，失败还原。

跨产品包只走显式导入适配器，不能当作 Flutter 原生备份直接覆盖。恢复成功后重建索引/观察者，确保没有旧连接继续写旧数据。

## 9. 网络、平台服务与错误处理

本机能力采用强类型接口：FileService、ClipboardService、CaptureService、WindowService、SystemConfigService、RuntimeService、SecretStore、UpdateService。无能力返回明确 capability/reason；不能捕获异常后伪造空列表或成功。

文件访问规范化、检查真实路径和作用域，防止目录逃逸。拖入文件与原生对话框导入走同一验证通道。Markdown 预览不执行脚本；附件路径只在所属 Vault 内解析，外部 URL 由用户点击后交给系统。

HTTP/翻译必须接入统一代理、超时、取消及网络错误模型；本地文本转换不联网。Cookie 会话由 Flutter 自己持有，不能读取其他产品浏览器会话。

运行代码是用户主动执行本机程序，界面如实显示运行时、目录、参数及输出，不把它宣传为安全沙箱。系统配置 helper 与代码运行服务分开，不允许用户代码借用提权 helper 的权限。

日志使用本产品路径和结构化错误码，默认脱敏 HTTP Authorization/Cookie、密码、token、完整用户文档内容。诊断导出展示将导出的内容范围，文件系统失败不能吞掉。

## 10. 独立打包、更新及发布

### 10.1 产物规划

| 平台 | 产物命名 |
| --- | --- |
| macOS arm64/x64 | `MooTool-Next-Flutter-{version}-mac-{arch}.dmg` |
| Windows x64 安装版 | `MooTool-Next-Flutter-{version}-win-x64-setup.exe` |
| Windows x64 便携版 | `MooTool-Next-Flutter-{version}-win-x64-portable.zip`，包含完整依赖目录 |
| Linux x64 | `MooTool-Next-Flutter-{version}-linux-x64.AppImage`、同前缀 `.deb` |

Flutter Windows 产物不应只复制单独 exe；打包完整 data、DLL 及 helper。安装目录、开始菜单、卸载项、快捷方式和运行图标均使用独立名字。卸载只操作本产品文件，删除用户数据需明确选择；不遍历删除 `MooTool*`。

### 10.2 更新契约

遵循仓库 [发布约定](../../RELEASE_CONVENTIONS.md)。当前主力 Latest 为 Electron；Flutter 的 Release 使用 `make_latest: false`。

- 编译期固定 productId，更新只读取 `products.next-flutter`；没有节点/无版本时显示未发布或已是最新，不回退其他产品。
- 按 semver、stable/prerelease、OS、架构、包类型选择资产；无匹配只打开本产品 Release。
- 严格校验 HTTPS、文件名/产品、长度和清单 SHA-512；hash 用于完整性，不将其等同于代码签名。
- 下载至 Flutter 专属缓存，支持进度、取消、失败重试；校验后才显示就绪。
- 首个可发布实现可采用“下载并校验 → 打开安装包”完整手动安装闭环；自动替换/重启按各平台验证后实现并记录差异，不照搬 Electron updater。
- Flutter 签名策略独立记录。未签名预览如实说明；签名验证失败不能提示自动安装成功，也不能替用户关闭系统安全设置。
- 不修改 Java/Electron/Tauri/Native 的版本文件、tag、资产名、更新节点和更新元数据。

### 10.3 CI

未来增加 Flutter 专属 workflow，路径过滤 `next-flutter/**` 及自身 workflow，tag 仅 `next-flutter-v*`。只安装 Flutter 与本产品声明依赖，不把其他产品测试加入 Flutter 必须构建链。

流水线验证 pubspec semver、build number、tag、release notes、平台二进制版本/ID、许可证、产物清单和 hash。先上传并验证资产，再更新自身清单节点；并发发布更新根清单时重新读取、合并本节点并保留其他产品变更。

本文任务只生成开发文档，不提前创建 Release、启用更新节点或修改仓库现有发布文件。后续实现阶段按以上要求建立并实测发布能力。

## 11. Cursor 分阶段执行计划

阶段是交付顺序，不是将后期功能删出目标。没有技术难点能用“占位完成”规避。单一平台能力受阻时记录原因并继续其他可做工作；不可把整个产品停在立项验证。

| 阶段 | 交付内容 | 退出条件 |
| --- | --- | --- |
| P0 基线与工程 | 基线清单、当前截图、SDK/依赖锁定、独立 app ID/路径、三平台空壳构建；编辑器/多窗口/PDF/动态 proto 样例 | 有实际样例和 ADR；每个高风险项有可执行路径及未解决项 |
| P1 壳与设计系统 | 26 项注册、首页、导航/搜索/自定义组、沉浸工具布局、设置框架、主题、i18n、基础编辑器与任务服务 | 三语言/明暗/窄窗口可用；切工具保留内容；样式 gallery + screenshot |
| P2 JSON 完整纵向闭环 | JSON 所有操作、文档树、保存、历史、查找、JSONPath、导入导出、基础 Git；独立窗口转移 | 真实 JSON → 格式化 → 保存 → 切页 → 分离 → 收回 → 重启恢复 |
| P3 本地工具 | 编码、配置、时间、UA、计算器、正则、Cron、Diff、格式化、Crypto、QR、颜色、Protobuf | 功能规格对应语义样本通过；没有伪算法/静态输出 |
| P4 知识与数据 | 随手记全部功能、列编辑/附件/预览、Vault Git 完整、历史/收藏、备份恢复、显式导入 | 数据回滚、外部修改、重复导入与撤销隔离通过 |
| P5 网络与系统 | HTTP、翻译、Host、网络/IP、变量、系统信息、Java/Groovy/Python/Node | 真实本地服务和本机子进程、超时/取消/异常、隔离提权验收 |
| P6 媒体与桌面收敛 | 图片/SVG、PDF、留言板、截图/取色、托盘/防休眠、全部工具多窗口 | 多屏/DPI/权限矩阵；跨窗口不丢状态；所有功能有明确状态 |
| P7 产品化 | 次要外观预设补齐、全量差异审查、性能、平台安装包、并存/升级/卸载、更新 | [发布 DoD](acceptance.md) 全部满足或有清楚标注的发行范围 |

P0 的 PDF/proto/编辑器样例是为了尽早消除错误选型，不要求先造完整工具页面。P2 的 Git 可先主流程，P4 收敛冲突/撤销/自动调度。功能需迭代时，每次交付一个真实用户流程，不能反复只重写规划。

### 11.1 每轮开发操作

1. 阅读本轮工具源码、测试和配套功能规格，列出可见控件、默认值及缺失项。
2. 检查工作树，仅修改 Flutter 范围；已有外部修改完整保留。
3. 先实现领域模型和持久化/服务路径，再接控件；UI 可先局部预览，但交付必须连真实路径。
4. 执行有意义的单元/集成/界面检查；输入编辑、分离窗口和系统对话框需实际操作。
5. 捕获当前 Flutter 截图，与冻结的 Electron 及 Flutter token 规范分别比较。
6. 更新 `docs/evidence/`、验收表和进度：实现、测试结果、平台、已知差异、下一步。

### 11.2 建议命令

仅在 Flutter 工程创建后使用；本次文档生成没有运行这些命令。

```bash
cd next-flutter
flutter --version
flutter doctor -v
flutter pub get
dart format --output=none --set-exit-if-changed lib test integration_test
flutter analyze
flutter test
flutter run -d macos
flutter test integration_test -d macos
flutter build macos --release
```

Windows/Linux 在对应平台选择设备并执行对应 build。目录尚未创建时不要照抄不存在的测试路径；脚本须先检查环境并给出实际错误。

`integration_test` 用于应用内流程；系统权限弹窗、托盘、安装器、辅助窗口、真实 IME 和跨应用剪贴板仍需平台自动化或人工记录。不能用 Widget test 通过来证明上述原生行为。参考 [Flutter 集成测试](https://docs.flutter.dev/testing/integration-tests)。

### 11.3 必须形成的 ADR

| ADR | 必须说明 |
| --- | --- |
| 001 编辑器 | 选型、列编辑、中文 IME、大文本、undo 模型及状态转移 |
| 002 多窗口 | engine 模型、服务 owner、插件注册、消息与失败回滚 |
| 003 平台矩阵 | 最低系统、架构、Wayland、截图/取色/托盘限制 |
| 004 复杂算法 | 动态 proto、JSONPath、格式化、PDF、SM 算法的实际实现与许可证 |
| 005 数据 | schema、文件权威源、原子写入、备份一致性、跨产品导入 |
| 006 更新 | 安装格式、独立通道、签名/手动安装策略、回退 |
| 007 UI 差异 | 明确现代化变化、次要预设、快捷键与辅助技术差异 |

只有真实验证后才能将选型标记为“采纳”；暂缺环境写“未验证”。依赖调整和普通工程决定可自行记录推进；涉及删除目标功能、共享产品数据或改变产品独立性时才需要用户决定。

## 12. 完成定义

首个完整 Flutter 产品必须同时具备：真实功能闭环、与 Electron 熟悉的布局/操作一致、现代且可访问的明暗 UI、可靠编辑和多窗口、可恢复数据、独立安装及更新。

验收以 [acceptance.md](acceptance.md) 和 [feature-parity.md](feature-parity.md) 为准。所有“通过”均附本产品测试证据；Electron 历史测试数量、Tauri/Native 的交付状态和依赖包说明不能当作 Flutter 已完成的证据。
