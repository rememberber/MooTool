# DIFF-028：Git 远程操作用 CLI + ASKPASS，不把 token 写入 URL

- 编号：DIFF-028
- 影响：F01 随手记、F04 JSON、A01 Vault、A03 Git
- 日期：2026-09-14

## 原行为（Electron）

`vaultGitService.ts` 对 fetch/pull/push 使用 `GIT_ASKPASS` 与 `MOOTOOL_GIT_TOKEN`。discard 对未跟踪文件 `clean -f`，对已跟踪文件 `restore`/`checkout`。冲突 `checkout --ours/--theirs` 后 `add`。自动检查点在有 remote 且 ahead/有变更时还会 push。

## 本产品行为

`GitEngine` 增加 fetch/pull/push/discard/abortMerge/resolveConflict/continueOperation。HTTPS token 写入临时 ASKPASS 脚本与 `MOOTOOL_COMPOSE_GIT_TOKEN`，**不**嵌入 origin URL，也**不** `git config --global`。远程操作超时 120s。本轮仍无自动提交/自动 pull 定时器。

本地 `file://` 裸仓库单测覆盖 discard 与 push/pull，不访问外网。

## 理由

规格要求凭据不进 remote URL。与 Electron 相同走系统 Git CLI，便于用户用已有凭据助手；Compose 侧用本产品环境变量名避免和其他产品 ASKPASS 冲突。

## 证据

`GitEngineTest`：既有 porcelain/init，以及 discard + 本地 `file://` 裸仓库 push/pull。`desktopTest` **149/149**。窗口内 Git 对话框未手工验收。

## 受影响范围

- 与 Electron 比：不写 local `user.name`；邮箱仍为 `next-compose@local`。
- 无 token 的 HTTPS 远程会因 `GIT_TERMINAL_PROMPT=0` 失败并显示 Git 输出，不会假装成功。
