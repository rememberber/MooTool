# DIFF-453：AI 预览隐私与安装备份

## 背景

Electron `previews without writing, installs both modes, backs up and is idempotent` 要求 Codex 预览 JSON 不含用户 `private-model`、安装前备份原 TOML、二次安装无备份。Claude 更新已有 JSON 时同样备份。

## 行为

- `codexBothPreviewOmitsPrivateTomlAndInstallBacksUp`：预览序列化不含 `private-model`、不写盘、安装 1 份备份、再预览全 `Unchanged` 且无新备份。
- `mcpInstallBacksUpExistingClientConfig`：已有 `.claude.json` 安装 MCP 时生成备份且内容为安装前原文。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（607/607）
