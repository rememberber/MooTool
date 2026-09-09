# MooTool Next FX

使用 **OpenJDK + OpenJFX（JavaFX）** 开发的跨平台桌面工具箱独立产品线。以 `next/` Electron 版的布局、样式、功能和操作习惯为基线，在此基础上改善视觉层级、可访问性、键盘操作与窗口适配。

当前版本：`0.1.0-SNAPSHOT`（P0 工程与高风险验证）。产品 ID `next-fx`，完整名称 `MooTool Next FX`。

## Cursor 阅读入口

先读 [AGENTS.md](AGENTS.md)，再按以下顺序执行：

| 文档 | 内容 |
| --- | --- |
| [编码开发主指南](docs/cursor-development-guide.md) | 目标、边界、P0–P7 开发阶段、第一轮任务及完成标准 |
| [源码基线与参考地图](docs/baseline.md) | 实际源码、版本、默认值、容易误判的功能及官方技术资料 |
| [UI 与交互规格](docs/ui-spec.md) | 布局尺寸、页面结构、现代 UI Token、JavaFX CSS、窗口与键盘行为 |
| [逐工具功能规格](docs/feature-parity.md) | 首页 + 25 工具、11 类设置、功能细节和兼容验收 |
| [JavaFX 架构](docs/architecture.md) | 工程、线程、编辑器、状态、多窗口、算法和依赖策略 |
| [数据、平台与发布](docs/data-platform-release.md) | 独立身份、安装/数据隔离、Vault/Git、系统能力、自带运行时和更新 |
| [验收与进度](docs/acceptance.md) | 任务矩阵、测试场景、证据格式和共存测试 |
| [Cursor 提示词](docs/cursor-prompts.md) | 可直接复制的启动、续接、逐工具实现、视觉修订和发布审查任务 |

建议在 Cursor 单独打开 `next-fx/`。

## 本机命令

在 `next-fx/` 下执行，需要 JDK 25：

```bash
export JAVA_HOME=/Users/zhoubo/Library/Java/JavaVirtualMachines/azul-25.0.4.1/Contents/Home
./mvnw -version
./mvnw clean verify
./mvnw javafx:run
./scripts/package.sh --type app-image
./scripts/verify-package.sh "dist/MooTool Next FX.app"
```

开发身份默认 `--profile=dev`，数据写到 `com.rememberber.mootool.next.fx.dev`，不会打开其他产品的数据库。

## 产品独立性是硬约束

- 各产品拥有独立代码、构建、版本、依赖、安装身份、数据、凭据、进程锁、备份和更新通道。
- **允许重复代码，允许各自演进。** 禁止 parent POM、源码目录、符号链接或共享数据库依赖。
- 安装包自带匹配架构的 OpenJDK 运行时与 JavaFX；普通用户无需安装 Java。

下一步：在真实桌面补 IME / 50 次窗口转移证据，再做 P1 首页与导航视觉（1440×920 / 1080×720 截图）。不要把 24 个占位工具做成假页面。
