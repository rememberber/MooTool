# DIFF-023：随手记 Markdown 预览用 commonmark AST，不渲染 HTML

- 编号：DIFF-023
- 影响：F01 随手记预览与附件
- 日期：2026-09-09

## 原行为（Electron）

`marked` 把 Markdown 编成 HTML，再用 DOMPurify 消毒后写入预览。图片附件落到文档库根下 `attachments/{yyyyMMddHHmmss}_{8hex}.ext`，正文写 `![image](attachments/...)`。剪贴板含图片时拦截粘贴并串行写入。外部 http(s) 图片会按浏览器策略加载。

## 本产品行为

解析使用 **commonmark-java 0.24.0**（GFM 表格、删除线、任务列表扩展，BSD-2-Clause），输出本产品 AST，由 Compose 绘制标题/列表/表格/任务/代码/链接/本地附件。**不**把正文交给 HTML 引擎或 JEditorPane。`<script>`、onclick 等作为 `Html` 文本节点显示，不执行。`http(s)`/`//` 图片默认不请求，预览只提示未加载；`javascript:`/`data:`/`file:`/`..` 标为不安全。本地图只读 Vault 边界内文件，缺文件可见失败。

附件命名与相对引用对齐 Electron；写入走 `NoteVault.writeBytes`，已存在则换新 id，不覆盖。工具栏「粘贴图片 / 插入图片」写入附件并插入 Markdown。**不拦截** 编辑器 Cmd/Ctrl+V，避免抢走文本粘贴。清理孤立附件先扫描全部笔记引用，仍被引用的文件拒绝删除。文档库列表隐藏 `attachments/`。

## 理由

架构 §5.4 要求结构化 AST 与 Compose 渲染，禁止用旧 HTML 控件冒充完整预览。JVM 没有 Chromium 网络栈，外部图默认不拉网符合「本地文档不无提示发起外部请求」。显式按钮粘贴图片比劫持剪贴板更不容易误伤文本。

## 证据

`MarkdownPreviewEngineTest`：标题/列表/任务/表格/代码；原始 HTML/脚本保留为文本。`NoteAttachmentEngineTest`：插入换行对齐 Electron 样本、唯一相对路径、引用中的附件不可删。`desktopTest` **128/128**。无运行截图。

## 受影响范围

- 预览是 Compose 排版，不是浏览器 CSS；复杂 HTML/内联样式不会按网页呈现。
- 外部图片不会出现在预览中，除非以后增加显式允许策略。
- 拖入编辑器内部、连续 Cmd+V 粘贴图片、列编辑、frontmatter、全文索引、Git、外部冲突仍未做。
- macOS 上 Vault `resolve` 对尚未创建的子目录改为先比较 normalize 路径，再对已存在祖先做 realpath，避免 `/var` 与 `/private/var` 误判逃逸。
