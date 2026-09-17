# 0.8.0 验收记录

产品：`next-macos-native` · 版本 **0.8.0**

## 自动化

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `./scripts/check-core.sh` | 59 组，0 失败 | 含 SQLite 导入（HTTP/Host/翻译/收藏/历史/草稿/`t_quick_note`/`t_json_beauty`）；**2026-09-17** 工作树终验复跑通过 |
| `./scripts/smoke.sh` | 通过（偶发需复跑） | 附件、随手记、JSON、格式化、文档库选区/滚动、86 张截图、重启持久化；**2026-09-17** 两批 i18n 后均单次全绿（约 6.1 min / 8.6 min） |

最近一次 smoke 全绿：**2026-09-17** — 调色板 / Cron describe / JSON worker / 计算器标签等批次后单次通过（约 6.1 min）；同日 Markdown 预览 / 托盘·附件批次（约 8.6 min）亦全绿。

## 范围说明

- **0.8.0** 重点：文本对比工作区、工作台分栏/迁移、加密/Host/翻译/收藏、SQLite 历史与 `t_func_content` 草稿、菜单栏托盘、**关闭主窗口**（`general.closeBehavior` 询问/隐藏/退出）。
- 仍按 [parity.md](parity.md) 列为边界：工具面板全文 i18n、可编辑全局快捷键、安装包后台下载/静默安装、Compose 级遗留迁移一键镜像、Monaco 级编辑装饰等（Java/Electron **磁盘目录**合并导入已支持）。
- 侧栏/搜索/工具名三语、`general.language`、GitHub API 更新检查、Electron 磁盘文档库导入合入后：**check-core 59 组 0 失败**。
- ToolPage/EditorPane 三语与 `autoDownloadUpdates` 静默打开下载页后：**smoke 全绿**（约 9.4 min，2026-09-16）。
- Java `quick-notes` / `json-beauty` 磁盘导入合入后：**smoke 全绿**（约 8.3 min，2026-09-16）；**check-core** 仍 59/0。
- TextTool / 工作台工具栏 i18n 后：**smoke 全绿**（约 8.1 min，2026-09-16）。
- HTTP 分栏枚举与主控件 i18n 后：**check-core** 59/0（2026-09-16）。
- 应用菜单 / 翻译三标签 / 词库主按钮 i18n 后：**check-core** 59/0；**smoke** 全绿（约 7.3 min，2026-09-16；首轮因 SwiftPM 并发构建触发 JSON 3s 超时失败，单独复跑通过）。
- 首页区块 / 设置分类标题 / 翻译编辑器栏 i18n；smoke 与 JSON 验收 JSON 超时放宽至 10s：**check-core** 59/0（2026-09-16）。
- 设置侧栏/代理主控件、时间转换工具栏 i18n；`data-migration.md` 与磁盘合并边界对齐：**check-core** 59/0、**smoke** 全绿（约 9.2 min，2026-09-16）。
- 提交 `99b3e402` / `28c9b33b` / `54775070`：0.8 横切 i18n、设置、Host/备份；**check-core** 复跑 59/0（2026-09-16）。
- 提交 `9d730756` / `3851f1e2`：历史弹窗、托盘菜单、调色板工具栏 i18n；**check-core** 59/0（2026-09-16，偶发 1 失败后复跑通过）。
- 提交 `c22bc88c`（设置快捷键说明 i18n）：**check-core** 59/0；**smoke** 首轮格式化验收失败、复跑全绿（约 8.2 min，2026-09-16）。
- 提交 `ee738d0d`（格式化 smoke 等待加固）后：**smoke** 单次全绿（约 8.7 min，2026-09-16）。
- 提交 `9e6facd2`（数据迁移面板主控件 i18n）：**check-core** 59/0（首轮 JSON worker 3s 超时 2 项，单独复跑通过）；**smoke** 单次全绿（约 8.7 min，2026-09-16）。
- 提交 `831dde36`（时间转换与迁移全文案 i18n）：**check-core** 59/0（偶发 1 失败后复跑通过，2026-09-16）。
- 提交 `3845f21c`（设置说明与调色板 HEX i18n）：**check-core** 59/0（偶发 1 失败后复跑通过，2026-09-16）。
- 提交 `aab23f42`（二维码/图片工具栏 i18n）：**check-core** 59/0；**smoke** 前两轮文本对比/格式化验收失败、第三轮全绿（约 8.9 min，2026-09-16）。
- 提交 `bb07bcc1`（文本对比/格式化 smoke 轮询加固、PDF 工具栏 i18n）：**check-core** 59/0（首轮 2 失败后复跑通过）；**smoke** 单次全绿（约 8.9 min，2026-09-16）。
- 提交 `33850377`（文本对比工作区 i18n）：**check-core** 59/0；**smoke** 单次全绿（约 8.6 min，2026-09-16）。
- 提交 `9f77f444`（格式化工作区 i18n）：**check-core** 59/0（首轮 1 失败后复跑通过）；**smoke** 单次全绿（约 8.6 min，2026-09-16）。
- 提交 `ba444fdf`（留言板/系统信息工具栏 i18n）：**check-core** 59/0（2026-09-16）；**smoke** 首轮 JSON 原生验收失败，需单独复跑。
- 提交 `c3ba9058`（自定义分组、侧栏工具显示 sheet i18n）：**check-core** 59/0（2026-09-16）。
- 提交 `1faf101c`（JSON smoke 输入转换轮询加固）：**check-core** 59/0（首轮 Reformat worker JSON 3s 超时 1 项，单独复跑通过）；**smoke** 单次全绿（约 9.2 min，2026-09-16）。
- 提交 `0f5bb7a5`（留言板预设三语）：**check-core** 59/0（2026-09-16）。
- 提交 `62a04025`（Cron 工作区与首页其他作品 i18n）：**check-core** 59/0（首轮 1 失败后复跑通过，2026-09-16）。
- QR/图片/PDF 常见错误三语：**check-core** 59/0（2026-09-16）。
- HTTP 表单与集合弹窗三语：**check-core** 59/0（2026-09-16）。
- Host/收藏弹窗与 `AppStore.run` 状态三语：**check-core** 59/0（2026-09-16）。
- 网络工具面板与翻译页主控件三语：**check-core** 59/0（2026-09-16）。
- 提交 `098a7f0f`（翻译词库/历史三语）后：**smoke** 单次全绿（约 6.7 min，2026-09-16）。
- 工作区未提交：代码运行/系统信息/旧 net 模式、JSON 主工作区与 JSON/随手记文档库侧栏三语：**check-core** 59/0、**smoke** 单次全绿（约 8.9 min，2026-09-16；基线提交 `e2359e23` 之上本地改动）。
- 同上批次追加随手记编辑区（工具栏/查找/24 项快速替换）三语：**check-core** 59/0、**smoke** 单次全绿（约 7.2 min，2026-09-16）。
- TextTool 扩展（正则/计算器/环境变量/编码/Cron 预设菜单）三语：**check-core** 首轮 2 失败、复跑 **59/0**（2026-09-16）。
- 横切未提交（VaultGit 面板全文 `git.*`、大文件读提示、cURL 标题、独立工具窗口标题等）后：**smoke** 单次全绿（约 7.4 min，2026-09-16）。
- TextTool legacy JSON/紧凑加密三语与 crypto 稳定模式键：**check-core** 首轮 1 失败、复跑 **59/0**（2026-09-16）。
- 文档库磁盘刷新/Finder/工作区读写错误三语、查找「全词」缩写标签、**0.7.0** release-notes 补全后：**check-core** **59/0**、**smoke** 单次全绿（约 8.4 min，2026-09-16）。
- 侧栏右键常用/独立窗口、网络 `interfaces`/`detailed` 稳定键、TextTool 部分校验错误与空导入提示三语：**check-core** **59/0**（2026-09-16）。
- `DocumentImportReader` 批量导入校验错误全文三语（`vault.import.error.*`）：**check-core** **59/0**（2026-09-16）。
- Host 校验/配置保存错误与校验结果摘要三语（`host.error.*` / `host.validate.*`）：**check-core** **59/0**（2026-09-16）。
- 时间转换校验错误与「详细解析」字段标签三语：**check-core** **59/0**、**smoke** 单次全绿（约 5.9 min，2026-09-16）。
- JSON worker（`JSONDispatch.js` + `JSONEngine` 宿主错误）随 `language` 三语：**check-core** **59/0**；**smoke** 首轮 JSON ⌘Return 格式化验收失败（与历史 flake 同类），与 **check-core** 一并复跑后全绿（约 7.9 min，2026-09-16）。
- JSON 结构树/路径选择面板（`JSONTreePane` + `JSONStructure` 限额提示）三语：**check-core** **59/0**；**smoke** 首轮格式化验收失败、复跑全绿（约 7.3 min，2026-09-16）。
- 编码/配置转换/正则/计算器 Core 校验错误（`encode.error.*`、`config.error.*`、`regex.error.*`、`calculator.error.*`）随 `language` 三语：**check-core** **59/0**（约 3.2 min，2026-09-16）。
- 格式化宿主/`ReformatTools` 边界错误与撤销动作名（`reformat.error.*` / `reformat.undo.*`，含 `vendor/formatters/entry.js` 重打包）：**check-core** **59/0**、**smoke** 单次全绿（约 6.6 min，2026-09-16）。
- Cron 解析/构建/运行次数等引擎错误（`cron.error.*`，`QuartzCronParser` / `CronExpression`）：**check-core** **59/0**（约 3.1 min，2026-09-16）。
- Protobuf wire 解析与留言板选项校验（`protobuf.error.*`、`messageBoard.error.*`；`DeveloperServices.pageIndices` 复用 `pdf.error.pageRange`）：**check-core** **59/0**（2026-09-16）。
- TextTool / `CryptoToolView` legacy **AES-GCM** `TextServices.digest` 校验错误（`textCrypto.error.*`）：**check-core** **59/0**（约 2.6 min，2026-09-16）。
- 文本对比输入超限错误（`textDiff.error.*`，`TextDiffEngine.compare`）：**check-core** **59/0**、**smoke** 单次全绿（约 5.5 min，2026-09-16）。
- Electron 设置/SQLite 导入与备份校验（`migration.error.*`、`backup.error.*`、`workspace.error.*`；迁移面板传 `language`）：**check-core** **59/0**（约 3.2 min，2026-09-16）。
- 迁移 **warnings**（`migration.warning.*`）、收藏/翻译词条/媒体/自定义分组校验与 `vault.error.documentMissing`：**check-core** **59/0**（约 2.9 min，2026-09-17）；**smoke** 首轮随手记查找计数验收失败（`随手记查找计数没有更新为 3`），与历史 flake 同类，需单独复跑确认。
- RSA/SM2/`RSAOpenSSLBridge` 用户可见错误与验签结果（`crypto.error.*`、`crypto.verify.*`）：**check-core** **59/0**（约 2.5 min，2026-09-17）；**smoke** 复跑仍失败于格式化验收（`JSON 操作超过 3 秒`），随手记查找计数段未再报错（已加长 `waitForFind` 等待）。
- 格式化 worker 超时改为 10s（`dc5f1049`）后 **smoke** 单次全绿（约 3.7 min，2026-09-17；含附件/随手记/JSON/Vault/86 截图与重启恢复）。
- HTTP/`ProcessRunner`/网络诊断校验错误（`http.error.*`、`process.error.*`、`net.error.*`）：**check-core** **59/0**（约 1.5 min，2026-09-17）。
- 对称 **AES/DES/SM4 ECB**、Base32、`CurlCommand` / `HTTPFields` / `HTTPMultipartBuilder` Core 错误（`crypto.error.*`、`encode.error.base32Invalid`、`http.curl.*` / `http.field.*` / `http.multipart.*`）：**check-core** **59/0**（约 2.8 min，2026-09-17；**smoke** 未在本批复跑）。
- `DocumentVault` 库内校验/重命名/移动/批量导入（`vault.error.*` 扩展键）、`VaultGitService` 用户可见 `ToolError` 与操作结果摘要（`git.error.*` / `git.result.*`，含 Git 面板与自动检查点）：**check-core** **59/0**（约 2.9 min，2026-09-17）；**smoke** 单次全绿（约 7.1 min，2026-09-17）。
- `QuickNoteOptions` / 查找替换限额、`NoteImagePayload` / `NoteAttachmentRepository` / 备份恢复附件路径（`quickNote.error.*`、`attachment.error.*`、`backup.error.attachment*`）：**check-core** **59/0**（2026-09-17；**MooToolNextCore** 内 `ToolError` 硬编码中文已清零）；**smoke** 首轮格式化验收失败、加长 `NativeReformatAcceptance` 等待后单次全绿（约 6.9 min，2026-09-17）。
- App 层托盘状态、随手记图片预览/插入队列、`VaultStore` 删除草稿提示、legacy **AES-GCM** 稳定算法键（`tray.status.*`、`quickNote.image.*`、`quickNote.error.*` 插入守卫、`vault.status.deletedDraftKept`）：**check-core** **59/0**（约 3.0 min，2026-09-17）；**smoke** 单次全绿（约 9.0 min，2026-09-17；附件/随手记/JSON/Vault/86 截图与重启恢复）。
- 随手记 **Markdown 预览**块级 UI（`quickNote.preview.*`，含代码块/任务列表/预览失败标题）：**check-core** **59/0**（2026-09-17）；**smoke** 单次全绿（约 8.6 min，2026-09-17）。
- Cron **`describe`/`naturalSummary`**（`cron.describe.*` / `cron.weekday.*`，`CronToolView` 传 `language`）：**check-core** **59/0**（约 3.1 min，2026-09-17）。
- JSON worker **`json.valid.*`**（`JSONDispatch.js` 与 `JSONTools.js` 校验 idle/ok/error）：**check-core** **59/0**（约 0.7 min，2026-09-17）。
- 调色板预览标题与结果栏格式标签（`color.preview.title` / `color.label.*`）：**check-core** **59/0**（2026-09-17）。
- 计算器进制转换结果行标签（`calculator.label.*`）：**check-core** **59/0**（约 2.7 min，2026-09-17）；**smoke** 单次全绿（约 6.1 min，2026-09-17；含 Cron/JSONDispatch/调色板等同工作树批次）。

## 手工建议

- 设置 → 数据迁移：从本机 Electron `MooToolNext.db` 扫描并合并（HTTP/Host/翻译/历史/草稿/收藏）。
- 菜单栏图标：取色、截图、Host 配置切换。
- 文本对比：忽略空白、统一视图、历史恢复。
