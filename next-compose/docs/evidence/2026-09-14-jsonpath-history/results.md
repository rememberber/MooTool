# 2026-09-14 JSONPath / 历史搜索 / 快捷键 / 安装身份

- 已执行：`./gradlew :composeApp:desktopTest`（Zulu 21，`--offline`）
- 结果：`desktopTest` **169/169** 通过，0 skipped
- 相对上一刀（167）：+2（JSONPath filter/slice/union/转义、JFrame 列拖选+IME 提交）

## 本轮落地

- JSONPath：filter / slice / union / `$..` / `$['foo.bar']`；空匹配返回 `[]`；非法路径 `JsonException`；不执行任意 JS
- 检查器路径树单击预览、双击查询；路径弹层双击采用
- 命令搜索点击打开
- `HistoryBrowser`：搜索/详情/恢复/删除/清空（JSON、HTTP、编码、正则、Cron）
- Regex/Cron 收藏分组与查询；调色板收藏搜索
- HTTP 当前响应 Tab 真实另存文件
- 设置快捷键帮助；About 展示 UpgradeCode / 包名 / 卸载隔离路径
- 主窗口最小 960×640（紧凑折叠已有）
- `JFrame` 上列选择拖选 + IME 提交写入两行；非手工窗口验收

## 未测

- 窗口截图：960×640、检查器、命令搜索、历史对话框
- 列编辑 / IME 手工窗口手势（本轮是派发 MouseEvent/InputMethodEvent，不是用户拖动手势）
- HTTP 真实联网另存、三平台安装/升级/卸载/公证
- Electron 六套 CSS 逐选择器皮肤
