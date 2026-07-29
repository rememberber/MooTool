# P0 技术验证记录

> 状态：独立工程基线完成，macOS 多 WebView / reparent 机制验证通过，P0 继续  
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
- 建立 Rust-owned 工具 WebView 生命周期管理器和独立子 WebView 状态探针。
- 验证 macOS 上停靠、分离、收回、隐藏/恢复、关闭以及 100 次 reparent 循环。

## 2. 已实现验证面

| 验证项 | 当前实现 | 验收方式 | 状态 |
| --- | --- | --- | --- |
| 独立工程 | 自有 `package.json`、`package-lock.json`、`Cargo.toml`、`Cargo.lock`、Tauri 配置 | `npm run check:boundaries` | 通过 |
| 产品身份 | 独立 product name / app ID / crate name | 边界脚本 + Rust 测试 | 通过 |
| Tauri-owned API | `RuntimeApi` 与 `ToolWebviewApi` 封装领域 Command | Vitest | 通过 |
| Rust Command | 返回运行时；管理工具 WebView 生命周期与状态探针 | Cargo test + release 桌面运行 | 通过 |
| 工作台 | 首页、分组导航、搜索、最近使用、紧凑导航 | 原生窗口视觉与交互检查 | 基线通过 |
| 会话保持 | Home 与 Calculator 切换时保留已挂载状态 | 将 `9*9 = 81` 会话切出再切回 | 通过 |
| Calculator | 表达式、进制、GCD/LCM、排列组合、记录 | Vitest + 原生窗口交互 | 通过 |
| 多 WebView 原型 | Shell 控制层 + Rust-owned 子 WebView 状态探针 | macOS 原生窗口 | 通过 |
| reparent 压力验证 | 普通原生 Window 与主 Window 之间移动同一个 WKWebView | 100 个往返周期 | macOS 通过 |
| WebView 权限边界 | Shell 与 `p0-tool-probe` 分配独立 Capability；Rust 校验调用方 label | 配置检查 + 原生 IPC 上报 | 通过 |
| 系统主题 | 跟随 macOS 浅色/深色 | 原生窗口视觉检查 | 深色通过，浅色待补 |

## 3. P0 Go/No-Go 矩阵

| 风险项 | macOS | Windows | Linux | P0 结论 |
| --- | --- | --- | --- | --- |
| 主窗口与系统 WebView | release 构建与原生窗口启动通过 | 未验证 | 未验证 | macOS 基线通过 |
| 工具 WebView 停靠/分离与 `reparent()` | 状态探针创建、分离、收回、关闭通过；累计 202 次 reparent | 未验证 | 未验证 | macOS 机制 Go |
| WebView 状态在切换/分离后保持 | 100 个往返周期后页面加载仍为 1，会话、计数器、草稿保持 | 未验证 | 未验证 | macOS 机制 Go |
| 工具 WebView 隐藏/恢复 | 切换到 Calculator 时隐藏，返回实验台后恢复且状态保持 | 未验证 | 未验证 | macOS 机制 Go |
| 页面截图 | 未验证 | 未验证 | 未验证 | 待定 |
| 屏幕截图与区域选择 | 未验证 | 未验证 | 未验证 | 待定 |
| 屏幕取色 | 未验证 | 未验证 | 未验证 | 待定 |
| 多语言与最小窗口 | 中文基线进行中 | 未验证 | 未验证 | 待定 |

注意：

- 本轮验证的是独立状态探针子 WebView，证明 macOS 窗口机制可行；Calculator 还没有改为独立工具 WebView，不能据此宣称工具架构已经完成。
- Tauri `2.11.5` 的 `reparent()` 已公开，但创建普通原生 Window 和子 WebView 的 Rust API 需要 `tauri/unstable`。当前版本必须精确锁定，升级时重复压力验证。
- Windows WebView2 与 Linux WebKitGTK 尚无真实环境结论，P0 的跨平台 Go/No-Go 仍未完成。

## 4. 独立性证据

- 前端通过 `src/platform/api` 访问 Tauri Command，不声明或读取 `window.mootool`。
- `src` 和 `src-tauri/src` 不引用 `next/src`、`next/electron` 或 `next/out`。
- NPM 依赖中不存在 `electron`、`electron-*`。
- Rust 产品常量、Tauri 应用 ID 与包名均使用 `next-tauri` 身份。
- 边界检查被纳入 `npm run check`，后续回归会阻止明显的 Electron 源码耦合。

## 5. 待完成

1. 将 Calculator 接入正式工具 WebView 会话模型，并验证焦点、快捷键和窗口关闭恢复。
2. 在 Windows、Linux 真实环境重复 100 次 reparent 压力验证。
3. 验证页面截图、区域截图、系统取色和权限恢复路径。
4. 补齐 macOS 浅色、最小窗口和英文/日文布局验证。
5. P0 评审后决定是否进入 P1 基础设施开发。

## 6. 本轮命令结果

```text
npm run check
  product boundary check: passed
  TypeScript: passed
  Vitest: 3 files / 5 tests passed
  Vite production build: passed
  Cargo tests: 3 passed

npm run build:desktop
  release profile: passed
  output: src-tauri/target/release/mootool-next-tauri
  artifact: x86_64 Mach-O, 9.9 MB, local unsigned build
```

原生窗口检查结果：

- 窗口标题为 `MooTool Next Tauri`，单实例测试窗口成功显示。
- 首页读取到 `v0.1.0`、`next-tauri`、`tauri`、`macos · x86_64`，证明前端到 Rust Command 的通路正常。
- Calculator 将 `9*9` 计算为 `81` 并追加记录。
- Home → Calculator 往返后，`9*9 = 81` 的输入、输出与记录保持。
- 深色模式下完成一次对比度复查并修正计划中导航及次级按钮颜色。
- 工具状态探针首次加载后会话 ID 为 `225d0abf-f717-4a2f-94c2-59ea6db821b2`，将计数器改为 `3`、草稿改为 `p0-macos-reparent-225d`。
- 手工分离到普通原生 Window 后，会话 ID、计数器和草稿保持；收回主窗口后仍保持。
- 随后执行 100 个分离/收回往返周期，Rust Manager 记录累计 `202` 次 reparent，页面加载始终为 `1`，压力结论为通过。
- 从实验台切到 Calculator 时，停靠子 WebView 正确隐藏；返回实验台后恢复，状态和压力结论不变。
- 关闭工具子 WebView 后，原生内容消失且生命周期按钮回到未创建状态。
