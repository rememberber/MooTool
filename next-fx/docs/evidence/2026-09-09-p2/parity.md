# 对照

| 项 | 源 | FX | 状态 |
| --- | --- | --- | --- |
| Vault 根 | 用户可选目录 | 本产品 `data/vaults/json` | 简单 Vault 通过；自定义路径待设置 data 页 |
| 路径逃逸 | realpath/allowlist | 拒绝 `..` 与根外路径 | 通过（测试） |
| 历史 | 工具历史 | SQLite 200 条，检查器双击恢复输出 | 核心通过 |
| 重启恢复 | 会话草稿 | `tool_draft` JSON 正文/选项/Vault 路径 | 核心通过（单元+草稿存储） |
| Git | VaultGit | 文案标明 P6 | 延期 |
