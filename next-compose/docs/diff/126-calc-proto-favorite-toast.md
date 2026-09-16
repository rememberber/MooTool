# DIFF-126：计算器/Protobuf/收藏/图片删除 toast

对照 Electron 运算成功反馈与 `FavoriteDialog` / `ImageTool` 收藏删除 toast。

## 范围

- **F21 计算器**：`runCalc` 成功/失败 toast。
- **F07 Protobuf**：`runOp` 成功/失败 toast。
- **F15/F16/F22**：正则/Cron/调色板收藏保存与删除 toast。
- **F24 图片**：批量删除资源 toast（`favorite.deleted`，与 Electron 图片库一致）。

## 文件

- `CalculatorScreen.kt`、`ProtobufScreen.kt`、`RegexScreen.kt`、`CronScreen.kt`、`ColorBoardScreen.kt`、`ImageScreen.kt`
