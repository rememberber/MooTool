# DIFF-033：折叠导航 tooltip、检查器关闭、主题 token、编辑器语法色与安装声明

- 编号：DIFF-033
- 影响：P1 壳/导航、A01 外观、F04 JSON 检查器、F01/F04 编辑器、A02 历史隐私、A03/P7 安装声明
- 日期：2026-09-14

## 原行为（Electron）

折叠侧栏悬停显示完整工具名。JSON 检查器顶栏有关闭。六套 `data-interface-style` 使用 CSS 变量（hero `--sidebar: #fafafa` / `--workspace: #f4f4f5` 等）。编辑器语法色走 `--syntax-*`。历史不得默认存私钥/密码/认证 Header。安装包带 Developer Tools 类别与独立产物名。

## 本产品行为

- 折叠导航、收起/展开按钮与折叠态设置按钮使用 `MooTooltip`（`Popup`，避免被 84dp 侧栏裁切）。
- JSON 检查器顶部增加标题与关闭，关闭后 `inspectorOpen = false`。
- hero / smartisan / miui-v5 / claude 的 sidebar、workspace、toolbar、文字与选中条对齐 Electron CSS 变量；仍是 Compose token，不是逐选择器 CSS 移植。对比度单测仍要求正文/侧栏 ≥ 4.5:1。
- `EditorHost`/`EditorBuffer.applyTheme` 使用当前风格 workspace 与 Electron `--syntax-*` 对应的 RSTA `SyntaxScheme`。
- 拆出窗口抽 `DetachedToolWindow`：标题「工具名 · MooTool Next Compose」、跟随主题、`minimumSize = 960×640`。
- 列粘贴对短行按视觉列补空格；单行剪贴板写入整列选区。IME `committedCharacterCount = 0` 的预编辑在列选区中被 consume，不写入文档。
- 加解密历史遮蔽私钥操作输入与生成密码输出；HTTP 历史遮蔽 URL 中的 basic-auth 密码，仍不存 Header。
- macOS 打包声明 `public.app-category.developer-tools`、`minimumSystemVersion = 12.0`、高分屏 Info.plist。`scripts/rename-dist-artifacts.sh` 把 jpackage 产物复制为规格文件名。签名/公证/三平台安装仍未测。

## 理由

ui-spec 要求折叠态完整名称 tooltip；检查器必须能关；风格开关不能只改一个 accent；编辑器不能写死黑白；历史规格禁止默认存私钥；安装身份必须在代码里完整但不能把未测平台标成已支持。

## 证据

`ThemeContrastTest`（含 hero/miui/claude token 与色板 PNG）、`EditorThemeTest`、`HistoryPrivacyTest`、`ColumnEditEngineTest` 短行粘贴、`EditorBufferColumnEditTest` IME 预编辑不落盘、`InstallIdentityTest` 产物名与 macOS 类别。`desktopTest` **184/184**，见 `docs/evidence/2026-09-14-shell-theme/`。色板 PNG 不是窗口截图；IME/列编辑手工手势与三平台安装未测。

## 受影响范围

- 统一背景开启时 hero/smartisan/claude 工作区仍用 inset。
- 列模式 IME 预编辑不会出现在文档里，提交后才写入各列。
- 已有加解密/HTTP 历史行不会回写遮蔽；仅新保存生效。
- jpackage 默认 DMG/MSI 文件名仍带空格；规格名需运行 rename 脚本，且本机未执行打包。
