# DIFF-040：流式输出跟尾、HTTP 上次响应与 1080 工具栏溢出

- 编号：DIFF-040
- 影响：F05 代码运行、F11 网络、F09 HTTP、F04 JSON 工具栏、F01 随手记工具栏、ui-spec L3/L4 与 1080–1439 密度
- 日期：2026-09-15

## 原行为（Electron）

运行台输出区可滚动；规格要求输出增长时默认跟随尾部，用户上翻后暂停。HTTP 失败或取消须保留请求与上次结果，并标明「上次响应」。1080–1439 宽度优先把工具栏低频动作收入「更多」，保留主要动作。Electron JSON「更多」本身是检查器开关，不是溢出菜单。

## 本产品行为

- `FollowTail`：距底部 ≤ 48px 视为钉住尾部；新一次运行/命令会重新钉住，结束或清空不会把已上翻的视图拽回底部。
- 代码运行与网络工具流式输出使用同一规则。
- HTTP 传输失败（取消/超时/网络/非法请求）继续展示上次可用响应，标题为「上次响应」；4xx/超限等仍是当前结果。
- 内容宽 < 1440 时 JSON 把换行、列编辑、导入导出、历史、Git、检查器收入「更多」；≥ 1440 仍用「更多工具」开关检查器。
- 随手记在同样宽度把字号、行距、列编辑、图片、导入导出、历史、Git 收入「更多」。

## 理由

目标要求对照 Electron 补齐布局/功能缺口。跟尾与「上次响应」是规格明文；Compose 用文字按钮，1080 档必须溢出，不能只靠横向滚动冒充已对齐。

## 证据

`FollowTailTest`、`LayoutPolicyTest.overflowToolbarBelowFullWidth`、`HttpEngineTest.failAndCancelKeepPreviousResponseLabel`。`desktopTest` **210/210**，见 `docs/evidence/2026-09-15-follow-tail-overflow/`。窗口手势、IME 手工、三平台安装仍未测。

## 受影响范围

- 跟尾阈值是 Compose 实现，不是逐像素移植 Electron `overflow: auto`。
- 仍非 Electron 六套 CSS 逐选择器移植。
