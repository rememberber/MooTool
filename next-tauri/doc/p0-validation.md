# P0 技术验证记录

> 状态：工程基线记录已归档；自 2026-07-29 起不再作为开发 Go/No-Go 闸门
> 日期：2026-07-29
> 产品：MooTool Next Tauri `0.1.0`
> 验证环境：macOS 26.6 / x86_64 / Apple Clang 17 / Rust 1.97.1 / Tauri 2.11.5

## 1. 本轮目标

本轮先建立一个可以持续扩展的独立产品基线，不以迁移 Electron 源码为实现路径：

- `next-tauri` 独立维护 NPM 包、锁文件、Rust crate、Tauri 配置与构建产物。
- 固定产品名 `MooTool Next Tauri` 和应用 ID `com.rememberber.mootool.next.tauri`。
- 建立 React 工作台、独立工具注册表与 Tauri-owned API。
- 完成 Calculator 首个纵向切片。
- 由 Rust Command 返回产品身份和平台信息。
- 建立前端单元测试、Rust 单元测试和产品边界检查。
- 建立按工具隔离的 Rust-owned WebView 生命周期与会话管理器。
- 将真实 Calculator 页面接入独立工具 WebView。
- 验证 macOS 上输入与快捷键、停靠、分离、收回、隐藏/恢复、关闭/重启以及 100 次 reparent 循环。
- 将真实 CodeMirror 6 编辑器接入第二个独立工具 WebView，验证 Unicode、选区、查找、中文拼音 IME 与状态保持。

## 2. 已实现验证面

| 验证项 | 当前实现 | 验收方式 | 状态 |
| --- | --- | --- | --- |
| 独立工程 | 自有 `package.json`、`package-lock.json`、`Cargo.toml`、`Cargo.lock`、Tauri 配置 | `npm run check:boundaries` | 通过 |
| 产品身份 | 独立 product name / app ID / crate name | 边界脚本 + Rust 测试 | 通过 |
| Tauri-owned API | `RuntimeApi` 与 `ToolWebviewApi` 封装领域 Command | Vitest | 通过 |
| Rust Command | 返回运行时；按工具管理 WebView 生命周期、会话与状态摘要 | Cargo test + release 桌面运行 | 通过 |
| 工作台 | 首页、分组导航、搜索、最近使用、紧凑导航 | 原生窗口视觉与交互检查 | 基线通过 |
| 会话保持 | Home 与 Calculator 切换时隐藏/恢复同一工具 WebView | `123 * 7 = 861` 会话切出再切回 | 通过 |
| Calculator | 表达式、进制、GCD/LCM、排列组合、记录；独立子 WebView | Vitest + 原生窗口交互 | 通过 |
| CodeMirror / IME | CodeMirror 6、JSON 高亮、历史、Unicode 选区、外部/原生查找、IME 事件计数；独立子 WebView | Vitest + macOS 中文拼音原生输入 | macOS 基线通过 |
| 多 WebView 模型 | Shell 控制层 + Calculator / CodeMirror / 探针隔离会话 + Rust-owned Manager | Rust 单测 + macOS 原生窗口 | 基线通过 |
| reparent 压力验证 | 主 Window 与普通原生 Window 之间移动真实 Calculator / CodeMirror WKWebView | 每个工具 100 个往返周期 | macOS 通过 |
| WebView 权限边界 | Shell 与 `tool-calculator` / `tool-editor-lab` / `p0-tool-probe` 分配独立 Capability；Rust 校验调用方 label | 配置检查 + 原生 IPC 上报 | 通过 |
| 桌面生命周期 | Rust-owned 窗口状态、原生菜单/托盘、关闭策略和登录启动 | Rust/Vitest + macOS 进程、窗口与 Accessibility 状态 | macOS 通过 |
| 系统主题 | 跟随 macOS 浅色/深色 | 原生窗口视觉检查 | 深色通过，浅色待补 |

## 3. 兼容性基线矩阵

| 风险项 | macOS | Windows | Linux | P0 结论 |
| --- | --- | --- | --- | --- |
| 主窗口与系统 WebView | release 构建与原生窗口启动通过 | 未验证 | 未验证 | macOS 基线通过 |
| 工具 WebView 停靠/分离与 `reparent()` | Calculator 创建、分离、收回、关闭/重启通过；累计 202 次 reparent | 未验证 | 未验证 | macOS 机制 Go |
| WebView 状态在切换/分离后保持 | 100 个往返周期后页面加载仍为 1，会话、表达式、结果和历史保持 | 未验证 | 未验证 | macOS 机制 Go |
| 工具 WebView 隐藏/恢复 | 切换到 Home 时隐藏，返回 Calculator 后同一会话和状态恢复 | 未验证 | 未验证 | macOS 机制 Go |
| 系统 WebView 编辑器与 IME | CodeMirror 6 中文拼音输入提交、Unicode 选区、外部/原生查找、隐藏/恢复及 100 个往返周期通过；日文字符显示通过，日文 IME 输入待补 | 未验证 | 未验证 | macOS 中文 IME 机制 Go |
| 窗口状态、菜单、托盘和关闭 | 关闭拦截保持同一原生窗口；最大化状态持久化；菜单隐藏后应用 `AXHidden=1` 且进程存活，显示后 `AXHidden=0` | 未验证 | 未验证 | macOS 生命周期通过 |
| 页面截图 | 未验证 | 未验证 | 未验证 | 待定 |
| 屏幕截图与区域选择 | 生产实现已完成，Codex 非交互会话无法枚举显示器 | 由发布矩阵验收 | 由发布矩阵验收 | 不作为立项闸门；行为见 ADR-010 |
| 屏幕取色 | 生产实现已完成，复用显示器快照可视化取色 | 由发布矩阵验收 | X11/Wayland 按 ADR-010 验收降级 | 不作为立项闸门；行为见 ADR-010 |
| 多语言与最小窗口 | 中文基线进行中；CodeMirror 日文字符显示通过 | 未验证 | 未验证 | 待定 |

注意：

- Calculator 是首个工具 WebView 会话切片；当前 25 个正式工具均已接入同一 Rust-owned 生命周期模型，并继续按各自状态和能力独立增强。
- CodeMirror 实验台仍是系统 WebView/IME 的工程探针，不是从 Electron 迁移的共享编辑器；正式 JSON 工作台已在后续开发阶段以独立功能实现，随手记等其他工具仍按路线图推进。
- Tauri `2.11.5` 的 `reparent()` 已公开，但创建普通原生 Window 和子 WebView 的 Rust API 需要 `tauri/unstable`。当前版本必须精确锁定，升级时重复压力验证。
- Windows WebView2 与 Linux WebKitGTK 尚无真实环境结论；这些结论转为持续兼容性待办，不阻塞正式功能开发。

## 4. 独立性证据

- 前端通过 `src/platform/api` 访问 Tauri Command，不声明或读取 `window.mootool`。
- `src` 和 `src-tauri/src` 不引用 `next/src`、`next/electron` 或 `next/out`。
- NPM 依赖中不存在 `electron`、`electron-*`。
- Rust 产品常量、Tauri 应用 ID 与包名均使用 `next-tauri` 身份。
- 边界检查被纳入 `npm run check`，后续回归会阻止明显的 Electron 源码耦合。

## 5. 后续兼容性待办（非开发前置）

1. 在 Windows、Linux 真实环境重复 100 次 Calculator reparent 压力验证。
2. 在 macOS 补日文 IME，并在 Windows/Linux 重复 CodeMirror/IME 与 100 次 reparent 验证。
3. 验证 SQLite、运行时 Channel、页面截图、区域截图、系统取色和权限恢复路径。
4. 补齐 macOS 浅色、最小窗口和英文/日文布局验证。

以上事项按对应功能和发布里程碑安排，不再要求在 P1/P2 开发前集中完成。

## 6. 本轮命令结果

```text
npm run check
  product boundary check: passed
  TypeScript: passed
  Vitest: 5 files / 7 tests passed
  Vite production build: passed
  Cargo tests: 6 passed

npm run build:desktop
  release profile: passed
  output: src-tauri/target/release/mootool-next-tauri
  artifact: x86_64 Mach-O, 10.0 MB (10,473,988 bytes), local unsigned build
```

原生窗口检查结果：

- 2026-08-09 桌面生命周期复验：定向 `⌘W` 后窗口 ID 与进程保持；窗口状态文件记录 Retina 物理尺寸和最大化标记；原生菜单隐藏后应用 `AXHidden=1`，显示后恢复为 `AXHidden=0`。
- 窗口标题为 `MooTool Next Tauri`，单实例测试窗口成功显示。
- 首页读取到 `v0.1.0`、`next-tauri`、`tauri`、`macos · x86_64`，证明前端到 Rust Command 的通路正常。
- Calculator 子 WebView 首次加载会话 ID 为 `06b9c8e3-0a28-436c-9b46-0dbfe277f713`；通过键盘全选、粘贴和按钮触发将 `123 * 7` 计算为 `861`，历史记录增至 2 条。
- 手工分离到 `MooTool Calculator` 普通原生 Window 后，表达式、结果和两条历史保持；收回主窗口后会话 ID 仍相同。
- 随后执行 100 个分离/收回往返周期，Rust Manager 记录累计 `202` 次 reparent，页面加载始终为 `1`，会话 ID、Calculator 完整状态摘要均保持，压力结论为通过。
- Calculator → Home 时停靠子 WebView 正确隐藏；由首页再次打开后恢复相同会话、`123 * 7 = 861` 和两条历史，未触发页面重载。
- 工具栏关闭 Calculator 后出现未启动占位页；重新启动生成新会话 ID，证明关闭/重建生命周期可恢复。
- CodeMirror 子 WebView 原生验证会话 ID 为 `210cc860-7fdf-47e8-8704-b13c68137087`。外部查找“世界”后下一处命中选区为 `47:49`，程序化 Unicode 选区“你好，世界”为 `44:49`；CodeMirror 自带查找面板可预填查询并高亮两处命中。
- 在真实 macOS 拼音输入法下输入并提交“测试”，内容增至 `168` 字符、光标位于 `168:168`、第 12 行，CodeMirror 捕获到完整的 `compositionstart/compositionend = 1/1`。
- CodeMirror 手工分离、收回后会话、内容、选区、查找词与 IME 计数均保持；随后 100 个往返周期累计 `202` 次 reparent，页面加载保持为 `1`，压力结论为通过。
- CodeMirror → Home → CodeMirror 后恢复同一会话及 `168` 字符状态，已提交的“测试”、`IME 1/1` 和压力结果均未丢失。
- CodeMirror 初始内容中的日文字符 `こんにちは世界` 在 WKWebView 下显示正常；本轮未执行日文输入法 composition 验收。
- 深色模式下完成一次对比度复查并修正计划中导航及次级按钮颜色。
- 工具状态探针首次加载后会话 ID 为 `225d0abf-f717-4a2f-94c2-59ea6db821b2`，将计数器改为 `3`、草稿改为 `p0-macos-reparent-225d`。
- 手工分离到普通原生 Window 后，会话 ID、计数器和草稿保持；收回主窗口后仍保持。
- 随后执行 100 个分离/收回往返周期，Rust Manager 记录累计 `202` 次 reparent，页面加载始终为 `1`，压力结论为通过。
- 从实验台切到 Calculator 时，停靠子 WebView 正确隐藏；返回实验台后恢复，状态和压力结论不变。
- 关闭工具子 WebView 后，原生内容消失且生命周期按钮回到未创建状态。
