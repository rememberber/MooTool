# DIFF-470：MooTool MCP 工具 `strictObject` 参数

## 背景

Electron `tools.ts` 用 `z.strictObject` 定义各 MCP 工具入参：未知字段与错误类型（如 `text: 123`）在 `schema.parse` 阶段失败。

## 变更

- `MooToolMcpTools`：各工具 `requireKnownKeys` + 字符串/整数边界（`text` ≤100k、timestamp `text` ≤100、JSONPath ≤1000、diff 两侧 ≤8000 等），`hash` 仅允许 `md5/sha1/sha256/sha384/sha512`。
- `MooToolMcpToolsTest`：补充 `mootool_hash` 非字符串 `text` 与未知 `path` 字段失败用例。

## 验证

```bash
./gradlew :composeApp:desktopTest --offline
```
