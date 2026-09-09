# 对照

| 项 | 源 | FX | 状态 |
| --- | --- | --- | --- |
| 设置 11 类顺序 | SettingsWindow.tsx | `SettingsCategory` | 通过（框架） |
| 布局默认最近关闭、分隔线开 | settings.ts | `Settings.defaults()` | 通过 |
| 侧栏 248 / 隐藏标题 84 | ui-spec / CSS | `NavigationLayout` | 通过（几何）；视觉截图未拍 |
| 最近最多 5 | Electron | `RecentTools` | 通过（模型） |
| E03 50 次转移 | 桌面 | `ToolWindowCoordinatorTest` 同一 Label Node | 自动通过；IME/选区/undo 桌面未测 |
| 六风格 | modern…claude | 仅 modern 深浅生效 | 延期 P6，见设置文案 |
