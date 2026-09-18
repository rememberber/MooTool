# DIFF-638：F04 JSON 检查器 JSONPath 复制/查询 `*ActionEnabled`

基线：DIFF-637（工作区）。

## 范围

- **F04**：`JsonInspectorPresentation.pathCopyActionEnabled`（委托 `pathCopyEnabled`）、`pathQueryActionEnabled`（空/纯空白路径禁用）；`JsonScreen` 检查器 JSONPath 复制/查询钮 `enabled` 接线。
- **单测**：`JsonInspectorPresentationTest` 补充 `pathCopyActionEnabled` / `pathQueryActionEnabled`。

**不重复** 600/603：600/603 为复制 toast 与查询失败 toast；本切片为按钮禁用态。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
