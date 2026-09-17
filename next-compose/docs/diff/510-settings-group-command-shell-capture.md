# DIFF-510：设置分组 `--surface`、命令盘搜索行帧、modern 压平壳走查

## 背景

DIFF-509「未做」仍列：设置分类与 Electron 逐组 UI 差、命令盘 Compose 新帧、非 p5 工具页 modern 压平走查、F-tool 引擎大切片、P7 本机 `MOOTOOL_P7_BUILD_DIST=1` 执行。本条优先 **A01 设置分组行块**（`settings-group__rows` 底与边框）+ **A02 命令盘** 搜索行 Tab 序证据帧 + **P1** 非 p5 `mooToolShell` 压平 Compose 帧；不展开 Vault/Git 或 P7 安装验收。

## 行为

### A01 设置分组

- `MooColors.settingsGroupRowsFill()` / `settingsGroupRowsBorder()`：分组行块背景对齐 Electron `--surface`（hero/claude/miui/smartisan 不再误用 `surfaceCard`）；边框 hero→`borderSoft`、smartisan→`borderControl`。
- `SettingsGroup`：行块圆角/阴影跟 `MooDimens.shellRadius` / `shellElevation`；分组标题 smartisan `650`、claude `letterSpacing 0.15.sp`。

### A02 命令盘

- `ToolbarFocusCaptureTest.captureCommandPaletteSearchRowTabChrome` → `151-compose-command-palette-search-row-tab.png`（搜索框 + 关闭钮同行，关闭钮焦点环；与 `149`/`140` 互补）。

### P1 非 p5 工具壳

- `ModernToolShellCaptureTest.captureModernFlattenedEncodeStylePanels` → `150-compose-modern-flatten-tool-shell.png`（modern 默认压平双栏，无 card 阴影壳）。

## 验证

- `SettingsGroupThemeTest`
- `./gradlew :composeApp:verifyNativePackageMetadata :composeApp:desktopTest --offline`（JDK 21）

## 未做

六套 CSS 逐选择器皮肤、产品窗全工具 Tab 走查、命令盘每条结果 Tab 遍历、其余 F-tool 引擎大切片、P7 三平台真实安装/公证、`MOOTOOL_P7_BUILD_DIST=1` 本机会话（若未执行则见 acceptance 说明）、设置逐组行内控件与 Electron 像素级差。
