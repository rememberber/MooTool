# DIFF-153：Java `t_func_content` 草稿字段对齐

## 背景

DIFF-152 将 SQLite 草稿写入部分工具会话，但 **Regex 误写入 `pattern`**（Java 版 `RegexForm` 将 `t_func_content` 载入**测试文本** `source`，模式在 `App.config`）。另缺 `TextDiff_left`/`TextDiff_right`、`TimeConvert`、`Calculator`、`QrCode` 等 `FuncConsts` 映射。

## 行为

- `Regex` → `RegexSession.source`
- `TextDiff_left` / `TextDiff_right` → `DiffSession.left` / `right` 并重新比较
- `TimeConvert` → 首行纯数字写入 `timestamp`，否则写入 `localTime`
- `Calculator` → `log` 行列表；末行 `expr = result` 解析表达式与结果
- `QrCode` / `qr` → 首非空行写入 `QrSession.content`

## 证据

- `LegacyToolDraftApplierTest.appliesCodeRunRegexAndJsonDrafts`（断言 `source`）
- `LegacyToolDraftApplierTest.appliesTextDiffTimeAndCalculatorDrafts`

## 仍未覆盖

- Java 计算器「输出区」多行日志与 compose UI 展示方式的视觉一致未验收
- 其余未使用 `t_func_content` 的工具（HTTP、Host 等）仍仅历史/专用存储
