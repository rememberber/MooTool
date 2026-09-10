# DIFF-009：二维码历史不保存 PNG 本体

- 编号：DIFF-009
- 影响：F17 二维码
- 日期：2026-09-09

## 原行为（Electron）

生成历史的 `outputText` 为完整 `data:image/png;base64,...`，恢复时可直接显示上次 PNG。生成用 `qrcode` npm，识别用 `@zxing/browser`。

## 本产品行为

ZXing 3.5.4 生成/识别 PNG 字节。历史只保存内容、尺寸、纠错级别与识别文本，不把 PNG 写入 SQLite；从历史恢复生成项后需再点生成。识别失败先 TRY_HARDER，再 PURE_BARCODE。剪贴板走 AWT `imageFlavor`。

## 理由

规格要求数据库不当无限文件仓库；二维码 PNG 可达数 MB，存 Base64 会撑爆历史表。编解码闭环用同一引擎的 PNG 往返验证。

## 证据

`QrEngineTest`：L/M/Q/H 往返、中文 URL、Logo 叠加后仍可识别、尺寸 120–2000、空内容/坏图/空白图报错。

## 受影响范围

- 历史恢复不带回预览图与剪贴板里的原图。
- 与 Electron `qrcode` 像素网格可能不完全一致，但仍应能互相识别普通二维码。
- 本机未测多屏 DPI 下剪贴板往返的全部窗口。
