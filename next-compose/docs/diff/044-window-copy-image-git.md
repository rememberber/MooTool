# DIFF-044：窗口夹回可见范围、JSON 复制反馈、图片 1080 溢出与 Git 提交文件

- 编号：DIFF-044
- 影响：A03 窗口恢复、F04 JSON 复制、F23 图片工具栏、F01/F04 Vault Git 提交差异
- 日期：2026-09-15

## 原行为（Electron）

恢复窗口时 Chromium 会尽量把窗口留在可用屏幕。JSON 复制成功把按钮改成「已复制」，失败改成失败文案，约 1400ms 后回到「复制」。图片助手工具栏在 1080 宽时动作过密。Git 提交差异在多个文件时用下拉选择，而不是只看第一个文件。

## 本产品行为

- 启动时按各显示器工作区夹紧保存的窗口坐标：标题条至少 48px 落在某个工作区内；完全越界时夹到最近屏，而不是 silently 居中。
- JSON 复制按钮短暂显示「已复制」或「复制失败」，1400ms 后恢复；剪贴板失败仍写 notice。
- 内容宽 < 1440 时图片页保留列表开关/截图/导入/保存/复制（及进行中的取消），剪贴板/Base64/SVG/压缩/水印/历史/分离收入「更多」。
- Vault Git 提交差异加载该提交全部文件，多于一个时用下拉切换；标签格式与 Electron 相同（`status  old → new`）。

## 理由

ui-spec 要求更换显示器后把可操作标题区夹回可见范围，以及复制后短暂反馈。1080 图片工具栏原先水平堆满。DIFF-043 已注明提交预览只展示第一个文件，这一刀补齐多文件选择。

## 证据

`WindowBoundsPolicyTest`、`CopyFeedbackPolicyTest`、`GitDiffSelectionTest`、`GitEngineTest.commitDiffListsAllChangedFiles`。`desktopTest` **231/231**，见 `docs/evidence/2026-09-15-window-copy-image-git/`。真实多显示器拖窗、复制按钮窗口观感、IME 手工、三平台安装仍未测。

## 受影响范围

- 无屏幕信息（headless）时保留已保存坐标，不擅自居中。
- 复制反馈不写入会话快照。
- 仍非 Electron 六套 CSS 逐选择器移植。
