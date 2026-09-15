# DIFF-025：Vault Git 使用外部 Git CLI，本轮不含 pull/push

- 编号：DIFF-025
- 影响：F01 随手记、F04 JSON、A01 Vault 设置、A03 Git
- 日期：2026-09-14

## 原行为（Electron）

`vaultGitService.ts` 用系统 Git CLI：`GIT_TERMINAL_PROMPT=0`，按仓库根串行。初始化写 `.gitignore` 并在有变更时提交 `Initial MooTool Vault setup`。提交 `add --all` + 消息（最长 300）。无变更返回成功「No changes to commit」。merge/冲突时拒绝提交。仓库必须是 Vault **根**本身（`rev-parse --show-toplevel` 与 Vault realpath 相同），不能误用父仓库。身份在缺省时写入**该仓库** local `user.name`/`mootool@local`。UI 含 status/diff/commit/history/pull/push/discard/冲突继续中止与自动提交定时器。

## 本产品行为

`GitEngine` 同样封装外部 Git CLI（`ProcessBuilder` argv，不拼 shell）。按规范化仓库根 `synchronized` 串行。检测 `git --version`；缺失时仍可记笔记，错误可见。身份只用命令级 `-c user.name=` / `-c user.email=next-compose@local` / `-c commit.gpgsign=false`，**不** `git config --global`，也**不**写入仓库 local config。Token 不嵌入 remote URL。提交前 flush 当前已打开的 Vault 文件；未命名脏缓冲阻止 Git。随手记与 JSON 共用 Compose Git 对话框：状态、变更列表、初始化、提交、历史、文件 diff、保存 origin。设置可存用户名与 remote。

本轮**不实现** pull/push/fetch/discard/merge abort/冲突 ours-theirs/自动提交定时器；这些入口没有做成假成功按钮。

## 理由

架构锁定 Git CLI 优先、JGit 仅为验证后的替代。命令级 `-c` 避免改用户全局配置，也避免在 Vault 里留下本产品写入的 local identity。pull/push 需要凭据与冲突工作流，单独切片才能真实验收。

## 证据

`GitEngineTest`：NUL porcelain/分支解析不联网；临时目录真实 `git` 初始化→改文件→提交→history→set-url；父仓库不被当成 Vault 根。本机 Git 2.45.1，3 项集成用例均执行（0 skipped）。`desktopTest` **139/139**。无运行截图；窗口内 Git 对话框未手工验收。

## 受影响范围

- 与 Electron 比：不写 local `user.name`；邮箱为 `next-compose@local` 而非 `mootool@local`。
- 未 fetch 时 ahead/behind 通常为 0。
- 无 Git 时对话框说明可见，文档库编辑不受阻。
- 完整 JSON 检查器弹层仍未做。更新通道见 [DIFF-027](027-update-channel-open-installer.md)。
