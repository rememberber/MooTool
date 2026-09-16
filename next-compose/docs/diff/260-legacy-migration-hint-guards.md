# DIFF-260：迁移提示显隐与导入后关闭

## 行为

- `shouldShowLegacyMigrationHint`：已 dismiss、或设置页/命令盘/自定义分组管理器打开时不显示 `LegacyMigrationHintDialog`（避免与其它全屏 overlay 叠层）。
- 跨产品导入确认成功后在 `updateSettings` 中写入 `legacyMigrationHintDismissed = true`，与用户在设置内完成迁移的期望一致。

## 验证

- `LegacyMigrationHintPolicyTest`
- `./gradlew :composeApp:desktopTest --offline`
