# DIFF-539：Vault Git 面板 presentation 补全 + pull 对齐 Electron

## 背景

DIFF-538「未做」仍列 Vault Git UI 相对 Electron `VaultGitDialog.tsx` 的 substantial 缺口。538 已集中 init/discard/abort；parity-gap 仍缺 continue/`git.counts` 显隐、冲突 ours/theirs 与刷新/中止确认 busy 语义集中到 `GitOperationPresentation`。DIFF-520 曾让 pull 在 `conflicts > 0` 时 UI 禁用，与 Electron（仅 `remote && !merging`）及 `GitEngine.pull`（不单独拒绝 conflicts）不一致。

## 行为

### Vault Git UI

- `GitOperationPresentation`：`showContinueAction`、`showChangeCounts`、`resolveConflictEnabled`、`refreshEnabled`、`abortConfirmEnabled`；`VaultGitDialog` 继续按钮、`git.counts`、ours/theirs、刷新与中止确认改用上述 helper。
- `pullEnabled` 改为 `remote && !merging`（对齐 Electron pull 禁用条件；未解决冲突时仍由 `git pull` 结果/toast 反馈）。

## Fixture

- `docs/fixtures/electron-next-vaultGitService-vitest.md` 登记 pull UI 对齐（更新 `git-ui-abort-copy` 说明）。

## 验证

- `GitOperationPresentationTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**874/874** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 876 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、JSON/随手记 Vault 外部冲突产品窗、F-tool 引擎大切片、update/tray 手工验收、导航/设置 substantial UI 差、Vault Git 面板布局/图标 substantial 差、目标未达成。
