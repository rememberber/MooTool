# DIFF-534：F08 导出分区 + previewDiff + Vault Git push 对齐

## 背景

DIFF-533 未做项含 F08/F25 与 Vault Git UI。F08 `formatExport` 分区标题应对齐 Electron `formatEnvironment` 且可单测锁定；编辑对话框 `previewDiff` 缺回归。Vault Git **push** 按钮曾比 Electron 多禁用「仅有 conflicts、非 merging」场景，与 `VaultGitDialog.tsx` 不一致（引擎仍由 `GitPushGuardTest` 拒绝）。

## 行为

### F08 环境变量

- `EnvExportSections`：导出四段标题常量；`EnvEngine.formatExport` 复用。
- `previewDiff` 单测：user/system 路径、更新与删除摘要。

### F25 系统信息

- `HardwareSessionRestoreTest`：Tab / 序列号显示开关恢复并清空 stale snapshot。

### Vault Git UI

- `GitOperationPresentation.pushEnabled`：remote 且非 merging（对齐 Electron push 禁用条件）。
- `VaultGitDialog` push 按钮改用上述 helper。

### Fixture

- `docs/fixtures/electron-next-envTools-vitest.md` 登记 F08 导出/preview/文件更新链。

## 验证

- `EnvEngineTest` / `HardwareSessionRestoreTest` / `GitOperationPresentationTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21，**841/841** 通过，2 skipped 为默认跳过的公网 GET/multipart smoke，合计 843 tests）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、F08/F25 通用历史（仍无）、Vault Git 面板大改、目标未达成。
