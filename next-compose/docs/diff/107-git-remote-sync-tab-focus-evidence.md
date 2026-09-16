# DIFF-107：Vault Git 远程删除/同步与 Tab 焦点证据帧

- 编号：DIFF-107
- 影响：F01/F04 Vault Git；F09 HTTP；F20 翻译；A03 Git
- 日期：2026-09-15

## 原行为（Electron `VaultGitDialog.tsx`）

- 配置 remote 时：有 URL 显示「保存」，清空输入且已有 origin 时显示「删除远程地址」并 `configure-remote` 空串
- 已配置 remote 时展示 `↑ahead ↓behind` 同步计数

## 本产品行为

- `VaultGitDialog`：展示 `git.sync`；remote 操作钮随输入在 `git.saveRemote` / `git.removeRemote` 间切换（空 remote 且未配置 origin 时禁用）
- 删除成功后清空本地 remote 输入并写入设置
- `ToolbarFocusCaptureTest` 新增 HTTP「发送」、翻译「翻译」`p5Toolbar`+`prominent` 焦点环帧 `129`/`130`（Compose 场景，非产品主窗）

## 证据

`desktopTest` 见 `docs/acceptance.md`；帧见 `docs/evidence/2026-09-15-inspector-screencapture/windows/129-compose-http-send-tab-focus.png`、`130-compose-translation-now-tab-focus.png`。

## 未做

Electron 中止合并二次确认、Git 面板逐选择器 CSS、真实 pull/push 联网验收、三平台安装仍未测。
