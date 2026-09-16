# DIFF-285：JSONPath 弹层双击执行查询（已由 DIFF-319 收回弹层部分）

## 对照 Electron / 检查器内联树

检查器内联路径树：**单击**预览、**双击**执行 `queryJsonPath` 并写入结果区（见 [DIFF-229](229-json-path-tree-row-shared.md)）。

## 行为（历史）

- 曾让 `PathPickerDialog` 双击自动查询以对齐内联树。
- [DIFF-319](319-json-path-picker-choose-only.md) 将弹层「使用」/ 双击改回与 Electron `onChoosePath` 一致；内联树双击查询保留。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
