# 证据：字体选择、960 按需辅助栏、快捷键冲突（2026-09-15）

- 覆盖：`SystemFonts.list`（回退字体 + 当前值）、`ShortcutBindings.conflict`、`LayoutPolicy` 紧凑辅助栏互斥、JSON/随手记工具栏字体与 960 切换代码
- `desktopTest` **193/193**（0 skipped）
- Compose 场景 PNG（**不是**真实窗口截图）：`docs/evidence/2026-09-15-tray-density/captures/compact-960-focus-ring.png`
- `JFrame.paint` 辅助图（**不是**用户窗口手势）：`captures/jframe-paint-960.png`
- 未测：IME/列编辑手工窗口、托盘权限对话框、三平台安装/升级/卸载、六套 CSS 逐选择器皮肤
