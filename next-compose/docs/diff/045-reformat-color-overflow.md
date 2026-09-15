# DIFF-045：格式化类型/缩进下拉、1080 溢出与调色板复制反馈

- 编号：DIFF-045
- 影响：F03 格式化、F22 调色板、ui-spec 1080 工具栏密度、复制短暂反馈
- 日期：2026-09-15

## 原行为（Electron）

格式化用 compact `<select>` 选类型和缩进，不把 Nginx/Java/XML/HTML 和 2–6 做成一排按钮。复制成功有 toast；JSON 另有按钮文案切换。调色板工具栏在窄宽度下动作过密。

## 本产品行为

- 格式化类型、缩进改为下拉，与 Electron 字段关系一致。
- 内容宽 < 1440 时格式化页保留 Tab/类型/缩进/格式化，复制/保存/清空收入「更多」；复制按钮短暂显示「已复制」或失败文案。
- 调色板在 < 1440 时保留取色/格式/色值/应用，复制/收藏/收藏夹/历史收入「更多」，复制同样有 1400ms 按钮反馈。

## 理由

Compose 原先把 4 种类型和 5 档缩进全部铺成按钮，1080 宽无法对照 Electron 的 compact 字段。复制反馈是 ui-spec 通用要求。

## 证据

`CopyFeedbackPolicyTest`（含自定义 idle 文案键）。`desktopTest` **231/231**，见 `docs/evidence/2026-09-15-reformat-color-overflow/`。真实窗口截图、IME 手工、三平台安装仍未测。

## 受影响范围

- 类型/缩进仍覆盖 Nginx/Java/XML/HTML 与 2–6，只是交互从按钮组改为下拉。
- 仍非 Electron 六套 CSS 逐选择器移植。
