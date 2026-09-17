# DIFF-545：JS/TS 增量格式化 + Vault Git merge 冲突 UI + 设置/导航

## 背景

DIFF-544 已补 Markdown 增量；parity-gap 仍列 JS/TS **完整 Prettier**（需 Node）、Vault Git merge 冲突 UI 说明、六套 CSS 与设置/导航关键词。本条不重复 Markdown，在 JVM 侧加强 JS/TS 增量（字符串/注释/import/箭头/多行块），并集中 merge 冲突提示与 git-panel 冲突徽标样式。

## 行为

### `CodeEditorSurfaceFormatEngine` · JS/TS 增量

- 行内按字面量/行注释/块注释分片，避免改写 `"a=b"`、`// x=y`。
- `import`/`export` 整行格式化（含 `from "module"`），对齐 Electron 单线样本。
- `=>`、赋值、对象字面量表面间距；`type X = { ... }` 补语句分号；`interface`/`function`/`{`/`}` 结构行不追加错误分号。
- `DocumentFormatEngine` / F09 `HttpEngine.formatBody` / F05 编辑器链仍走同一路径（非 Prettier AST）。

### Vault Git merge 冲突 UI

- `GitMergeConflictPresentation`：`unresolvedHintKey`（merge/rebase 进行中且 `conflicts > 0`）、`showConflictBadge`。
- `VaultGitDialog` 状态区追加说明文案；变更行「冲突」标签 **Italic**（对齐 Electron `<em>`）。

### 设置 / 导航

- 命令盘 `editor` 分类增 `javascript`/`typescript`/`format`/`格式化` 关键词。

## Fixture

- `docs/fixtures/electron-next-codeEditorLanguage-vitest.md` 登记 `format-js-ts-incremental`。
- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 merge 冲突提示行。

## 验证

- `CodeEditorSurfaceFormatEngineTest` / `GitMergeConflictPresentationTest` / `CommandSearchCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装/升级/卸载/公证、JSON/随手记 Vault 外部冲突 **产品主窗** PNG、JS/TS/Markdown **完整 Prettier**、translate/http/pdf 引擎大改、update/tray 手工验收、目标未达成。
