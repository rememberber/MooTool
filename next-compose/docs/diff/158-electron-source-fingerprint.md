# DIFF-158：Electron `sourceFingerprint` 对齐

## 背景

Electron legacy 迁移用 `sourceFingerprint(root, [database, config, quickNoteVault, jsonVault])` 写入 `t_next_migration_run.fingerprint`。compose 此前仅用自有内容摘要 `fingerprint` 与 `source_path` 匹配，无法在路径搬迁或仅指纹一致时识别 Electron 已迁完的数据包。

## 变更

- `LegacyJavaDataPaths`：与 Electron `resolveLegacySource` 相同的路径解析（含 `~`、相对 `dbFilePath`、Vault 默认名）。
- `LegacySourceFingerprint`：递归目录指纹（相对路径 + size + mtimeMs，跳过 `.git` 与符号链接）。
- `ImportPreview.legacyElectronFingerprint`；`readElectronMigrationRunComplete` 同时匹配 `source_path` 与 `fingerprint` 列。
- `rememberImport` 与「已迁移」判断同时记录/识别 Electron 指纹。

## 测试

- `LegacySourceFingerprintTest`：同目录两次指纹一致；`inspect` 暴露 `legacyElectronFingerprint`。
- Node `legacyMigrationService` 对照需在本机用 `next/electron/main/legacyMigrationService.ts` 同结构抽样（mtime 字符串与 `/var`→`/private/var` realpath 敏感）。

## 未覆盖

- compose 内容 `fingerprint` 仍用于行级/整包导入幂等，不与 Electron 指纹合并为单一字段。
