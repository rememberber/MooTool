# DIFF-134：AI JSON 重复键与 MCP 工具清单校验

对照 Electron `mergeJson` 的 `jsonc-parser` visit + `testConnection` 工具列表。

## 范围

- `mergeJson` / `parseJsonRoot`：启用 Jackson `STRICT_DUPLICATE_DETECTION`（保留 JSONC 注释与尾逗号），重复键报错文案与 Electron 一致。
- `testConnection`：校验通用 7 个 + Vault 4 个 MCP 工具均已注册。
- 单测：`mergeJsonRejectsMalformedAndDuplicateKeys`。

## 文件

- `AiClientConfigMerge.kt`、`AiIntegrationService.kt`、`AiClientConfigMergeTest.kt`
