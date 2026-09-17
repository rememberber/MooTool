# DIFF-531：工具默认值即时同步 + Vault Git 命令盘关键词 + F-tool fixture 登记

## 背景

DIFF-530 已补编辑器 SQL 方言与 JSON 系字号即时生效；parity-gap 仍列 **工具默认值**（QR 尺寸/纠错、随机串长度）在设置页失焦提交后，F17/F14 会话仍停留在 `remember` 初值。命令盘 Vault Git 工作流（merge/rebase/push）关键词仍可加强；F18/F25 引擎单测缺 Electron fixture 登记。

## 行为

### A01 / F17 / F14 工具默认值 · 即时生效

- `ToolsSettingsLiveApply`：`qrCodeSize` / `qrErrorCorrection` / `randomStringLength` 边界与 Electron `normalizeSettings` 一致。
- `QrCodeScreen` / `CryptoScreen`：`LaunchedEffect(settings.tools.*)` 在设置变更后同步会话字段（对齐 JSON `softWrap` 链，见 DIFF-528）。
- F20 翻译源/目标/引擎已每帧读 `settings.tools.*`，本切片不重复。

### A02 命令盘 · Vault Git / 工具默认值

- `CommandSearchCatalog`：`vault` 增 `merge`/`rebase`/`push`/`pull`/`冲突`；`tools` 增 `qr`/`random`/`correction`/`二维码`/`随机`。

### F18 / F25 fixture

- `docs/fixtures/electron-next-timeTools-vitest.md`、`electron-next-hardwareTools-vitest.md` 登记 Compose `TimeEngineTest` / `HardwareEngineTest` 对照项。

## 验证

- `ToolsSettingsLiveApplyTest` / `CommandSearchCatalogTest`
- `./scripts/prepare-tray-screencapture-evidence.sh`（恢复 `57-color-baseline.png`）
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 皮肤、产品主窗 Tab 走查、TCC 系统对话框 PNG 手工、P7 三平台安装、multipart 文件 Tab、Host 软换行、随手记已开笔记随全局 softWrap 切换、编辑器 sql/字号链（DIFF-530）、目标未达成。
