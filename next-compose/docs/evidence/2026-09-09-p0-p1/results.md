# 本轮验收记录

- 阶段/条目：P0 + P1 + JSON 最小纵向切片（不是完整 P2/F04）
- 本产品工作树：`next-compose/` 新建 Gradle 工程与源码；未修改其他产品目录
- 参考产品：Electron `next` 1.1.4（阅读 `toolRegistry.ts`、`jsonTools.ts`、`JsonToolbar.tsx`、`HomePage.tsx`）；仓库当时 HEAD 与基线文档 9fdd130 不同，本轮未改写冻结基线
- OS/arch/JDK/Compose/Kotlin/Gradle：macOS 15-family Darwin 25.6.0，Intel Core i7 x86_64，Zulu JDK 21.0.12.1，Compose Multiplatform 1.12.0，Kotlin 2.2.21，Gradle 8.14.3
- 实际测试数据目录：`MOOTOOL_COMPOSE_DATA_DIR=/tmp/mootool-compose-smoke2-*`（隔离 profile）
- 已执行命令及退出结果：
  - `./gradlew :composeApp:printTooling` 成功
  - `./gradlew :composeApp:desktopTest` 成功，13 个测试 0 失败（JsonEngine 7、FindReplace 2、ToolRegistry 2、StorageIsolation 2）
  - `./gradlew :composeApp:createDistributable` 成功，产物 `composeApp/build/compose/binaries/main/app/MooTool Next Compose.app`
  - 安装镜像启动：进程保持存活，创建 `product.json` / SQLite / vault 目录；日志无异常
- 手工步骤与真实结果：用隔离目录启动镜像；因本环境无窗口截图权限，未保存运行截图
- 截图/日志位置：本目录；镜像启动日志曾出现缺少 `java.sql`，已通过 jlink modules 修复后再测通过
- 已通过条目：独立构建、产品身份、26 入口注册、JSON 算法 fixture、设置/历史 SQLite、本机 app-image 启动
- 未测/失败及原因：中文 IME/列编辑/5MiB 编辑器、分离收回 undo、视觉明暗截图、Windows/Linux 构建、动态 protoc 捆绑、完整 Vault Git、更新通道
- 已记录 DIFF/ADR：ADR-001 jpackage 0.x 映射；ADR-002 RSTA 宿主；ADR-003 Intel Mac 条件目标
- 下一轮具体任务：补编辑器桌面交互证据；JSON Vault 监视/冲突与 JSONPath 弹层对齐；实现 F13/F18 等下一个完整工具；在 arm64 runner 验证官方平台
