# DIFF-168：设置页 / 配置转换分栏与 Electron 迁入

## 背景

Electron 设置窗口 `settings-page`（220:780，最小 180/420）；配置转换 `config-convert` / `config-validate` 为三列 IO 布局（与编码工具同构）。compose 此前固定 220dp 设置导航、配置页固定中栏宽度，且迁入未覆盖上述键。

## 行为

- **A01**：设置左侧分类导航可拖，写入 `layout.paneSizes["settings-page"]` 索引 0。
- **F06**：转换与校验 Tab 各三列可拖（双手柄），分别写入 `config-convert` / `config-validate` 索引 0、1。
- **A03**：`ElectronPaneSizeImport` 增加上述三键映射；`mapIoThreePane` / `mapTwoColumnWorkspace` 统一使用字符串 pane key（与 Electron storageKey 一致）。

## 验证

- `ElectronPaneSizeImportTest.convertsSettingsPageAndConfigPaneRatios`
- `./gradlew :composeApp:desktopTest --offline`
