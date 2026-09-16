# DIFF-259：Java 版迁移提示对话框

## 对照 Electron

`LegacyMigrationHintDialog` 在 `general.legacyMigrationHintDismissed == false` 时展示，引导用户到「数据与备份」迁移；「打开设置」调用 `openSettings('data')`。

## 行为

- `GeneralSettings.legacyMigrationHintDismissed`（默认 `false`）持久化到本产品设置。
- `LegacyMigrationHintDialog` 挂到 `Workbench`；「知道了」与遮罩关闭写回 `true`；「打开设置」先 dismiss 再 `openSettings(categoryId = "data")`。
- Electron 设置合并时带入 `legacyMigrationHintDismissed`。

## 验证

- `ElectronNextSettingsImportTest`
- `./gradlew :composeApp:desktopTest --offline`

## 后续（DIFF-260）

- 设置/命令盘/自定义分组打开时不叠提示；跨产品导入成功后自动 `legacyMigrationHintDismissed = true`。见 [DIFF-260](260-legacy-migration-hint-guards.md)。
