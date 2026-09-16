# DIFF-084：加密 IO、编码中栏、网络分区、二维码标签、勾选行

- 编号：DIFF-084
- 影响：F14 加解密；F13 编码解码；F11 网络；F17 二维码；F23 图片水印勾选；F25 序列号勾选
- 日期：2026-09-15

## 原行为（Electron）

- `.crypto-io-grid`：两侧编辑器 + 132 中栏；`.crypto-textarea > span` 11/600
- `.digest-output`：标签 | 结果 | 24 复制
- `.encode` 与加密同款三栏
- `.net-workspace`：左输出 46 高工具条、11 等宽；右 `.net-section` 11/600 标题、9 字号标签与提示
- `.checkbox-row label`：10 字号 muted、6 间距

## 本产品行为

- 加密/编码中栏 132dp；字段标签 11 SemiBold muted；摘要结果 24dp 复制
- 网络去掉输出卡片壳，改为 toolbar 头 + `borderSoft` 中缝；分区改为底部分隔而非卡片
- 二维码内容/结果标签 11；尺寸紧凑字段
- 图片对角线、硬件序列号勾选 10sp muted

## 理由

DIFF-083 覆盖表/预设后，这批是剩余工具页密度缺口。

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

crypto-key-grid 两列密钥区、protobuf-wire-layout、完整 checkbox-row 组件抽取仍未做。系统 IME、托盘 TCC、产品窗 Tab 焦点帧、三平台安装仍未测。
