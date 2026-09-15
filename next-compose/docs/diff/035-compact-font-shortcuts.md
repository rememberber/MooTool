# DIFF-035：JSON 工具栏字体、960 按需辅助栏与快捷键冲突可见提示

- 编号：DIFF-035
- 影响：F04 JSON 工具栏/窄窗、F01 随手记工具栏/窄窗、A01 shortcuts/editor 字体
- 日期：2026-09-15

## 原行为（Electron）

`JsonToolbar.tsx` 在压缩后有 `FontSelect`，写入 `json.font`。960–1079 内容宽导航折叠，文件树/检查器按需切换，编辑区占满。随手记工具栏有字体、字号、语法。设置里搜索/设置快捷键冲突时拒绝覆盖并给出原因。

## 本产品行为

- JSON 工具栏增加系统字体选择，写入 `settings.editor.jsonFontName`；设置页同一 `FontSelect`，列表为 Electron 回退字体 + `GraphicsEnvironment` 已安装字体。
- 内容宽 < 960 时 JSON 默认隐藏 Vault 与检查器；工具栏「JSON Vault」「更多工具」互斥打开其一。检查器关闭会清掉紧凑态。宽窗仍同时显示 Vault，检查器由 `inspectorOpen` 控制。
- 随手记同样按需切换文档库/快速替换；工具栏可改当前笔记 `fontName`/`fontSize`/`syntax`，并立即作用于 `EditorHost`。
- 搜索与设置快捷键冲突时字段旁显示错误，绑定不写入设置。

## 理由

规格点名 JSON 工具栏「格式化、压缩、字体、换行」；ui-spec 960 档要求文件树/检查器按需切换而不是挤掉编辑器；快捷键冲突必须可见，不能静默丢输入。

## 证据

`SystemFontsTest`、`ShortcutBindingsTest`、`LayoutPolicyTest` 紧凑辅助栏、`CompactShellCaptureTest`（Compose 场景 PNG）、`CompactWindowPaintTest`（`JFrame.paint`，**不是**用户窗口手势截图）。`desktopTest` **193/193**，见 `docs/evidence/2026-09-15-compact-font-shortcuts/`。IME 手工、三平台安装、逐选择器 CSS 仍未测。

## 受影响范围

- 紧凑宽度下必须点工具栏才能看到 Vault/检查器/快速替换；一次只显示一个辅助栏。
- 随手记字体写入文档 metadata，保存后进入 frontmatter；未保存的字体只在当前会话。
- `JFrame.paint` 与 Compose 场景 PNG 不能代替真实窗口截图验收。
