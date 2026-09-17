# DIFF-540：Vault Git 布局/图标 + Vault 冲突 UI 流 + F03 导出目录 + 命令盘更新/导航

## 背景

DIFF-539「未做」仍列 Vault Git 面板相对 Electron `VaultGitDialog.tsx` 的 **布局/线性图标** substantial 差；JSON/随手记外部冲突 UI 流分散在 Screen 内、删除态说明未集中；F03 格式化文件 Tab 未对齐 `tools.exportDirectory` 初始目录链；命令盘 **更新/导航** 关键词仍可加强。

## 行为

### Vault Git UI

- `GitPanelIcon` / `GitPanelIconKind`：Lucide 对齐分支/刷新/fetch/push/merge/discard/ours·theirs/commit 图标。
- `VaultGitDialog`：`git-panel__status` 式顶栏（分支图标 + 状态列 + 工具栏底）、操作钮 `MooButton.leading` 图标（对齐 Electron 按钮内 lucide）。
- `MooButton` 可选 `leading` 槽（Vault Git 首用）。

### JSON/随手记 Vault 外部冲突

- `VaultConflictPresentation`：`showReloadAction` / `hintMessageKey` / `previewText`；删除态 `vault.conflict.hintDeleted`。
- `VaultConflictDialog` 改用 presentation；`JsonVaultConflictOverlay` / `QuickNoteVaultConflictOverlay` 统一 Screen 回调链（reload/saveCopy/keep + refresh）。
- `VaultConflictPresentationTest` / `VaultConflictOverlayFlowTest` / 删除态 UI 无 reload 钮。

### F03 格式化 · 工具默认导出目录

- 文件 Tab 导入/另存走 `chooseFileWithExportDirectory` + `persistToolsExportDirectory`（对齐 QR/PDF/图片链）。

### 命令盘 · 更新/导航

- `layout` 增 `custom`/`hidden`/`group`/`分组`/`隐藏`；`about` 增 `upgrade`/`download`/`release`/`升级`/`版本`。

## Fixture

- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 Git 面板图标/顶栏（补充 presentation 行）。

## 验证

- `VaultConflictPresentationTest` / `VaultConflictOverlayFlowTest` / `VaultConflictDialogInteractionTest` / `CommandSearchCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**882/882** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 884 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、JSON/随手记 Vault 外部冲突 **产品主窗** PNG、Vault Git Diff 区 CodeMirror 级体验、update/tray 手工验收、目标未达成。
