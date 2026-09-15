# DIFF-039：JSON 检查器结果区、独立随手记树展开与文档库方向键

- 编号：DIFF-039
- 影响：F04 JSON 检查器、F01 文档库树、A01 Vault 设置、ui-spec 树键盘
- 日期：2026-09-15

## 原行为（Electron）

JSON 检查器含格式/转换/JSONPath 以及底部「结果」状态。Vault 工具栏有展开全部/折叠全部，随手记与 JSON 各自写入 `quickNoteTreeExpandMode` / `jsonTreeExpandMode`。规格要求复杂树可用方向键展开/折叠。

## 本产品行为

- 检查器增加缩进标签与结果区：展示 `notice` 或校验 `status`（错误用危险色）。
- 设置拆出随手记树展开；侧栏按钮切换 expandAll/collapseAll 并写入对应设置。
- 文档库获得焦点后 ↑↓ 移动、← 折叠或回到父级、→ 展开或进入子项、Enter 打开文件。

## 理由

目标点名 JSON 完整检查器与导航/设置未实现类。随手记此前误用 JSON 展开设置；检查器缺结果区；树只能鼠标点。

## 证据

`VaultTreeKeyTest`。`desktopTest` **206/206**，见 `docs/evidence/2026-09-15-inspector-tree-keys/`。窗口手势、IME 手工、三平台安装仍未测。

## 受影响范围

- 方向键在树未获焦点时不拦截编辑器。
- 仍非 Electron 六套 CSS 逐选择器移植。
