# DIFF-655：F07 Protobuf `*ActionEnabled`

基线：DIFF-654 补记（工作区）。

## 范围

- **F07**：`ProtobufWiringPresentation` 各 Tab 转换/解码/复制 `*ActionEnabled`；`ProtobufScreen` 与溢出「复制」`enabled` 接线。
- **单测**：`ProtobufWiringPresentationTest.toolbarActionEnabledMatchesGuards`。

## 验证

JDK 21：`./gradlew :composeApp:desktopTest --offline --no-daemon`  
`./scripts/verify-product-evidence-prep.sh`
