# 0.4.0 本机验收记录

- 验收系统：macOS 26.7，Intel x86_64；Swift 6.2.3 / Command Line Tools 26。
- 主程序和 JSON 辅助程序均构建为 arm64 + x86_64 Universal。Apple Silicon 与 macOS 14 未做真机运行验收。
- 39 组核心测试通过，包含旧工作区兼容、JSONPath、特殊键名、重复键、XML/JavaBean、Unicode 查找替换、超时与取消。
- 实际原生按钮/键盘/编辑器验收通过：原地格式化与自动保存、撤销/重做、转换弹窗、使用结果、输入转换、文档切换和工作区选项。
- 文档库、快速切换、编辑位置、跨窗口同步、导入原子性及新进程重启恢复通过。
- 64 个界面/主题截图，加 2 张转换结果弹窗截图；人工检查 JSON 检查器、查找高亮、路径树/预览、结果弹窗及窄窗口。
- `javac` 编译原生辅助进程生成的 Java 文件通过，覆盖关键字、字段重名和嵌套类型命名冲突。
- 应用复制到仓库外后，签名、应用标识、嵌入资源与真实 JSON 辅助程序调用通过。独立辅助程序的 4 秒终止计时通过阻塞输入用例验证。
- DMG 完整性、产物哈希及双架构检查通过。本地 ad hoc 签名，尚未做 Developer ID 签名与 Apple 公证。

## 产物

- 文件：`MooTool-Next-macOS-Native-0.4.0-mac-universal.dmg`
- 大小：7033468 字节
- SHA-256：`72de0b2ae7c3af92f600eab15e3c29bcd48110c5c3f356f646ae2b3d1a5710f8`
- 构建时间（UTC）：2026-09-07T23:50:51.026893+00:00

日志及完整截图在本产品 `dist/` 下：`check-0.4.0.log`、`smoke-0.4.0.log`、`java-0.4.0.log`、`package-0.4.0.log`、`verify-package-0.4.0.log`、`acceptance/report.json`。复现入口见 [README](../README.md#检查)。

这些验收不等于 Electron 全部高级功能已移植；剩余差异见 [功能对齐清单](parity.md)。
