# DIFF-529：编辑器设置按工具生效 + F05 运行台换行 + Vault Git discard + 检查器 Schema 守卫

## 背景

DIFF-528「未做」：将 `EditorSettingsLiveApply` 扩展到 Host/QuickNote/CodeRun（按 Electron 是否即时生效）、feature-parity 小缺口、Vault Git 走查、JSON 检查器收尾。Host 无软换行控件；Runtime 硬编码不换行；随手记新建笔记应使用全局 softWrap 而非当前会话 toggle。

## 行为

### A01 设置 · 编辑器

- `EditorSettingsLiveApply`：`httpEditorWrap`、`newQuickNoteLineWrap`、`runtimeEditorWrap()`（对齐 Electron `RuntimeTool.tsx` `wrap={false}`）。
- `HttpScreen` 请求/响应 `EditorHost` 经 `httpEditorWrap`；`CodeRunScreen` 使用 `runtimeEditorWrap()`。
- `QuickNoteScreen` 新建 Vault 笔记 `lineWrap` 取 `newQuickNoteLineWrap(settings.editor.softWrap)`（对齐 Electron `createQuickNote`）。

### F05 代码运行

- 全局「编辑器自动换行」不再作用于运行台代码区（Electron 亦如此）。

### F01/F04 Vault Git

- `GitDiscardDuringMergeTest.discardRestoresTrackedConflictMarkdownDuringMerge`：随手记式路径 `notes/conflict.md` 在 merge 冲突期 discard。

### F04 JSON 检查器

- `jsonInspectorInferSchemaEnabled`：结构解析失败时不展示「生成 JSON Schema」；`JsonEngine.inferJsonSchema` 非法 JSON 单测。

## 验证

- `EditorSettingsLiveApplyTest` / `JsonInspectorStructureTest` / `JsonEngineTest.inferJsonSchema_rejectsInvalidJson` / `GitDiscardDuringMergeTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）
- 可选：`MOOTOOL_HTTP_MULTIPART_SMOKE=1` → `HttpEngineTest.optionalHttpBinMultipartPostSmoke`（默认 CI 跳过；DIFF-528 引擎已覆盖）

## 未做

六套 CSS 皮肤、产品主窗全 Tab 走查、multipart 文件 Tab UI、TCC 系统对话框 PNG 手工验收、P7 三平台安装/公证/升级卸载、Host 软换行（Electron 无）、随手记已打开笔记随全局 softWrap 即时切换（Electron 保持 frontmatter `line_wrap`）。
