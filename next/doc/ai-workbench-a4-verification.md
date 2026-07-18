# AI 工作台 A4 验证记录

记录日期：2026-07-18  
平台基线：macOS x64 / Intel MacBook

## 已完成切片

- 客户端 Adapter：Codex、Claude Code、Cursor、Gemini CLI、GitHub Copilot。
- 模型运行时 Adapter：Ollama、LM Studio、llama.cpp、vLLM、LocalAI。
- 经验证的生命周期：Ollama 拉取/加载/卸载/删除；LM Studio 下载/加载/卸载。未发现官方稳定删除 API 的运行时不开放删除按钮。
- Prompt Lab：持久化测试集、输出评分、耗时与 Token 对比。
- Agent Profile 分享、Project Starter、Context Inspector 和受控 Codex/Claude CLI 任务。
- Claude Code Auto Memory 官方目录只读发现；不写回、不自动晋升长期记忆。
- 可选本地 Memory Embedding：Ollama `/api/embed`、LM Studio `/v1/embeddings`，事务重建、取消保留旧索引、内容指纹失效、语义预览和覆盖率指标。

## 安全边界

- Memory Embedding 只调用回环地址；LAN/远程 Endpoint 被拒绝。
- 仅 `public` / `internal` 记忆进入向量索引，`private` / `restricted` 明确计数并跳过。
- LM Studio Token 从 `safeStorage` 在主进程读取，不经过 Renderer、SQLite、日志或返回对象。
- FTS 始终可用且仍为默认检索；未安装 Ollama/LM Studio 不影响基础记忆管理。
- 模型操作和 CLI 任务均需显式批准，并支持取消、超时、输出上限与退出时清理。

## 自动化结果

- TypeScript 严格类型检查通过。
- Vitest：75 个测试文件、268 项测试通过。
- Electron/Vite 生产构建通过。
- Playwright：35 项 Electron E2E 全部通过；其中包含本地 Embedding 纵向链路（重建索引、覆盖率、语义检索与作用域内结果）。

Multi-Agent / Worktree 的稳定性结论见 [ai-multi-agent-worktree-evaluation.md](./ai-multi-agent-worktree-evaluation.md)。
