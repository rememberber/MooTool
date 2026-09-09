# DIFF-007：Protobuf JSON 使用 Java JsonFormat

- 编号：DIFF-007
- 影响：F07 Protobuf
- 日期：2026-09-09

## 原行为（Electron）

`protobufjs` `parse(..., { keepCase: true })`，`toObject({ defaults: true, longs: String, enums: String, bytes: String })`。用户粘贴的 `.proto` 在进程内解析，不调用 `protoc`。

## 本产品行为

构建期从 Maven 拉取官方 `com.google.protobuf:protoc:4.29.3` 各 OS/arch 可执行文件，运行时解出当前平台二进制，在临时目录编译 descriptor set（`--include_imports`，8s 超时，定义上限 1 MiB）。编解码走 `DynamicMessage` + `JsonFormat`（`preservingProtoFieldNames`、默认字段、int64 JSON 字符串、enum 名、bytes Base64）。Wire 解码不需要 `.proto`。任意 `import` 只解析临时工作目录，不自动拉网。

## 理由

规格要求用户粘贴全新 message 无须重编译应用，且走 protoc + Descriptor，而不是写死几种 message 或依赖本机已安装 protoc。

## 证据

`ProtobufEngineTest`：Person JSON↔Hex 往返、Hex/Base64 互转、Wire `field=1 value="Moo"`、定义格式化、nested/map/oneof/int64、非法 proto/缺 message/坏 Hex/截断 Wire。

## 受影响范围

- JSON 排版与缺省 repeated 是否输出空数组，可能与 protobufjs `defaults: true` 不完全一致。
- 用户 `import` 其他文件或未随包的非 well-known 路径会编译失败。
- `JsonFormat` 对未知 JSON 字段默认忽略（与 Java 工具一致），不会像 `verify` 那样全部拒绝。
