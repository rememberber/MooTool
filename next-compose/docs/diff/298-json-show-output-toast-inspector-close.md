# DIFF-298：JSON `showOutput` 无成功 toast + 检查器顶栏图标关闭

## 背景

Electron `JsonTool.showOutput`（`jsonToXml` / `jsonToBean` / JSONPath 查询）成功时只更新 `outputDialog` 与 `notice`，**不**调用 `toast.success`；失败仍 `showError` → `toast.error`。`runTransform` 与 `runInputConversion` 成功仍 toast。

检查器顶栏 `JsonInspector` 使用 14px `X` 图标关闭（`aria-label` = `common.close`），不是全文「关闭」按钮。

## 行为

- **F04**：`showResult` 成功路径去掉 `toastSuccess`；失败仍 `toastError`。结果对话框、检查器 `pathResult`/`notice`、历史写入不变。
- 检查器侧栏顶行关闭改为 `MooIconButton`（28×28、×14sp、`contentDescription` = `common.close`）。

## 验证

- 对照 `next/src/features/json/JsonTool.tsx` `showOutput` / `showError`
- `./gradlew :composeApp:desktopTest --offline`
