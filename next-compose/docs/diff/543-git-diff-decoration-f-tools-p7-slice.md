# DIFF-543：Vault Git diff 行/字符装饰 + F21/F07 历史单测 + 冲突证据/P7 登记

## 背景

DIFF-542「未做」仍列 Vault Git diff 字符级 decoration 与 F02 不一致、完整 Prettier、产品主窗冲突 PNG、F 工具历史缺单测、P7 三平台、六套 CSS。

## 行为

### Vault Git diff · decoration parity

- `GitDiffDecoration`：与 F02 `both` 一致，行 16% + 字符 42% 叠层；insert/delete 侧过滤与 `EditorBuffer.markDiffSide` 共用。
- `GitDiffPresentation.diffHighlightMode` → `GitDiffSideBySideEditors` 只读双 `EditorHost`。
- `DiffHighlight.annotateSide` 改走同一区间算法（Compose 文本对比与 Vault Git RSTA 对齐）。

### 编辑器格式化（增量表面路径）

- `CodeEditorSurfaceFormatEngine.formatTypescript` 单测锁定（仍非 Prettier AST；Markdown 完整 Prettier 未引入 Node）。

### F 工具历史

- `CalculatorHistoryRestoreTest`（F21 表达式/结果）。
- `ProtobufHistoryRestoreTest`（F07 JSON→Binary + metadata）。

### 产品证据 / P7

- `docs/evidence/2026-09-17-vault-conflict-product-window/results.md` 登记 Compose 帧 `147`（Overlay 链，非产品主窗 §A PNG）。
- `prepare-vault-conflict-evidence.sh` / `scripts/README-product-evidence.md` 交叉引用 `147` 与 §A 手工截图分工。
- P7：`prepare-p7-package-smoke.sh` 注释说明 DIFF-543 后仍仅本机门禁，不代替三平台安装/公证。

## Fixture

- `docs/fixtures/electron-next-codeEditorLanguage-vitest.md` 补充 TS 表面格式化样本行。

## 验证

- `GitDiffDecorationTest` / `GitDiffPresentationTest` / `CalculatorHistoryRestoreTest` / `ProtobufHistoryRestoreTest` / `CodeEditorSurfaceFormatEngineTest`
- `VaultConflictOverlayCaptureTest`（`147`，回归）
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装/升级/卸载/公证、JSON/随手记 Vault 外部冲突 **产品主窗** PNG、JS/TS/Markdown **完整 Prettier** 语义、update/tray 手工验收、目标未达成。
