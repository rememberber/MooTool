# DIFF-182：Java Host 当前方案名迁入

## 行为

- **A03 / F10**：Java `func.host.currentHostName` 在跨产品迁移确认后，按名称匹配已迁入的 `HostProfileStore` 方案，写入 `HostSession.selectedId` 与编辑内容（在 SQLite 方案导入之后执行）。

## 验证

- `LegacyJavaSettingsTest.applySessionPatchesSelectsHostProfileByJavaCurrentName`
- `./gradlew :composeApp:desktopTest --offline`（**339/339**）
