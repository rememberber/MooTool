# MooTool AI 接入

此功能属于 **MooTool Next Electron（`next/`）**。打开「设置 → AI 接入」，选择客户端和方式，查看安装位置与新增内容后点击「一键安装」。安装会先完成 MCP 初始化、工具发现和一次 JSON 调用，再写入配置。完成后重启客户端或重新加载 MCP / Skill，并按客户端提示启用工具。

## 客户端与安装位置

| 客户端 | MCP 用户配置 | Skill 用户目录 |
| --- | --- | --- |
| Codex | `$CODEX_HOME/config.toml`，默认 `~/.codex/config.toml` | `~/.agents/skills/mootool/` |
| Claude Code | `~/.claude.json`，支持 `CLAUDE_CONFIG_DIR` | `~/.claude/skills/mootool/`，支持 `CLAUDE_CONFIG_DIR` |
| Cursor | `~/.cursor/mcp.json`，保留 JSONC 注释 | 当前入口仅安装 MCP |

MCP 名称为 `mootool`。其他支持本地 stdio MCP 的客户端可使用页面的「复制 MCP 配置」按钮，按该客户端的配置格式添加。这里的本地可执行文件路径不能用于远程云端会话。

安装遵循 [Codex MCP 配置](https://learn.chatgpt.com/docs/extend/mcp?surface=cli)、[Codex Skill 目录](https://learn.chatgpt.com/docs/build-skills)、[Claude Code MCP 用户作用域](https://code.claude.com/docs/en/mcp)、[Claude Code Skills](https://code.claude.com/docs/en/skills) 和 [Cursor MCP 配置](https://cursor.com/docs/mcp)。

## 可调用能力

| 工具 | 功能 |
| --- | --- |
| `mootool_json_format` | JSON 格式化/压缩、键排序、重复键检查 |
| `mootool_json_query` | JSONPath 查询；禁用表达式执行 |
| `mootool_encode` | Base64、URL、十六进制、Unicode 编解码；URL 支持 UTF-8 / GB2312 |
| `mootool_timestamp` | 时间戳与本地时间转换；指定时区和秒/毫秒单位 |
| `mootool_diff` | 文本比较、统一 diff 和行数统计 |
| `mootool_hash` | UTF-8 文本摘要：MD5、SHA-1、SHA-256/384/512 |
| `mootool_uuid` | 生成 1–100 个 UUID v4 |
| `mootool_notes_search` / `mootool_notes_read` | 搜索、分页读取授权的随手记库 |
| `mootool_json_documents_search` / `mootool_json_documents_read` | 搜索、分页读取授权的 JSON 文档库 |

例如：「用 MooTool 把这段 JSON 按键排序」「用 MooTool 将这个时间戳转成上海时间」。Skill 的调用入口为 Codex 的 `$mootool` 或 Claude Code 的 `/mootool`。

前七个工具仅处理传入的参数，不读取文档库。文档工具默认关闭，需要在「设置 → AI 接入」分别开启「允许读取随手记」和「允许读取 JSON 文档」，页面会显示授权的实际目录。工具不访问应用数据库、剪贴板或凭证，不执行 Shell、发送网络请求或保存调用历史。普通文本上限 100,000 字符，diff 单侧上限 8,000 字符，结果上限 1 MB。工具错误返回 MCP `isError`，不会伪装为成功。MD5/SHA-1 仅用于兼容已有摘要格式。

## 文档只读访问

- 搜索可按标题、相对路径、内容匹配；返回相对路径、标题、摘要和修改时间。读取随手记时解析 YAML 元数据，只返回正文；JSON 文档返回原文，不强制修改格式。
- 使用搜索返回的相对路径读取文档。先搜索，再分页读取；`nextOffset` 表示下一页偏移，`truncated` 表示搜索未覆盖所有候选项，应缩小查询。
- 搜索每页最多 50 条，读取每页最多 50,000 字符；单文件最多 2 MB，单次搜索最多检查 4,000 个目录项、读取约 10 MB 内容，目录深度最多 24。
- 排除隐藏路径、符号链接以及文档库根目录 `.gitignore` 忽略的文件。禁止绝对路径和目录穿越。随手记支持 `.txt/.md/.json/.java/.js/.ts/.py/.xml/.yaml/.yml/.sql`，JSON 库仅支持 `.json`。
- 只读层不会创建目录、欢迎笔记或索引。授权目录记录为实际路径，每次调用重新检查权限；关闭开关在下一次调用生效。修改文档库位置会撤销两个库的授权，需要重新开启。
- 这些开关控制所有使用此 MooTool 运行时的本地 AI 客户端。卸载某一客户端接入不改变其他客户端的文档访问授权。

## 运行与安装行为

- MCP 使用官方 TypeScript SDK 的 stdio 传输。独立 `out/main/mcp.js` 入口复用 MooTool 工具函数，不初始化桌面窗口和数据仓库。
- 通过 [Electron 的 `ELECTRON_RUN_AS_NODE`](https://www.electronjs.org/docs/latest/api/environment-variables) 使用自带运行时，无需另装 Node.js。构建时必须保留 `runAsNode` fuse。
- 支持固定位置的 macOS `.app`、Windows 安装版、Linux deb，以及开发构建。请先把 macOS 应用移入 Applications。App Translocation、临时挂载的 AppImage 和解压到临时目录的 Windows portable 会拒绝安装，以免写入下次启动失效的路径。
- 页面显示未安装、已安装、需要修复或冲突。移动/升级 MooTool 或缺失 Skill 文件后，打开新位置应用，点击「一键修复 / 更新」，安装记录用于辨认旧配置并更新路径。用户改写过的同名配置或 Skill 文件会显示冲突，请先保存或移走自定义内容；安装器不会覆盖。重新排版过的托管 TOML 区块也会保守拒绝更新。
- 预览仅展示新生成的内容，不向界面传送其他配置或凭证。写入前复查文件是否变化；已有文件先保存为同目录的 `.mootool-backup-<id>`，再以临时文件替换。多文件安装失败会尝试回滚本次已写文件，保留备份；并发变更不会被回滚覆盖。
- 重复安装相同配置不会追加重复项或生成多余备份。预览有效期 10 分钟，过期或源文件变化时刷新预览即可。
- 点击「卸载所选接入」只移除所选客户端与方式中由安装器管理的 `mootool` MCP 项及 `SKILL.md`、`runtime.md`，保留目录内其他文件。即使旧运行时失效也能卸载。需要恢复原配置时，可使用页面列出的备份；恢复前检查安装后是否还有其他修改。
- 安装记录和文档授权保存在 MooTool 用户数据目录的 `ai-integration/` 中，默认以仅当前用户可读写的权限保存。客户端配置只包含启动路径和必要环境变量。

## Skill 独立使用

Skill 安装包含 `SKILL.md` 与按安装路径生成的 `runtime.md`。没有 MCP 连接时，Agent 读取后者，执行 `--list` 获取实时参数 Schema，再把 JSON 参数传入 `--call TOOL_NAME` 的标准输入。命令返回 MCP 结果 JSON；失败退出码为非零。macOS/Linux 的路径按 POSIX Shell 转义，Windows 提供 PowerShell 命令和 UTF-8 编码设置，输出管道确保等待 GUI 子系统进程完成，并显式检查退出码，参见 [PowerShell 进程等待行为](https://devblogs.microsoft.com/powershell/managing-processes-in-powershell/)。

## 开发验证

```sh
npm run build
npm run test
npx playwright test tests/electron/ai-integration.spec.ts
node out/main/mcp.js --list
# After packaging: verifies the actual ASAR entry on the current OS
node scripts/test-packaged-ai.mjs
```

设置 `MOOTOOL_TEST_EXECUTABLE` 为打包后的应用可执行文件绝对路径，可对同一个安装流程执行打包版 E2E，包含 ASAR 内 MCP 入口的启动验证。

协议测试覆盖初始化、发现、调用和错误；安装测试覆盖 TOML/JSONC 保留、幂等、备份、冲突、预览失效、失败回滚、移动路径修复和卸载。文档测试覆盖分页、元数据、权限撤销及路径限制。Electron E2E 使用临时用户目录，先检查安装目标，再执行真实一键安装、连接测试、UTF-8 Skill 命令、授权/撤销、修复与卸载；不会修改开发者的实际客户端配置。

设置 `MOOTOOL_CODEX_EXECUTABLE` 为已登录的 Codex CLI 绝对路径，可额外运行真实 `codex exec` 验收。它保留当前认证，仅注入安装器生成的 MooTool MCP 配置，使用临时示例文档，核对实际 `mcp_tool_call` 完成事件，并保存 `codex-mcp-calls.json` 作为证据。默认 CI 不调用收费模型；四个平台打包任务均执行 SDK 与打包版界面验收，Windows 还执行生成的 PowerShell 命令。
