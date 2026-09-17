# ADR-012：Tauri 产品线 AI / MCP 集成范围

> 状态：已采纳  
> 日期：2026-09-17

## 背景

MooTool Next Electron 通过 Node 主进程运行 MCP Server，并向 Codex、Claude、Cursor 等客户端安装 MCP 与 Skill 文件。MooTool Next Tauri 独立产品线不以 Electron 目录为源码上游，且正式方向是不以 Node Sidecar 作为长期业务后端。

## 决策

1. **Tauri 1.0 不交付**与 Electron 等价的「设置 → AI 集成」安装器与内置 MCP 进程；该能力保留在 Electron 产品线。
2. Tauri 用户仍可通过 JSON Vault、备份、显式导入与 JSON 工作台 Git 完成数据协作；AI 客户端如需 Vault 访问，应使用独立部署的 MCP（例如 Electron 版安装产物或未来 Tauri 专用 MCP 发行物），不假设与 Tauri 安装包捆绑。
3. 若后续为 Tauri 增加 AI 集成，必须：在 `next-tauri` 内维护独立 MCP 入口（Rust 或经 ADR 批准的可审计 Sidecar）、独立 `access.json` 与安装 receipt，并更新 [`electron-parity.md`](../electron-parity.md) 与功能基线。

## 结果

- 与 Electron 的 **AI 集成差异** 为已批准的产品线边界，不是遗漏的 1.0 阻塞项。
- 体验对照以 [`electron-parity.md`](../electron-parity.md) 为准；不得将 Electron `toolRegistry.status` 中的 `parity-review` 直接映射为 Tauri 未完成。
