# DIFF-542：Vault Git diff EditorHost + 编辑器格式化 JS/Python + 冲突 Overlay 截图

## 背景

DIFF-541「未做」仍列 Vault Git Diff CodeMirror/EditorHost 级并排编辑器、JS/TS/Markdown Prettier 级格式化、Vault 冲突 Overlay Compose 测试/截图缺口。

## 行为

### Vault Git diff · EditorHost

- `GitDiffPresentation.rstaSyntaxForFile`：路径语言 → RSTA 语法键。
- `GitDiffSideBySideEditors` / `GitDiffEditorHost.kt`：只读双 `EditorHost`、diff 段背景高亮（`EditorBuffer.markDiffSide`）、垂直滚动同步。
- `VaultGitDialog` diff 区由 Compose `Text` 改为上述 EditorHost 链（对齐 Electron `VaultGitDiffView` + `TextCodeEditor`）。

### DocumentFormatEngine · JS/TS/Python/Markdown

- `CodeEditorSurfaceFormatEngine`：Python tab 展开 + 行尾 trim（对齐 Electron `formatPython`）；plain/markdown 行尾 trim；JS/TS **表面间距**（非 Prettier AST，对照 `codeEditorFormatting.test.ts` 单线样本）。
- `DocumentFormatEngine.format` 路由上述引擎；F09 `HttpEngine.formatBody`、随手记格式化间接受益。

### Vault 冲突 Overlay 截图

- `JsonVaultConflictOverlay` 可选按钮 `Modifier`（测试注入焦点）。
- `VaultConflictOverlayCaptureTest`：`147-compose-json-vault-conflict-overlay-keep-tab-focus.png`（Overlay 完整链，非仅 `VaultConflictDialog`）。

## Fixture

- `docs/fixtures/electron-next-codeEditorLanguage-vitest.md` 登记 Git diff EditorHost 语法链。
- `docs/fixtures/electron-next-httpTools-vitest.md` 登记 JS `formatBody` 样本。

## 验证

- `GitDiffPresentationTest` / `CodeEditorSurfaceFormatEngineTest` / `HttpEngineTest`（JS formatBody）
- `VaultConflictOverlayCaptureTest` / `VaultConflictOverlayInteractionTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、JSON/随手记 Vault 外部冲突 **产品主窗** PNG、JS/TS/Markdown **完整 Prettier** 语义、Vault Git diff 字符级 decoration 与 Electron 像素级一致、update/tray 手工验收、目标未达成。
