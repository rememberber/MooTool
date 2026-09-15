# DIFF-030：分栏持久化、Vault 树拖放、风格装饰、线性图标与安装身份

- 编号：DIFF-030
- 影响：F01/F04 文档库、A01 外观/布局、A03 安装身份、P7 打包声明
- 日期：2026-09-14

## 原行为（Electron）

随手记/JSON Vault 树支持把条目拖到目录或根；`layout.paneSizes` 按工具记住分栏。六套 `data-interface-style` 有渐变、inset、选中条等 CSS 装饰，导航为 lucide 线性图标。编辑器走 CodeMirror IME 与矩形选择。安装包用独立 bundleID / UpgradeCode。

## 本产品行为

- Vault 树从扁平列表建成可展开节点；拖到目录或根调用已有 `move()`，禁止拖进自身。按钮移动仍保留。
- `layout.paneSizes` 按工具写入：随手记 Vault/替换、JSON Vault/检查器、Host/HTTP/图片/翻译左栏、代码运行输出高度。拖动手势，双击恢复默认。范围对齐 ui-spec（文件树 200–320，右栏 240–340）。
- 六套风格补齐 raised/toolbar 渐变、inset 输入、smartisan/miui 选中条；按钮与输入在焦点时使用 `focusRing`。线性 `ToolIcon` 替换侧栏/命令搜索字形。
- 列编辑在 JSON 与随手记均启用 Alt 拖选；闩锁按钮可不用 Alt。IME 提交写入列选择并短暂抑制 `keyTyped` 重复。`EditorLimits` 在 ≥5 MiB 时于状态栏提示。RSTA `enableInputMethods(true)`。
- 打包增加 RPM；Windows `perUserInstall`/`shortcut`/`dirChooser`；Linux shortcut/menu/rpmLicense。`InstallIdentity` 固定 UpgradeCode，卸载不得触碰其他产品路径。Windows/Linux/arm64/公证/升级卸载仍未测。

## 理由

规格要求面板按工具保存、Vault 拖放真实执行、风格开关不能只改色板、导航必须是线性图标。安装身份必须在代码中完整，不能把未测平台标成已支持。

## 证据

`VaultMoveTest`、`LayoutPaneSizesTest`、`VaultTreeTest`、`ThemeContrastTest`、`EditorBufferLargeDocumentTest`、`InstallIdentityTest`。`desktopTest` **167/167**，见 `docs/evidence/2026-09-14-layout-theme/`。窗口截图、IME 手工、三平台安装未测。

## 受影响范围

- 分栏宽度写入本产品 `settings.json`，不读取 Electron schema。
- 对比度测试覆盖 6 风格 × 明暗的正文/侧栏 4.5:1 与焦点环 3:1；不是窗口截图验收。
- RPM 仅进入 `targetFormats`；须在 Linux 上执行 `packageRpm` 才能声称可用。
