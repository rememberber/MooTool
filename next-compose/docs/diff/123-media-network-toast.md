# DIFF-123：QR/PDF/翻译/时间/HTTP/加密/图片 toast

对照 Electron 各 `*Tool.tsx` 的 `actions.toast.success` / `toast.success`。

## 范围

- **F17 二维码**：生成、识别、保存 PNG。
- **F23 PDF**：拆分/合并完成。
- **F20 翻译**：存单词、单词本保存。
- **F18 时间**：双向转换成功（沿用 `time.notice.*` 文案）。
- **F09 HTTP**：保存集合。
- **F14 加密**：非对称密钥生成。
- **F24 图片**：保存、导入、批处理、矢量化、导出目录。

## 文件

- `QrCodeScreen.kt`、`PdfScreen.kt`、`TranslationScreen.kt`、`TimeConvertScreen.kt`、`HttpScreen.kt`、`CryptoScreen.kt`、`ImageScreen.kt`
