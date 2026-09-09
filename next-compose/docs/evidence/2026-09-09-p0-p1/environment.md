# 环境

- 日期：2026-09-09
- 机器：macOS Darwin 25.6.0，Intel Core i7-1068NG7 x86_64
- JDK：Zulu 21.0.12.1，`JAVA_HOME=/Users/zhoubo/Library/Java/JavaVirtualMachines/azul-21.0.12.1/Contents/Home`
- 构建：Gradle Wrapper 8.14.3，Kotlin 2.2.21，Compose Multiplatform 1.12.0
- 产品版本：`appVersion=0.1.0`；macOS 安装包 `packageVersion=1.1.0`（见 ADR-001）
- 应用 ID：`com.rememberber.mootool.next.compose`
- 数据覆盖：`MOOTOOL_COMPOSE_DATA_DIR`
- 官方 Compose 1.12.0 平台表仅列 macOS arm64；本机 Intel 结果不能外推为 arm64 已验收
