# 0.8.0 验收记录

产品：`next-macos-native` · 版本 **0.8.0**

## 自动化

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `./scripts/check-core.sh` | 59 组，0 失败 | 含 SQLite 导入（HTTP/Host/翻译/收藏/历史/草稿/`t_quick_note`/`t_json_beauty`） |
| `./scripts/smoke.sh` | 通过（偶发需复跑） | 附件、随手记、JSON、格式化、文档库选区/滚动、86 张截图、重启持久化；与 SwiftPM 并发时格式化验收可能需等待更久 |

最近一次 smoke 全绿：**2026-09-16** — 关闭主窗口 `closeBehavior` 合入后复跑通过（约 8.4 min）；同日 `t_quick_note` / `t_json_beauty`、托盘与 `t_func_content` 亦全绿。

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

## 手工建议

- 设置 → 数据迁移：从本机 Electron `MooToolNext.db` 扫描并合并（HTTP/Host/翻译/历史/草稿/收藏）。
- 菜单栏图标：取色、截图、Host 配置切换。
- 文本对比：忽略空白、统一视图、历史恢复。
