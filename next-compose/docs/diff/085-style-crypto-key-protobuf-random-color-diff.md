# DIFF-085：加密密钥网、随机行、Protobuf Wire、色板分栏、对比编辑、图片选项

- 编号：DIFF-085
- 影响：F14 非对称/随机；F07 Protobuf Wire/转换；F22 色板；F02 文本对比；F23 图片选项
- 日期：2026-09-15

## 原行为（Electron）

- `.crypto-key-grid` / `.crypto-data-grid`：两列 12 缝；`.crypto-action-row` 居中换行 7 缝
- `.random-row`：124 | 输出 34 高等宽 | 32 复制 | 84 生成，底部分隔
- `.protobuf-wire-layout`：两侧 + 132 中栏
- `.color-board-layout`：0.34/0.66、预览 ≥190、色码 136
- `.diff-editor-grid`：中缝 `border-soft`，标题 11/600
- `.image-options`：16 缝、11 字号

## 本产品行为

- 非对称密钥/明文两列；动作 `FlowRow` 居中 7dp
- 随机行对齐 124/34/32/84；长度紧凑 100
- Protobuf Wire/转换中栏 132，标签 11 SemiBold；消息名紧凑
- 色板 0.34/0.66 分栏、预览底对齐 190、色码 136 compact
- 对比左右中缝、11 SemiBold 标题；状态 11 muted
- 压缩/水印选项 16 缝、11 字号、颜色 compact

Hex/Base64、忽略空白、高亮模式、主题与「更多」仍用 `primary = expr`。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

file-drop-row 完整拖放条、protobuf 定义区 30 高 select 全覆盖仍未抽组件。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
