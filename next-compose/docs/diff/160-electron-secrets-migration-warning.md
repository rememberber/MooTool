# DIFF-160：Electron `secrets` 密文迁移提示

## 背景

Electron 将 `proxyPassword` / `gitToken` 存入 `mootool-next.json` 根级 `secrets`（`safeStorage` 密文）。compose 无法调用 OS 密钥链解密，此前仅清空 settings 内明文，未在预览中提示用户 store 里仍有密文条目。

## 变更

- `ElectronNextSettingsImport.hasEncryptedSecretsInStore`：检测 `secrets` 对象内非空值。
- `CrossProductImporter.inspect` 发出 `electron:secretsSkipped`；迁移面板复用 `settings.migration.warning.secretsSkipped` 文案。
- JSONPath 弹层双击路径与「使用」一致，写入 `json.notice.pathApplied`。

## 测试

- `ElectronNextSettingsImportTest.detectsEncryptedSecretsBlobInElectronStore` / `emptySecretsObjectDoesNotWarn`。

## 未覆盖

- 跨平台解密 Electron 密文并写入 compose 设置（需 OS 级密钥链，不在 JVM 桌面产品范围）。
