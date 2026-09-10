# 结果

| 命令 | exit | 观察 |
| --- | --- | --- |
| `./mvnw test` | 0 | 不含 `@Tag("ui")`；新增 NavigationLayout/RecentTools/SettingsStore/SettingsCategory 等 |
| `./mvnw -Pui-tests test` | 0 | 31 tests，含 `ToolWindowCoordinatorTest`：同一 Node 分离/收回 50 次，首页不可分离 |

实现：

- 11 类设置按 Electron 顺序；通用/外观/布局/编辑器/关于/数据路径有真实控件；其余类别标明开发中且无假开关
- 导航：248/84、紧凑行高、分隔线、最近 5 项、搜索、右键分离、设置返回原工具、Esc 关闭设置
- 强调色与 12–18 字号写入 ThemeSnapshot
- 编辑器 composition 期间不同步 DocumentSession（IME 桌面候选窗仍待测）
