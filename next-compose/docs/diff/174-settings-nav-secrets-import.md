# DIFF-174：设置导航批量显隐与 Electron 明文密钥迁入

## 背景

Electron 设置「导航中的工具」提供 **全部显示 / 全部隐藏** 与说明文案；`mootool-next.json` 在无 `secrets` 密文时，`settings.network.proxyPassword` 与 `settings.vault.gitToken` 为明文，应迁入 compose。

## 行为

- **A01**：布局设置中增加 `settings.nav.showAll` / `hideAll` 与 `toolsDescription`；工具显隐按侧栏分组展示并带 `ToolIcon`（见 DIFF-175）。
- **A03**：`loadFromStore` / `mergeInto(..., retainSecrets)` 在无根级 `secrets` 密文时保留代理密码与 Git 令牌；存在密文仍清空并保留 `secretsSkipped` 警告。

## 验证

- `ElectronNextSettingsImportTest.loadsAndMergesElectronStoreWithoutSecrets` / `stripsSecretsWhenEncryptedBlobPresent`
- `./gradlew :composeApp:desktopTest --offline`
