# DIFF-102：加解密各 Tab P5 操作按钮

- 编号：DIFF-102
- 影响：F14 加解密/随机
- 日期：2026-09-15

## 原行为（Electron）

- 对称/摘要/Base64 中栏加解密与复制、非对称操作条、摘要文本/文件、随机行「生成」为 P5 `panel-command`（约 30px）
- 算法紧凑下拉为同类密度

## 本产品行为

- 对称/Base 中栏三钮、非对称生成密钥与 FlowRow 全部操作、摘要文本/文件、随机各行生成、各 Tab `CompactChoice` 触发钮均 `p5Toolbar`
- 摘要/随机结果区仍用 `MooGhostButton` 复制图标

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

工具标题栏历史/分离/更多仍为默认密度；file-drop、系统 IME、三平台安装仍未测。
