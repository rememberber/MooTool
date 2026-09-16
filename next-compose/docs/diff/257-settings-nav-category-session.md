# DIFF-257：设置页导航分类会话记忆

## 对照 Electron

`SettingsPage` 在工具会话内用 `activeCategory` 记住当前左侧分类；离开设置再返回时不应总回到「通用」。

## 行为

- `AppContainer.settingsNavCategoryId` 保存当前分类 id（与 `settings.{id}` 文案键一致，如 `vault`、`ai`）。
- 点击左侧导航时写回；再次打开设置页从该 id 恢复。
- `SettingsNavCategory` + `settingsNavCategoryFromStorageId` 负责解析与回退 `general`。

## 验证

- `SettingsNavCategoryTest`
- `./gradlew :composeApp:desktopTest --offline`
