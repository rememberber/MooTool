# DIFF-125：JSON XML/Bean 转换与环境变量删除/导出 toast

对照 Electron `JsonTool.runInputConversion` 与 `VariablesTool` toast。

## F04 JSON

- XML/Bean 输入弹层转换成功：`toastSuccess`（动作为标题）；失败 `toastError`。

## F08 环境变量

- 删除变量、导出快照成功 → `variables.deleted` / `variables.exported` toast。

## 文件

- `JsonScreen.kt`、`VariablesScreen.kt`
