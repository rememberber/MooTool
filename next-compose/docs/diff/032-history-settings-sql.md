# DIFF-032：全工具历史浏览器、设置生效字段与 SQL 格式化

- 编号：DIFF-032
- 影响：A02 历史、F17/F23/F24 历史入口、A01 外观/编辑器/Vault、F01 随手记格式化、拆出窗口主题
- 日期：2026-09-14

## 原行为（Electron）

通用历史支持搜索/详情/恢复/删除/清空。图片与 PDF 有历史入口。二维码 History Tab 可搜可删。设置含统一工作区背景、SQL 方言、JSON/随手记字体字号、Vault 树展开。随手记 Cmd/Ctrl+Shift+F 按当前语法格式化，SQL 跟随方言。拆出窗口使用当前外观风格。

## 本产品行为

- 所有声明通用历史的工具都接到 `HistoryBrowser`（搜索/恢复/单条删除/清空），含 UA、Diff、格式化、配置、Protobuf、加解密、时间、计算器、网络、Host、代码运行、随手记、调色板。
- 图片、PDF 增加历史按钮与恢复（摘要与输出路径，不重放处理）。二维码 History Tab 增加搜索与单条删除。
- 设置新增并真正生效：统一工作区背景、界面字体、SQL 方言、JSON/随手记字体与字号、JSON Vault 树展开模式。关闭统一背景后 modern 工作区使用略偏色的 workspace，对照度仍 ≥ 4.5:1。
- 拆出窗口与主窗共用当前风格、强调色、统一背景和界面字体，不再回落到默认 modern。
- 随手记增加格式化按钮与 Cmd/Ctrl+Shift+F；按 frontmatter 语法走 JSON/Java/XML/HTML/YAML/SQL 或去尾空白。SQL 标识符引号随方言：MySQL `` ` ``、PostgreSQL `" "`、Transact-SQL `[]`。打开文件时写入对应 RSTA 语法。
- SQL 默认方言改为与 Electron 一致的 `Standard SQL`。已有设置为 `mysql` 的配置仍映射到 MySQL 按钮。

## 理由

规格要求历史能搜能删，不能只清列表；设置字段必须改变运行结果；拆出窗口不能丢失主题。SQL 不用新 Maven 依赖，用自有分词器实现方言可见差异，避免 `--offline` 无法解析新构件。

## 证据

`SqlFormatEngineTest`、`DocumentFormatEngineTest`、`ThemeContrastTest.unifiedBackgroundChangesModernWorkspace`。`desktopTest` **175/175**，见 `docs/evidence/2026-09-14-history-settings/`。窗口截图、IME 手工、三平台安装未测。

## 受影响范围

- 关闭「统一工作区背景」后 modern/quiet/miui-v5 工作区颜色与面板色分离。
- 随手记格式化改写当前缓冲，记一次撤销；非法 JSON/Java/XML 会显示真实错误并保留原文。
- 历史删除按 `id` 删本产品 SQLite 行。
