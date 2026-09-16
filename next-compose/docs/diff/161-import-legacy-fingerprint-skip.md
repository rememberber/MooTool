# DIFF-161：`apply` 识别已记录的 Electron 源指纹

## 背景

DIFF-158 在 `rememberImport` 与迁移预览「已迁移」判断中同时写入/识别 `legacyElectronFingerprint`，但 `CrossProductImporter.apply` 仍只比对 compose 内容 `fingerprint`。源数据微变导致 compose 指纹变化时，可能重复导入数据块。

## 变更

- `CrossProductImporter.importAlreadyRecorded`：`fingerprint` 或 `legacyElectronFingerprint` 命中 `fingerprints.txt` 即跳过 `apply` 数据阶段（与面板 `alreadyMigrated` 一致）。

## 测试

- `CrossProductImporterFingerprintTest.applySkipsWhenLegacyElectronFingerprintAlreadyRecorded`
