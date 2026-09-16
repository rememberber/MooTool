# DIFF-189：二维码保存目录迁入与保存对话框

## 背景

Java `func.qrCode.qrCodeSaveAsPath` 记录上次导出目录，并写入 `config.setting`。Compose 使用全局 `tools.exportDirectory`（与随手记/JSON/图片/Host 导出路径合并，见 DIFF-142）。

## 行为

- **迁移**：`qrCodeSaveAsPath` 加入 `applyPatch` 导出目录候选链（绝对路径时写入 `tools.exportDirectory`）。
- **F23 运行时**：生成 Tab「保存」打开系统保存对话框时，以 `VaultPathConfig.effectiveCustomRoot(exportDirectory)` 作为初始目录；保存成功后把父目录写回 `tools.exportDirectory`（对齐 Java 保存后更新 `qrCodeSaveAsPath`）。

## 证据

- `LegacyJavaSettingsTest.applyPatchUsesQrSaveAsExportDirectory`
- `./gradlew :composeApp:desktopTest --offline`
