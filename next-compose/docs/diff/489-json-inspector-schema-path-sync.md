# DIFF-489：JSON 检查器 Schema / 路径复制 / 路径树同步

## 背景

DIFF-488 补齐结构面板后，检查器仍缺：

- 从有效 JSON **推断 Draft-07 JSON Schema**（结构区动作，结果走既有结果弹层）；
- **复制 JSONPath**（输入框旁按钮、重复键路径可点复制，对齐 macOS `JSONTreePane` 复制路径）；
- **路径树与 `jsonPath` 同步**：Tauri `path.picker` 快捷下拉 + 路径弹层再次打开时跟当前 `jsonPath`（修复旧 `pathPickerSelection` 残留）。

## 行为

- `JsonEngine.inferJsonSchema`：对象/数组/标量推断 `type`、`properties`、`required`、`items`。
- 结构卡片「生成 JSON Schema」仅在解析成功时可用。
- JSONPath 区：`json.path.picker` 快捷菜单（与内联树同源、80 条截断）、`json.path.copy` 复制当前路径。
- `JsonPathPickerDialog` 打开时 `jsonPathPickerSelectionForOpen` 始终对齐 `jsonPath`。
- 重复键列表单击复制该 JSONPath。

## 验证

- `JsonEngineTest.inferJsonSchema_emitsDraft7ObjectWithRequiredKeys`
- `JsonInspectorPathUiTest`、`JsonPathPickerSelectionTest`
- `./gradlew :composeApp:desktopTest --offline`

## 未做

产品窗手工验收、Electron 无 Schema 按钮（对照 Tauri/原生树复制路径能力）。
