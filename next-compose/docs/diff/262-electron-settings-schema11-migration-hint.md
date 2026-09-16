# DIFF-262：Electron 设置迁入 schema&lt;11 迁移提示

## 对照 Electron

`next/src/shared/contracts/settings.ts` 的 `mergeSettings`：当 `previousSchema < 11` 且未显式传入 `legacyMigrationHintDismissed` 时，将该项设为 `true`，避免老用户升级后再次看到 Java 迁移首启提示。

## 行为（next-compose）

- `ElectronNextSettingsImport.sanitize`：若迁入的 `schemaVersion < 11`，强制 `general.legacyMigrationHintDismissed = true`。
- `schemaVersion >= 11` 时保留迁入文件中的布尔值（含显式 `false`）。

## 验证

- `ElectronNextSettingsImportTest.merge_oldElectronSchema_autoDismissesMigrationHint`
- `ElectronNextSettingsImportTest.merge_schema11_keepsExplicitMigrationHintFalse`
- `./gradlew :composeApp:desktopTest --offline`
