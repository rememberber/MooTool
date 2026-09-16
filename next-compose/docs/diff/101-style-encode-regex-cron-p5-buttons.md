# DIFF-101：编码 / 正则 / Cron P5 操作按钮

- 编号：DIFF-101
- 影响：F13 编码解码；F15 正则；F16 Cron
- 日期：2026-09-15

## 原行为（Electron）

- 编码中栏正向/反向、URL 字符集与 ASCII 进制切换为 P5 `panel-command`（约 30px）
- 正则测试/取消/复制匹配为同类密度
- Cron 解析、复制运行结果、预设芯片、时区选择为同类密度

## 本产品行为

- `EncodeScreen`：正向/反向、UTF-8/GB2312、十进制/十六进制 ASCII 切换均 `p5Toolbar`
- `RegexScreen`：测试/取消/复制匹配均 `p5Toolbar`（`prominent` 仍保留在测试钮）
- `CronScreen`：解析、复制、各预设、时区钮均 `p5Toolbar`

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

收藏弹层内按钮仍为默认密度；产品窗 Tab 焦点帧、系统 IME、三平台安装仍未测。
