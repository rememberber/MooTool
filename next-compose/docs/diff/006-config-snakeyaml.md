# DIFF-006：配置转换冲突报错与 SnakeYAML 1.1

- 编号：DIFF-006
- 影响：F06 配置文件转换
- 日期：2026-09-09

## 原行为（Electron）

`yaml` 包（YAML 1.2）解析/序列化。Properties 路径展开时，标量与对象/数组冲突用 `??=` 保留先写入的值，后续键**静默丢弃**。格式化走 `parseDocument`，可能保留部分注释。`yes`/`on`/`no` 在 1.2 中是普通字符串。

## 本产品行为

路径展开规则对齐 `configTools.ts`（点路径、`[index]`、标量数组合并为逗号列表、`null` 写成空格）。YAML 读写使用 SnakeYAML 2.3（YAML 1.1 + `SafeConstructor`，限制别名与输入大小）。**标量与对象/数组互相覆盖时抛出 `conflict`，不丢值**。格式化是 load 后再 dump，不保留注释/锚点。

## 理由

规格要求冲突不能静默丢值，并允许有记录的规则。SnakeYAML 是架构指定的 JVM YAML 解析器，且 Java 版同样使用它。

## 证据

`ConfigEngineTest`：Electron 嵌套/列表样本、`tags=a,b` 扁平化、校验与格式化幂等、`server=foo` 再写 `server.port` 报 `conflict`、非对象根拒绝、非法 YAML 抛错。

## 受影响范围

- 原先在 Electron 被丢掉的冲突键，现在会失败并保留两侧原文。
- `yes`/`on`/`no`/`off` 可能被当成布尔，往返 Properties 变成 `true`/`false`。
- YAML 注释、锚点、自定义 tag 不保证无损。
