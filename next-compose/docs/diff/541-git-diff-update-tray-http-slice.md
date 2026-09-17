# DIFF-541：Vault Git diff presentation + 关于/更新/托盘 + F09 正文格式化 + 冲突 Overlay 交互

## 背景

DIFF-540「未做」仍列 Vault Git Diff 区 CodeMirror 级体验、update/tray substantial 代码、F-tool 引擎差、导航/关于 UI、冲突 Overlay Compose 测试缺口。

## 行为

### Vault Git diff

- `TextCodeEditorLanguages` / `TextCodeEditorLanguage`：对齐 Electron `resolveTextCodeEditorLanguage`（含 Git 路径扩展名 hint）。
- `GitDiffPresentation`：diff 预览文案键、多文件选择、并排区显隐、`languageForFile`；`VaultGitDialog` diff  pane 改用上述 helper。

### 关于/更新 · 托盘

- `UpdateAboutPresentation`：关于页状态文案键与下载/取消/打开安装包/发行说明显隐；`SettingsScreen` about 区集中调用。
- `TraySyncPresentation`：`shouldInstallTray` + `menuRevision`；`Main.kt` 托盘 `LaunchedEffect` 与 hide-to-tray 对齐 Electron `updateTray` 重建语义。

### F09 HTTP

- `HttpEngine.formatBody` 改走 `DocumentFormatEngine.format`（对齐 Electron `formatCodeEditorContent` 对 JSON/XML/HTML/SQL 等 MIME 链；`HttpScreen` 传入 `sqlDialect`）。

### 命令盘 · 布局/关于

- `layout` 增 `classic`/`separators`/「紧凑」「经典」；`about` 增 `changelog`/`notes`/`check`/「检查更新」「发行说明」。

### Vault 冲突 Overlay

- `VaultConflictOverlayInteractionTest`：`JsonVaultConflictOverlay` keep 钮 → `onRefresh` 回调。

## Fixture

- `docs/fixtures/electron-next-codeEditorLanguage-vitest.md` 登记 MIME/短名映射（补充 Git diff 路径扩展名）。
- `docs/fixtures/electron-next-httpTools-vitest.md` 登记 `formatBody` → `DocumentFormatEngine` 链。

## 验证

- `TextCodeEditorLanguageTest` / `GitDiffPresentationTest` / `UpdateAboutPresentationTest` / `TraySyncPresentationTest`
- `VaultConflictOverlayInteractionTest` / `CommandSearchCatalogTest` / `HttpEngineTest`（formatBody）
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、JSON/随手记 Vault 外部冲突 **产品主窗** PNG、Vault Git Diff CodeMirror/EditorHost 级并排编辑器、update/tray 手工验收、JS/TS/Markdown Prettier 级格式化、目标未达成。
