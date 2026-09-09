# 本轮验收记录

- 阶段/条目：F07 Protobuf（P3 本地算法）
- 本产品工作树：仅 `next-compose/`
- 依赖：`com.google.protobuf:protobuf-java` / `protobuf-java-util` / `protoc` **4.29.3**（构建拉取 osx/linux/windows 分类器，资源名 `helpers/protoc-*`）
- 参考：Electron `protobufTools.ts`、`ProtobufTool.tsx`；架构要求 protoc + DynamicMessage
- 已执行：`JAVA_HOME` Zulu 21.0.12.1，`./gradlew :composeApp:desktopTest`，**67/67** 通过（ProtobufEngine 5）
- 语义：JSON↔Binary（Hex/Base64）、Wire 无需 schema、定义格式化、历史与分离窗口；protoc 8s 超时；输入 1 MiB / 二进制 2 MiB 上限
- 差异：DIFF-007（JsonFormat vs protobufjs；单文件 import 根）
- 未测：窗口截图、恶意超大 descriptor、自定义 import root UI、分离窗口手工、`createDistributable` 内 protoc 解出
- 下一轮：P3 本地算法主路径已齐；可做 P4 或 F04 Git / F05 代码运行
