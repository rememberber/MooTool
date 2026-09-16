# DIFF-177：导航隐藏工具 ID 规范化

## 行为

- 对齐 Electron `normalizeNavigationToolIds`：加载设置与 Electron 设置迁入时，去掉首页 `mootool`、未知工具 ID 与重复项。
- `SettingsRepository.load()` 若规范化结果与磁盘不一致则写回。

## 验证

- `NavigationToolVisibilityTest.normalizeHiddenNavigationToolIdsMatchesElectronContract`
- `SettingsVaultPathSanitizeTest.loadNormalizesHiddenNavigationToolIds`
- `./gradlew :composeApp:desktopTest --offline`（**328/328**）
