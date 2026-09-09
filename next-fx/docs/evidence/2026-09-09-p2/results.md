# 结果

`./mvnw test` exit 0。新增 `JsonVaultStoreTest`（读写、拒绝 `..` 逃逸、文件夹删除）与草稿表往返。

P2 交付：本产品 `data/vaults/json` 本地 Vault（新建/打开/保存/刷新）、格式化历史双击恢复、草稿 600ms 防抖写入 SQLite、`Cmd/Ctrl+S` 保存当前 Vault 文件、分离窗口后快捷键随 Scene 转移。Git 仍明确未实现。
