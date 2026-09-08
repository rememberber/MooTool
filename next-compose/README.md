# MooTool Next Compose

面向 macOS、Windows、Linux 的 **Compose Multiplatform Desktop 独立产品线**。布局、操作习惯和功能以当前 Electron 版为主要参考，Java 版补充算法与桌面能力；在此基础上改善层级、密度、键盘操作、窗口适配和可访问性。

**当前状态：开发规格已编写，尚未创建 Gradle 工程，尚无已实现或已验收的功能。** 文档中的目录、接口、构建命令和里程碑是后续开发要求，不是运行结果。

## 给 Cursor 的阅读入口

建议在 Cursor 中直接打开 `next-compose/`，先读 [AGENTS.md](AGENTS.md)，再依次阅读：

| 文档 | 解决的问题 |
| --- | --- |
| [编码开发主指南](docs/cursor-development-guide.md) | 目标、范围、优先级、开发阶段和第一轮任务 |
| [源码基线与参考地图](docs/baseline.md) | Electron/Java 的实际版本、源码位置、已核实差异 |
| [UI 与交互规格](docs/ui-spec.md) | 窗口布局、视觉 Token、页面结构、现代化规范 |
| [逐工具功能规格](docs/feature-parity.md) | 首页、25 个工具、设置及通用能力的功能与验收要求 |
| [Compose 技术架构](docs/architecture.md) | Kotlin/JVM、编辑器、线程、状态、多窗口和算法选型 |
| [数据、平台与发布](docs/data-platform-release.md) | 独立安装、数据、Vault、系统能力、更新和发行 |
| [验收与进度记录](docs/acceptance.md) | 可执行检查、逐项状态、视觉与平台验收 |
| [Cursor 任务提示词](docs/cursor-prompts.md) | 可复制的启动、续接、逐工具开发和审查指令 |

打开整个 MooTool 仓库时，请把 [启动提示词](docs/cursor-prompts.md) 发送给 Cursor，明确仅开发 `next-compose/`；不要假设嵌套目录中的 `.cursor/rules` 在所有工作区打开方式下都会自动启用。

本产品 `.gitignore` 已局部放行开发文档与 `.cursor/rules/next-compose.mdc`，避免被仓库根的通用忽略规则排除；无需改动其他产品配置。

## 产品独立原则

- 产品 ID：`next-compose`；完整名称：`MooTool Next Compose`。
- Compose Desktop 使用自身打包的 JVM 运行时，**不依赖安装 MooTool Java**。
- 允许复制有权使用的代码、图标、样本和算法，在本目录维护独立副本；不要求跨产品抽公共库。
- 自有构建、版本、安装标识、数据目录、凭据、单实例锁、备份及更新通道；不同版本产品线可同时安装、运行和卸载。
- 阅读相邻产品源码只为建立参照，不能把它们变成构建或运行依赖。
- 系统 hosts、系统环境变量以及用户主动指定的同一文件是系统/用户资源；此类显式操作的影响不能靠产品 ID 隔离，界面必须清楚区分。

文档基线日期：2026-09-08。后续变更从本产品需求出发，不要求永久追随其他实现。
