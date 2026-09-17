# DIFF-544：DocumentFormatEngine Markdown 增量格式化

## 背景

DIFF-542/543 已补 JS/TS/Python 表面路径与 Vault Git diff decoration；parity-gap 仍列 Markdown **完整 Prettier**（需 Node）。本条在 JVM 侧补 **Markdown 增量**（对照 Prettier `markdown` 插件常见样本），不重复 DIFF-543 Git diff 装饰，也不引入 Prettier AST。

## 行为

### `CodeEditorSurfaceFormatEngine.formatMarkdown`

- 行尾 trim；围栏代码块（` ``` ` / `~~~`）内仅 trimEnd，保留内部空格。
- 标题 `#`、无序/有序列表、引用 `>` 的 marker 后单空格；段落与列表项正文折叠连续空格。
- 标题后若紧跟非块级段落，插入空行（对齐 `# Title\nparagraph` → `# Title\n\nparagraph`）。
- YAML front matter（首行 `---` 至闭合 `---`）仅 trimEnd，不改写内容。

### 路由

- `DocumentFormatEngine.format(..., "text/markdown"|"markdown"|"md", ...)` 改走 `formatMarkdown`（F01 随手记格式化、F09 HTTP Body `formatBody` 间接受益）。

## Fixture

- `docs/fixtures/electron-next-codeEditorLanguage-vitest.md` 登记 `format-markdown-incremental` 与 Prettier 样本对照说明。

## 验证

- `CodeEditorSurfaceFormatEngineTest`（Markdown 列表/标题/引用/围栏样本）
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装/升级/卸载/公证、JSON/随手记 Vault 外部冲突 **产品主窗** PNG、Markdown **完整 Prettier**（换行 reflow/表格/嵌套块深度）、Vault Git merge 冲突 UI 大改、update/tray 手工验收、目标未达成。
