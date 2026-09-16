# DIFF-393：设置数据目录与运行时有效 `dataRoot`

## 背景

Electron 设置页可编辑 `settings.data.directory`，变更后重开数据仓库并撤销 AI Vault 授权。Compose 此前仅展示固定 `directories.dataRoot`，`data.directory` 写入设置但不生效。

## 行为

- `DataPathConfig`：空串保留产品默认 `dataRoot`；非空须绝对路径（加载时清除非法值）。
- `AppContainer.dataDirectories()`：Vault、Host/HTTP/图片等文件存储、备份导出/恢复均走有效 `dataRoot`。
- `AppDatabase.rebindDataDirectories`：`data.directory` 变更时切换 SQLite 文件并迁移。
- 设置「数据」分类：`DirectorySettingRow` 编辑 `data.directory`，「打开数据目录」指向有效根。

## 测试

- `DataPathConfigTest`
- 既有 `VaultPathAiAccessTest.changingDataDirectoryRevokesAiDataAccess`

## 验收

- A01 设置/数据；F01/F04 默认 Vault 路径；`desktopTest --offline`。
