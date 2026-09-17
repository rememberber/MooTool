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
| Hero/Modern 界面风格、统一背景 | 有 | 无 | **批准差异**：Tauri 固定现代壳层 |
| 设置页分栏可调宽度 | `ResizableColumns` | 已补齐 | 设置窗口 |
| 设置分类（布局/网络/Vault/快捷键/关于） | 12 类 | 已对齐信息架构 | 2026-09-17 拆分 |
| macOS 毛玻璃强度 | Chromium 半透明 | 原生标题栏 | **C 级**，ADR-010 |
| 沉浸式工具壳层类名 | `app-shell--immersive-tool` | 已补齐 | 工具页限制标题栏拖拽区域至侧栏宽度 |

## 功能差异（明确不做或后续）

| 项目 | 说明 |
| --- | --- |
| AI 集成（MCP/Skill 安装） | Electron 独有 | **批准差异**（[ADR-012](./adr/012-ai-integration-scope.md)） |
| 随手记 Vault 文件树 + Git 工作区 | Electron 文件模型；Tauri SQLite 模型，[ADR-011](./adr/011-quick-note-data-model.md) |
| Vault Git 远程/自动拉取等完整设置页 | Electron 设置 → Vault；Tauri JSON 工作台内 Git + 自动提交开关 |
| SQL 方言等编辑器扩展项 | Electron 设置；Tauri 未暴露（无对应工具面） |
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
