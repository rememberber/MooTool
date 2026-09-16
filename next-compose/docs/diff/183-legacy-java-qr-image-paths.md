# DIFF-183：Java 二维码 Logo/识别图路径迁入

## 行为

- **A03 / F19**：迁移确认后，若 Java `func.qrCode.qrCodeLogoPath` / `qrCodeRecognitionImagePath` 指向本机仍存在的文件，则写入 `QrSession.logoPath`/`logoName`/`logoImage` 与 `recognitionName`/`recognitionBytes`（路径无效则跳过，不伪造文件）。

## 验证

- `LegacyJavaSettingsTest.applySessionPatchesRestoresQrLogoAndRecognitionPathsWhenFilesExist`
- `./gradlew :composeApp:desktopTest --offline`（**340/340**）
