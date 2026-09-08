# 可直接交给 Cursor 的任务提示词

使用方式：建议打开 `next-compose/`；如果打开仓库根目录，明确所有路径以仓库根的 `next-compose/` 为工作范围。嵌套 `.cursor/rules` 是否启用取决于工作区设置，因此第一条消息始终明确要求读取文档。

以下任务描述默认会实施编码；本次文档编写没有执行这些后续任务。根据准备开展的阶段复制相应段落，不必一次要求完成整个产品。

## 1. 启动：P0、P1 和 JSON 纵向切片

```text
请在 /Users/zhoubo/IdeaProjectsCE/MooTool/next-compose 开发 MooTool Next Compose。

先读 AGENTS.md、README.md、docs/cursor-development-guide.md、docs/baseline.md、
docs/ui-spec.md、docs/architecture.md、docs/data-platform-release.md、
docs/feature-parity.md、docs/acceptance.md。检查 Git 状态并保留已有修改。

目标是独立 Compose Multiplatform Desktop 产品，主要对齐当前 next/ Electron 的
布局、操作和功能，用 Java 版补充 JVM 算法；在此基础上统一现代桌面 UI。
各版本产品线独立，允许重复代码，禁止依赖相邻产品构建/运行/资源/数据库。

本轮完成 P0 + P1 + JSON 最小真实纵向切片，不要一次铺满 25 个假工具。
先冻结可构建的 Kotlin/Compose/Gradle/JDK 组合并创建独立 Wrapper 工程。
验证编辑器的中文输入、列编辑、undo、弹层与窗口转移；验证动态 proto 和打包风险。
完成 26 入口注册、导航、搜索、modern 明暗、基础设置、会话/分离收回与自有数据路径。
JSON 切片要能真实导入、编辑、校验、格式化/压缩、查找、复制/导出、undo、历史恢复。
其余工具可以明确显示“尚未实现”，不能返回 mock 成功。

使用本产品临时 profile 测试，不能写其他版本个人数据。完成本机测试、独立目录构建、
安装镜像启动与必要 UI 检查；把真实版本、命令结果、截图及未测平台记入 docs/evidence。
更新 docs/acceptance.md 和 README。不要把 JSON 切片标记成完整 P2/F04 通过。
在已有授权范围内持续完成工作，最终报告结果、验证证据、限制和下一轮具体任务。
```

## 2. 完成一个工具

把 `<F编号/ToolId>` 替换为实际条目，例如 `F09/http`。

```text
继续 next-compose 的 <F编号/ToolId>。先读本产品约束、对应功能/UI/架构/验收章节。
查阅 docs/baseline.md 指向的 Electron 当前页面、相关算法/服务/测试及 Java 对照。
先列出实际 Tab、按钮、选项、默认值、数据字段、主流程、错误与取消语义，再实施。

完成真实算法/IO、UI、会话、历史或收藏、设置联动、保存恢复和键盘行为。
不要只做默认 happy path，也不要用一个通用 TextField 替换该工具原有多面板工作流。
复杂参数以源测试冻结；源实现错误可修正，但保留 sourceObserved 与 composeExpected。
构建和运行必须仅依赖 next-compose；可授权参考代码可复制后独立维护。

运行与本功能相关的 fixture、集成、UI 和必要平台检查，补明暗/默认/错误截图。
更新逐项验收及证据，说明具体未完成项；未运行的检查明确写未测。
```

## 3. 编辑器与多窗口专项

```text
请完成 next-compose 的 EditorHost 和多窗口所有权验收。
重点读取 docs/architecture.md 第 5–6 节和 docs/acceptance.md T04–T07。

验证 5 MiB 中文/emoji/Tab 文本、中文预编辑、普通/矩形选择、块粘贴、查找替换、
格式化一次 undo、软换行、选区/滚动、自动保存、深色与 DPI。
执行编辑→分离→编辑→收回→undo，并检查只存在一个文档写入者和一套事件监听。
检查搜索/设置/菜单/对话框是否被 Swing 遮挡；关闭后焦点是否返回。

先修复当前实现，不将功能困难直接改成“兼容差异”；如首选组件确实无法满足，
做具体替代实验并形成 ADR，说明已验证能力与尚缺能力。不得以普通文本框交差。
使用实际桌面交互证据补充测试，不能仅靠 headless 单元测试宣称输入法通过。
```

## 4. 布局与现代 UI 校准

```text
按 docs/ui-spec.md 审查并修复 next-compose 的实际页面。
保持当前 Electron 的入口顺序、主要面板关系与操作位置；改善层级、对齐、密度、
明暗对比、焦点、tooltip、错误就近反馈和窄窗口行为。

使用相同测试内容，核对 1440×920 与 1080×720、三语言和适用 DPI；重点检查
JSON/随手记/HTTP/设置/独立窗口，不把沉浸式工具区改成大标题/大卡片。
调整公共 Token 和组件以保证一致，避免逐页硬编码。截图必须来自运行的程序。
最终提供变更前后、已检查状态和仍受平台限制的行为；更新验收证据。
```

## 5. 独立安装与更新验收

```text
按 docs/data-platform-release.md 完成 next-compose 的打包和并存验收。
检查 ProductIdentity、bundle/AppUserModelID、安装名、快捷方式、升级 UUID、
数据/缓存/日志/凭据/锁、helper 和更新 next-compose 节点，不能只检查包名。

在隔离系统中与 Java/Electron 并存，验证各自主题/草稿/历史不互相影响，
Compose 升级及卸载不改其他产品。启动自带 runtime 的最终镜像，确认无相邻产品依赖。
更新只比较 next-compose 的 SemVer，错误架构/坏大小或 hash 不得安装。
首版是下载校验后打开安装包；无真实自动安装服务时不要显示“自动安装成功”。

各 OS 在对应 runner 构建/启动。准确记录已测/未测平台、签名和权限状态。
若本轮未要求公开发布，只完成可审查的包、配置和证据，不创建线上 Release。
```

## 6. 继续上次工作

```text
继续 next-compose。先检查实际 Git 状态、README、docs/acceptance.md、最近 evidence
和相关 ADR，核实已完成的代码及测试，不从头重建，也不相信没有证据的完成标记。
选取当前阶段最小且完整的未完成工作流，保持产品独立边界和既定 UI/功能规格。
修复本轮问题并完成相应检查后更新进度；不要批量把其他未验证功能标成完成。
最终说明本轮真实变化、验证、剩余问题和下一步。
```
