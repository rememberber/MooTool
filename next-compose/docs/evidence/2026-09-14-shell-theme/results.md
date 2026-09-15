# 证据：壳/主题/编辑器/安装声明（2026-09-14）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **184/184** 通过，0 skipped，0 failed
- 覆盖：
  - 折叠导航 `MooTooltip`、JSON 检查器关闭
  - hero/miui-v5/claude/smartisan token 对齐 Electron CSS 变量（对比度仍 ≥ 4.5:1）
  - 编辑器 workspace + `--syntax-*` 方案
  - 拆出窗口最小 960×640
  - IME 预编辑 `committed=0` 不改文；列粘贴短行补齐
  - 加密/HTTP 历史遮蔽
  - macOS appCategory / Info.plist / 产物命名约定
- 色板 PNG（token 辅助，**不是窗口截图**）：`boards/*.png`
- 未测：真实窗口截图、IME/列编辑手工手势、macOS 公证、Windows/Linux/arm64 安装升级卸载
