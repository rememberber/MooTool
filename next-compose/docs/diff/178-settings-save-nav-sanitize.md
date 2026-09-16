# DIFF-178：设置保存时导航隐藏列表规范化 + 侧栏「全部」分隔线

## 行为

- **A01**：`SettingsRepository.save()` 经 `sanitizeLoadedSettings`，写入前即去重/过滤 `hiddenNavigationToolIds`（与 load 一致）。
- **A01**：「全部隐藏」按钮在 `visibleNavigationToolCount == 0` 时禁用；「全部显示」在全部可见时禁用（对齐 Electron Set 语义，重复隐藏项不计数）。
- **P1**：存在自定义分组且仍有内置工具可见时，才显示 `app.group.all` 分隔线（对齐 `Workbench.tsx` `visibleBuiltinToolCount > 0`）。

## 验证

- `NavigationToolVisibilityTest.visibleNavigationToolCountIgnoresDuplicateHiddenEntries`
- `SettingsVaultPathSanitizeTest.saveNormalizesHiddenNavigationToolIds`
- `./gradlew :composeApp:desktopTest --offline`（**329/329**）
