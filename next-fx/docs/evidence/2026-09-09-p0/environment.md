# 环境

- 日期：2026-09-09
- 仓库 HEAD（取证时）：`fa4910eef1bda86a5eaa549fcc1d5086607b1770`（`feat(next-compose): add quick note vault and 24 replace actions`）
- 本产品工作树：仅 `next-fx/` 新增/修改；未改其他产品。并行工作树中另有 `next-compose` / `next-macos-native` 等未提交改动，本轮未触碰。
- Electron 参照：`next/package.json` 1.1.4
- OS：macOS 26.7 (25G224)，Darwin 25.6.0，`x86_64`
- JDK：Azul Zulu 25.0.4.1 LTS（`Zulu25.36+205-CA`），`JAVA_HOME=/Users/zhoubo/Library/Java/JavaVirtualMachines/azul-25.0.4.1/Contents/Home`
- OpenJFX：26.0.2（Maven 模块开发；Gluon jmods 用于 jlink）
- Maven Wrapper：3.3.4 分发 Apache Maven 3.9.16
- 测试数据：单元测试使用 JUnit `@TempDir`。app-image smoke 使用本产品 release 身份目录 `~/Library/Application Support/com.rememberber.mootool.next.fx/`，未读写其他产品数据。
