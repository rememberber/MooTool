# DIFF-100：HTTP / 时间 P5 操作按钮

- 编号：DIFF-100
- 影响：F09 HTTP；F18 时间
- 日期：2026-09-15

## 原行为（Electron）

- HTTP 请求条 Method/发送/Body 控制/响应查找复制另存、集合脚栏 cURL 等为 P5 `panel-command`（约 30px）
- 时间转换区时区/快捷区/转换/复制/时钟等为同类密度

## 本产品行为

- HTTP：Method、发送/停止、Body 类型与格式化、响应查找/复制/另存/更多、集合 import/copy/save/delete 均 `p5Toolbar`
- 时间：时区选择、快捷时区、单位、双向转换、各复制钮、当前带时钟钮 `p5Toolbar`

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

产品窗 Tab 焦点帧、系统 IME、三平台安装仍未测。
