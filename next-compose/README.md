# MooTool Next Compose

面向 macOS、Windows、Linux 的 **Compose Multiplatform Desktop 独立产品线**。布局、操作习惯和功能以当前 Electron 版为主要参考，Java 版补充算法与桌面能力；在此基础上改善层级、密度、键盘操作、窗口适配和可访问性。

**当前状态：0.1.0 开发切片。** 已有独立 Gradle Wrapper 工程，本机可构建、测试，并生成带自有 runtime 的 macOS app-image。已实现桌面壳、26 个入口、modern 明暗、基础设置，JSON 切片，文本对比，格式化，配置转换，Protobuf，编码解码，加解密/随机，二维码，UA 分析，正则，Cron，时间转换，以及计算器。其余工具明确显示尚未实现。这不是完整 P2/F04，也不是三平台发行验收。

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

## 锁定的工具链

本机已验证（Intel macOS x86_64，不能代替官方 arm64 矩阵）：

| 组件 | 版本 |
| --- | --- |
| 产品版本 | 0.1.0（`gradle.properties` 的 `appVersion`） |
| JDK | 21（Zulu 21.0.12.1） |
| Gradle | Wrapper 8.14.3 |
| Kotlin | 2.2.21 |
| Compose Multiplatform | 1.12.0 |

macOS `jpackage` 拒绝 `0.x` 作为 `--app-version`，安装包元数据映射为 `1.1.0`，界面仍显示 0.1.0。见 [ADR-001](docs/adr/001-macos-jpackage-version.md)。

## 开发与运行

需要 JDK 21。不要把本产品数据写到其他 MooTool 目录：

```bash
cd next-compose
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./scripts/check-toolchain.sh
./scripts/check-core.sh
MOOTOOL_COMPOSE_DATA_DIR="$PWD/local.properties.d/dev-profile" ./scripts/run.sh
```

已核实的 Gradle 任务：

```bash
./gradlew --version
./gradlew :composeApp:printTooling
./gradlew :composeApp:desktopTest
./gradlew :composeApp:run
./gradlew :composeApp:createDistributable
```

`createDistributable` 本机输出：

```text
composeApp/build/compose/binaries/main/app/MooTool Next Compose.app
```

bundle ID 为 `com.rememberber.mootool.next.compose`，应用名为 `MooTool Next Compose.app`，不会写成 `MooTool.app`。Windows MSI / Linux DEB 需在对应 OS 执行 `packageDistributionForCurrentOS`。

常用操作：`⌘/Ctrl+K` 搜索工具，`⌘/Ctrl+,` 打开设置。JSON 支持格式化、压缩、查找替换、导入导出、历史和左侧 Vault。文本对比支持行/字符差异、三种高亮、忽略空白、统一补丁与上/下跳转。格式化支持 Nginx/Java/XML/HTML、缩进选择、文件另存与语法错误定位（与 Electron Prettier 差异见 [DIFF-005](docs/diff/005-reformat-jvm.md)）。配置转换支持 Properties ↔ YAML、校验/格式化与导入导出（冲突与 YAML 1.1 见 [DIFF-006](docs/diff/006-config-snakeyaml.md)）。Protobuf 使用捆绑 protoc 4.29.3 动态编译用户粘贴的 `.proto`，支持 JSON↔Binary、Wire 与 Hex/Base64（JSON 映射见 [DIFF-007](docs/diff/007-protobuf-jsonformat.md)）。编码解码支持 Unicode、URL（UTF-8/GB2312）、UTF-8 Hex 与 ASCII 码点列表。加解密支持 AES/DES/SM4、RSA/SM2、摘要、Base64/Base32 与安全随机（密钥字节规则见 [DIFF-008](docs/diff/008-crypto-key-bytes.md)）。二维码使用 ZXing 生成/识别 PNG，支持纠错、Logo、保存与剪贴板（历史不存 PNG，见 [DIFF-009](docs/diff/009-qr-history-png.md)）。调色板支持 HEX/RGB、7 主题与 10 标准色、五种运算、屏幕取色与文件夹收藏（Robot 冻结截图见 [DIFF-010](docs/diff/010-color-screen-picker.md)）。留言板支持预设、主题、自动字号与沉浸展示（屏幕常亮见 [DIFF-011](docs/diff/011-message-board-wake.md)）。UA 分析使用 `uap-java` 规则并归一 Chrome/Safari/Firefox 名称（引擎推断见 [DIFF-002](docs/diff/002-ua-engine-inference.md)）。正则使用 Java Pattern，经独立 worker 超时终止，并带 21 条常用模式与收藏（引擎差异见 [DIFF-003](docs/diff/003-regex-java-pattern.md)）。Cron 使用 Quartz 6/7 字段、IANA 时区与未来 10 次运行（与 Electron 差异见 [DIFF-004](docs/diff/004-cron-quartz.md)）。时间转换支持显式秒/毫秒、IANA 时区、历史和大屏时钟（单位语义见 [DIFF-001](docs/diff/001-time-explicit-unit.md)）。计算器支持四则表达式、2/10/16 进制、GCD/LCM 与排列组合，不用 eval。

## 产品独立原则

- 产品 ID：`next-compose`；完整名称：`MooTool Next Compose`。
- Compose Desktop 使用自身打包的 JVM 运行时，**不依赖安装 MooTool Java**。
- 允许复制有权使用的代码、图标、样本和算法，在本目录维护独立副本；不要求跨产品抽公共库。
- 自有构建、版本、安装标识、数据目录、凭据、单实例锁、备份及更新通道；不同版本产品线可同时安装、运行和卸载。
- 阅读相邻产品源码只为建立参照，不能把它们变成构建或运行依赖。
- 系统 hosts、系统环境变量以及用户主动指定的同一文件是系统/用户资源；此类显式操作的影响不能靠产品 ID 隔离，界面必须清楚区分。

文档基线日期：2026-09-08。工程落地记录：2026-09-09。后续变更从本产品需求出发，不要求永久追随其他实现。
