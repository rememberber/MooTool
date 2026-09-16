# DIFF-258：按分类打开设置页

## 对照 Electron

`window.mootool.openSettings(category?)` 经 `open-settings` 导航事件传入 `data` / `runtime` 等分类 id；迁移提示打开 `data`，运行台缺运行时引导打开设置。

## 行为

- `AppContainer.openSettings(open, categoryId?)`：非空 `categoryId` 时规范化写入 `settingsNavCategoryId` 再显示设置。
- `SettingsScreen` 在设置页可见时根据 `settingsNavCategoryId` 同步左侧选中项。
- F05 运行台在当前运行时不可用时显示横幅与「打开运行环境设置」，直达 `runtime` 分类（比 Electron 仅打开设置更明确，仍满足配置路径闭环）。

## 验证

- `SettingsNavCategoryTest`
- `./gradlew :composeApp:desktopTest --offline`
