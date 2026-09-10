# DIFF-021：随手记快速替换与本产品文档库

- 编号：DIFF-021
- 影响：F01 随手记
- 日期：2026-09-09

## 原行为（Electron）

文档库为用户可选目录；元数据可写 frontmatter。快速替换 24 项在 `quickReplace.ts`：行处理前 CRLF 归一；`localeCompare` 默认 locale 排序；`Number(...).toExponential()` 转科学计数；`JSON.stringify`/`JSON.parse` 做转义。选区优先，否则全文，一次操作为一次撤销。

## 本产品行为

默认库为 `data/vaults/quick-note`，设置可改绝对路径，路径 `normalize` 后禁止逃出库根。快速替换对齐上述 24 个 ID 与 Electron 测试样本；排序用 `Collator(Locale.ROOT)`；科学计数用手写 mantissa/exponent，不以 Java `String.format("%e")` 冒充 JS。转义用手写 JSON 字符串规则 + Jackson 反解析。选区走 `EditorBuffer.replaceRange`。

本轮不做 Git、Markdown 预览/分栏、列编辑、附件、frontmatter 隐藏、全文索引或外部冲突监视；这些入口没有做成空按钮。

## 理由

产品线独立要求自有 Vault。JVM 没有 JS `localeCompare`/`toExponential` 的默认实现，必须固定可测规则。未完成能力按验收规则保持未实现，不能用预览壳冒充。

## 证据

`QuickReplaceEngineTest`：与 Electron 相同的空行/去重计数/排序、`hello_world`/`1.25e3`/`1,234,567.5`、单引号列表与 escape 往返。`StorageIsolationTest.noteVaultRejectsPathEscapeAndCreatesMarkdown`。`desktopTest` **120/120**。

## 受影响范围

- 中文/emoji 行排序可能与 Chromium `localeCompare` 不完全相同。
- `normalToScientific` 对极大/极小浮点的位数可能与 V8 `toExponential()` 有末位差异；当前 fixture 为 `1234567.5` → `1.2345675e+6`。
- 切换笔记时若当前文件有未保存更改会先写入，失败则可见；无 mtime 冲突检测。
- 正则 worker 单测超时从 800ms 调整为 3s，避免 classpath 变大后冷启动误杀；生产默认超时未改。
