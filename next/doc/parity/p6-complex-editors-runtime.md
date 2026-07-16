# P6 复杂编辑器、Vault 与运行时 Parity Checklist

> 阶段：P6
> 状态：实现完成，待 Java 对照验收
> Java 基线：当前 `github-develop-ts` 分支 Java 版
> 最后更新：2026-07-16

## 1. Java 基线审计

Quick Note 以 `QuickNoteForm`、`QuickNoteListener`、`QuickNoteVaultUtil`、`QuickNoteFrontmatter`、`QuickNoteTreeDragDrop`、`QuickNoteAttachmentUtil`、Markdown 编辑器组件、Git 对话框和自动同步调度器为行为基线。代码运行以 `JavaConsoleForm` 和 `JavaConsoleListener` 为基线；旧版实际使用 GroovyShell 执行 Java/Groovy 片段，并提供格式化、清空、结果和历史。

`SyncAndBackupDialog` 的全局远程同步输入及按钮在 Java 版中均被禁用，并显示“功能尚未实现”。因此本轮 parity 范围是可见存储路径与本地备份；Quick Note 和 JSON 的实际远程同步由各自 Git Vault 提供，不额外建设未完成的全局同步协议。

## 2. Quick Note

| 能力 | Java 基线 | Next 实现 | 状态 |
| --- | --- | --- | --- |
| Vault 格式 | `.txt` + YAML frontmatter | 兼容标题、语法、字体、字号、颜色、换行和时间元数据 | 待对齐验收 |
| 文件树 | 文件夹、笔记、增删改移、复制 | 嵌套目录、创建、重命名、移动、复制、删除和打开 Vault | 待对齐验收 |
| 搜索与排序 | 标题/内容搜索，多种排序 | 标题/路径/正文搜索，名称/创建/修改排序，`.gitignore` 过滤 | 待对齐验收 |
| 编辑器 | 语法、字体、字号、颜色、换行、列表 | 对应工具栏、行号、自动保存、文档统计和状态栏 | 待对齐验收 |
| Markdown | 编辑与预览 | 编辑、分栏、预览，`marked` 渲染并经 DOMPurify 清理 | 待对齐验收 |
| 图片附件 | 插入、引用与清理 | 导入、读取、插入 Markdown、孤立附件清理 | 待对齐验收 |
| 查找与快速替换 | 查找替换与批量文本转换 | 查找替换、空白/引号/分隔符/排序/转义等快速替换 | 待对齐验收 |
| 导入导出 | 文本导入与笔记导出 | 原生文件对话框导入、导出当前笔记 | 待对齐验收 |
| Git Vault | 初始化、状态、提交、拉取、推送、历史、Diff | 复用安全 Git 服务及独立 Quick Note IPC，支持自动提交和定时拉取 | 待对齐验收 |
| 外部变化 | Vault watcher | 文件监视后安全刷新树与当前笔记 | 待对齐验收 |

Repository 对所有相对路径执行规范化、真实路径和符号链接检查，阻止路径穿越和越界访问；保存、移动和删除会维护附件生命周期。切换笔记前先保存当前内容，未检测到 Vault 时自动创建 Java 风格欢迎笔记。

## 3. 代码运行

| 能力 | Next 实现 | 状态 |
| --- | --- | --- |
| 运行时结构 | Java/Groovy、Python、Node.js 三个主 Tab；Java 主 Tab 内切换 Java/Groovy | 待对齐验收 |
| 本机检测 | 自动检测命令、版本和设置覆盖路径；单一运行时缺失不影响其他 Tab | 待对齐验收 |
| 编辑与格式化 | 每种运行时独立草稿；Java、Groovy、Python、Node.js 按语言格式化 | 待对齐验收 |
| 参数与目录 | 每种运行时独立持久化程序参数和工作目录 | 待对齐验收 |
| 执行与输出 | 主进程受控子进程、stdout/stderr 流式输出、退出码、耗时和命令摘要 | 待对齐验收 |
| 停止与限制 | 进程树停止、超时、1 MB 源码限制、2 MB 输出限制和临时目录回收 | 待对齐验收 |
| 历史 | 复用 Java 兼容功能历史，支持查看和恢复输入/输出 | 待对齐验收 |
| 设置迁移 | AppSettings schema v3 自动补齐旧配置缺少的草稿和运行参数 | 已验证 |

Renderer 不接受 shell 字符串，也不直接访问 Node API。主进程只将验证后的参数数组传给 `spawn`，工作目录必须是真实存在的目录，子进程环境变量使用白名单。Java 源文件模式会按公开 class、record、interface 或 enum 名称生成临时文件。

## 4. 数据与备份

- 设置页展示数据目录、数据库、配置文件和图片目录的真实位置，并支持在系统文件管理器中打开。
- 可导出完整备份、数据库、配置或图片；完整备份包含数据库及 WAL/SHM、Electron 设置、图片、Quick Note Vault 和 JSON Vault。
- 备份写入时间戳目录，并拒绝把目标目录放进任一源目录内部，避免递归复制。
- 全局远程同步未实现，因为 Java 基线本身处于禁用状态；Quick Note 和 JSON 使用各自 Git Vault。

## 5. 自动化与视觉证据

- `quickNoteVaultRepository.integration.test.ts`：CRUD、旧 frontmatter、`.gitignore`、文件夹删除、附件清理、路径穿越与符号链接拒绝。
- `quickReplace.test.ts`：快速替换规则与边界。
- `runtimeTools.test.ts`：格式化、参数解析和输入校验。
- `runtimeExecutionService.integration.test.ts`：真实 Node 执行、参数传递、流式输出、取消和恶意请求拒绝。
- `backupService.integration.test.ts`：完整/分类备份内容与路径边界。
- `tests/electron/app.spec.ts`：Quick Note 保存、Markdown、快速替换、Git；Node 运行/停止/历史；设置备份入口。
- 全量 Vitest：33 个测试文件、101 项测试通过；集成资源并发固定为最多 4 个 worker。
- Electron Playwright：13 项 E2E 通过。
- TypeScript 类型检查、Electron/Vite 生产构建和 `git diff --check` 通过。
- React Doctor：`100 / 100`，无诊断问题。
- `1440 x 920` 浅色、`1080 x 720` 深色和 `920 x 700` 设置窗口共 6 张截图通过零溢出与关键区域边界审计。

截图位置：`doc/screenshots/p6-*.png`

## 6. P7 共享验收跟踪

- [ ] 使用 Java 版同尺寸截图完成 Quick Note 和代码运行的人工 parity sign-off。
- [ ] Quick Note 当前以移动对话框替代树节点拖拽；评估是否必须补齐直接拖拽手势。
- [ ] 补齐 Git 冲突逐项解决、丢弃工作区和中止合并等高级入口。
- [ ] 使用真实旧 Quick Note 数据库和 Vault 样本执行迁移、附件与 frontmatter 回归。
- [ ] 在 macOS、Windows、Linux 安装包中分别验证 Java、Groovy、Python、Node.js 路径、参数、取消和进程树回收。

以上项目属于 P7 parity、数据迁移和跨平台发布验收，不影响 P6 功能实现阶段已完成的结论。
