# DIFF-311：Vault Git 默认提交说明文案

## 问题

Electron `json.git.defaultMessage` / `quickNote.git.defaultMessage` 用于 Git 面板提交框初始值与自动检查点语义。Compose 此前为泛化英文（如 “JSON vault checkpoint”），与 Electron `messages.ts` 不一致。

## 行为

- `json.git.defaultMessage`：各语言均为 `MooTool JSON checkpoint`（与 Electron 一致）。
- `quickNote.git.defaultMessage`：zh `MooTool 随手记检查点`、en `MooTool Quick Note checkpoint`、ja `MooTool クイックノート チェックポイント`。

## 验证

- `./gradlew :composeApp:desktopTest --offline`
