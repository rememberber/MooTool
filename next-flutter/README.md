# MooTool Next Flutter 开发文档

本目录规划 **MooTool Next Flutter** 独立桌面产品线。首版以当前 `next/` Electron 版的布局、样式、功能及操作语义为参照，使用 Flutter 实现，并改善桌面信息密度、可访问性、响应式布局和操作反馈。

**当前状态：开发指导文档已建立，Flutter 应用尚未实现。** 文档中的阶段、目录、命令和验收指标是后续开发要求，不表示已经完成或通过测试。

| 阅读顺序 | 文档 | 用途 |
| --- | --- | --- |
| 1 | [Cursor 开发主指南](docs/cursor-development-guide.md) | 产品边界、技术路线、现代 UI、架构、数据、多窗口、阶段计划 |
| 2 | [逐工具功能规格](docs/feature-parity.md) | 26 个入口、面板和操作顺序、算法兼容边界、关键验收 |
| 3 | [验收与交付标准](docs/acceptance.md) | 功能、视觉、持久化、跨平台、性能及产品互不影响的验证 |
| 4 | [Cursor 启动与续接提示词](docs/cursor-prompts.md) | 可以直接复制给 Cursor 的执行指令 |
| 自动约束 | [.cursor/rules/flutter-product.mdc](.cursor/rules/flutter-product.mdc) | 将本目录作为 Cursor 工作区时使用的产品约束 |

源码参考基线：2026-09-08，仓库提交 `ba6a9bdf0c590173cd35696f4c1a3a5fa5198392`，`next/package.json` 版本 `1.1.4`。这是本次读取的工作树基线，不声明所有历史截图都对应此版本。当前源码及测试优先于旧截图和旧规划。

各产品线可独立演进、独立发版、保留重复代码。Flutter 不依赖 Java、Electron、Tauri 或 macOS Native 产品的源码、构建产物和正在使用的数据；安装、启动、更新、卸载不得覆盖其他产品。

建议在 Cursor 中打开 `next-flutter/`，从提示词文档的“首次启动”开始。若打开仓库根目录，请显式附加本目录的规则和上述文档；不要假设嵌套规则在所有 Cursor 版本中都会自动加载。
