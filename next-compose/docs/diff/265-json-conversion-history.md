# DIFF-265：JSON XML/Bean 转换写入通用历史

## 对照 Electron

`JsonTool.runInputConversion` 在成功后将 `conversionInput` → 输出 JSON 写入 `window.mootool.saveHistory`（`funcType: json`），与工具栏格式化等操作一致。

## 行为

- XML/Bean 转换弹层成功执行后调用 `container.history.save(ToolId.Json, …)`，摘要为对应 action 标题，输入为 `conversionInput`，输出为转换结果。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
