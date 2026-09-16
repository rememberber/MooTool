# 数据迁移（原生 0.8+）

设置 → **数据迁移** 与 **设置 → 侧边栏 / 关于** 中的相关项，用于从 next Electron 或 Java 版合并数据到 `next-macos-native` 独立工作区（`~/Library/Application Support/com.rememberber.mootool.next.macos-native/`）。

## Electron `mootool-next.json`

- 侧栏：自定义分组、隐藏工具、分栏宽度（`layoutPaneSizes` 映射）、显示最近使用、仅图标导航等。
- 偏好：HTTP 代理、编辑器字号/换行、文档库 Git 自动检查点/Pull 间隔、菜单栏托盘（`general.trayEnabled`）、关闭主窗口行为（`general.closeBehavior`）、自动下载更新偏好（`general.autoDownloadUpdates`，原生版仍须手动安装）。
- 磁盘文档库：Electron 根据 `mootool-next.json` 的 `settings.vault` 路径（默认 `quick-notes`、`json-vault`）；Java 版根据 `~/.MooTool/config/config.setting`（默认 `quick-notes`、`json-beauty`）扫描并合并 UTF-8 文本；随手记正文中的 `attachments/*` 会登记为原生附件。
- 不导入：`safeStorage` 加密的代理密码等密钥字段。

## SQLite（`MooToolNext.db` 或 `~/.MooTool/MooTool.db`）

| 表 | 合并目标 |
| --- | --- |
| `t_msg_http` | HTTP 请求集合 |
| `t_http_request_history` | 全局历史（HTTP） |
| `t_host` | Host 配置列表 |
| `t_translation_word` / `t_translation_history` | 翻译词库 / 历史 |
| `t_func_history` | 各工具历史记录 |
| `t_func_content` | 工具编辑区草稿（正则/JSON/文本对比/时间/计算器/二维码/代码运行等） |
| `t_quick_note` / `t_json_beauty` | 随手记 / JSON 文档库正文 |
| `t_next_favorite` 或 Java `t_favorite_*` | 颜色/正则/Cron 收藏 |

重复项策略：HTTP/Host/收藏等按业务键跳过；文档正文按工具 ID + 标题（含扩展名等价）+ 正文跳过。

## 未覆盖 / 边界

- **Compose/Electron 遗留迁移服务**级一键镜像（全表、全 pane 键、实时双向同步）；原生版已支持 SQLite 合并与 **磁盘目录合并**（Electron `quick-notes` / `json-vault`，Java `quick-notes` / `json-beauty`，含随手记 `attachments/` 引用登记），见设置 → 数据迁移。
- `safeStorage` 加密字段、第三方账号令牌、安装包后台下载与静默安装。
- 各工具面板**完整**多语言（侧栏/设置导航与主操作已部分三语；表单说明与错误文案仍以中文为主）。

详见 [功能对齐清单](parity.md) 工作台段。
