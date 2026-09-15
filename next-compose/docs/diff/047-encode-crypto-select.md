# DIFF-047：编码/加解密 compact 下拉与 1080 溢出

- 编号：DIFF-047
- 影响：F13 编码解码、F14 加解密、ui-spec 1080 工具栏密度
- 日期：2026-09-15

## 原行为（Electron）

编码 URL charset、ASCII 进制和加解密各 Tab 算法都是 compact `<select>`，不把 UTF-8/GB2312 或 AES/DES/SM4 等铺成按钮。历史/清空在 Tab 头图标区，窄宽不占一整排文字按钮。

## 本产品行为

- URL 字符集、ASCII 十进制/十六进制改为下拉，能力不变。
- 对称/非对称/摘要/Base 算法改为下拉，选项与原先 enum 一致。
- 内容宽 < 1440 时编码页历史/清空/分离、加解密历史/分离收入「更多」。

## 理由

与 DIFF-045 同一类密度缺口：Electron 用 select，Compose 原先把选项铺成按钮，1080 宽无法对照。

## 证据

引擎单测覆盖既有转换/加解密语义，本切片不改算法。`desktopTest` **232/232**，见 `docs/evidence/2026-09-15-encode-crypto-select/`。真实窗口截图未取。

## 受影响范围

- 算法集合未增删；只改交互控件。
- 仍非 Electron 六套 CSS 逐选择器移植。
