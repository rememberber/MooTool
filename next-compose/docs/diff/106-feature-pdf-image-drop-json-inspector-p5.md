# DIFF-106：PDF/图片拖放导入与 JSON 检查器 P5 按钮

- 编号：DIFF-106
- 影响：F04 JSON 检查器；F23 图片；F24 PDF；延续 [DIFF-105](105-feature-file-drop-drag.md)
- 日期：2026-09-15

## 原行为（Electron）

- PDF/图片通过选择文件导入；检查器区 `inspector-action` 为 P5 密度（约 30px）

## 本产品行为

- **PDF**：拆分/合并表区域支持多文件拖放，走 `ingestPdfFiles`（与「添加」相同校验与上限）
- **图片**：主工作区 Row 支持多图拖放，走 `importPickedFiles`（与「导入」相同库逻辑）
- **JSON 检查器**：关闭、格式化应用、JSONPath 查询/选取、转换九动作网格均 `p5Toolbar`

## 证据

`desktopTest` 见 `docs/acceptance.md`；拖放手势需产品窗手工验收。

## 未做

Vault 树/编辑器区拖放、检查器与 Electron 逐选择器 CSS、系统 IME、三平台安装仍未测。
