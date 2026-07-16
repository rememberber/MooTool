# P7 全量对齐、迁移与发布验收

> Next 版本：`1.7.8`  
> Java 基线：当前 `github-develop-ts` 分支 Java 版 `v1.7.8`  
> 更新日期：2026-07-16  
> 结论：代码实现与 macOS Apple Silicon 本机候选包已通过；正式公开发布仍需远端跨平台矩阵、签名/公证和人工 Java 同尺寸截图签收。

## 1. P7 完成范围

| 范围 | 当前结果 | 状态 |
| --- | --- | --- |
| JSON Vault | 文件夹、重命名、复制、移动、拖拽、排序、忽略规则、文件监视 | 已完成 |
| Vault Git | 状态、Diff、历史、冲突处理、撤销、中止合并、自动提交、自动拉取 | 已完成 |
| Quick Note | Java 兼容文件、附件、树操作、拖拽、监视、Git、自动保存 | 已完成 |
| 旧版迁移 | SQLite、配置、Quick Note、JSON Vault、收藏、HTTP、翻译 | 已完成 |
| 更新 | Java 版更新源、启动检查、24 小时轮询、手动检查、发布页 | 已完成 |
| 桌面生命周期 | 托盘、隐藏/退出/询问、设置同步、菜单 | 已完成 |
| 性能 | 启动、3 MB JSON、5 MB Quick Note、进程与 heap 内存门槛 | 已完成 |
| macOS ARM64 包 | App、DMG、ZIP、启动、压缩与镜像完整性 | 已完成 |
| 跨平台包 | Apple Silicon/Intel/Windows/Linux GitHub Actions 矩阵 | 工作流已就绪，待远端执行 |
| 正式签名 | Apple Developer ID、公证、Windows 签名 | 待发布凭证 |

OCR 是已批准的 parity 例外，不进入 Next，也不阻塞本轮验收。

## 2. 旧版迁移边界

### 2.1 扫描与导入

- 默认扫描 `~/.MooTool`，同时识别 Java 配置中的自定义数据库、Quick Note Vault 和 JSON Vault 路径。
- 扫描阶段只读，不修改 Java 目录、数据库或 Git 仓库。
- 导入 `t_func_history`、HTTP 请求/历史、Host、翻译单词本/历史、颜色/Regex/Cron 收藏、二维码历史及旧功能内容。
- `t_quick_note` 转换为 Java 兼容 YAML frontmatter 文本；`t_json_beauty` 转换为 JSON 文件。
- 迁移现有 Quick Note、JSON Vault 文件和附件；识别 Java `.migrated-from-db` 标记，避免数据库与文件重复导入。
- 仅映射明确存在的语言、主题、布局、编辑器、非敏感代理、Vault 和工具默认值。
- 代理密码与 Git Token 不从明文配置迁移，用户需在 Next 设置中重新写入 `safeStorage`。

### 2.2 幂等与回滚

- 每次迁移先创建完整 Next 备份，再关闭 SQLite Repository，执行事务导入，最后重新打开 Repository。
- `t_next_migration_run` 记录来源指纹和运行结果，`t_next_migration_row` 记录逐行来源键；重复运行不会重复写入。
- 来源与目标重叠、符号链接逃逸和路径穿越会被拒绝。
- SQLite 失败时事务回滚，同时删除本次新建的 Vault 文件。
- Java 原数据始终保留，可继续启动旧版；Next 导入失败不影响 Java 数据。

### 2.3 回退步骤

1. 退出 MooTool Next，保留 Java 目录不动。
2. 在设置的“数据与备份”中确认迁移前完整备份位置。
3. 删除或移走本次 Next 数据目录，再恢复迁移前完整备份。
4. 重新启动 Next 并核对数据库、配置、图片、Quick Note 和 JSON Vault。
5. 若仍需使用旧版，直接启动 Java MooTool；迁移过程未写入旧目录。

## 3. 性能验收

基准运行于打包后的 macOS ARM64 App，结果保存在 `doc/parity/p7-performance-results.json`。

| 指标 | 结果 | 门槛 |
| --- | ---: | ---: |
| 冷启动至工作台可用 | 622.53 ms | 8,000 ms |
| 2.98 MB JSON 输入并格式化 | 1,084.32 ms | 8,000 ms |
| 5.26 MB Quick Note 保存/读取/删除 | 177.83 ms | 8,000 ms |
| 峰值相对内存增量 | 257.58 MB | 400 MB |
| 页面卸载并 GC 后稳定内存增量 | 96.69 MB | 200 MB |

JSON 编辑器已由原生 `textarea` 改为 CodeMirror 6 虚拟视口。超过 1 MB 时停用完整语法树，仅保留纯文本虚拟渲染；格式化、校验、查找、换行、字号、历史和 Vault 行为保持不变。历史 IPC 保存后不再把完整输入输出回传 Renderer。

## 4. 自动化与视觉证据

- TypeScript 与生产构建：通过。
- Vitest：`35` 个文件、`114` 项单元/集成测试通过。
- Electron Playwright：`16` 项端到端测试通过。
- React Doctor：`100 / 100`。
- P7 视觉：JSON `1440 x 920` 浅色、`1080 x 720` 深色；迁移设置 `920 x 700` 明暗主题，全部零页面溢出、零目标越界。
- 迁移集成：5 项覆盖预览、配置映射、敏感项跳过、幂等、重叠路径和失败回滚。
- 更新服务：4 项覆盖 feed 校验、版本比较、可用更新和已是最新版。

截图：

- `doc/screenshots/p7-json-1440-light.png`
- `doc/screenshots/p7-json-1080-dark.png`
- `doc/screenshots/p7-migration-920-light.png`
- `doc/screenshots/p7-migration-920-dark.png`

## 5. 包与发布证据

本机已验证 `dist/mac-arm64/MooTool.app`，版本字段均为 `1.7.8`，启动后工作台可见且注册入口数量为 25。

| 产物 | 大小 | SHA-256 |
| --- | ---: | --- |
| `MooTool-1.7.8-mac-arm64.dmg` | 128 MB | `147c6ced5ec3d62dc54e6ff102f37c2fb0e4b244d5b81a6bdd3d003bfdffc0e7` |
| `MooTool-1.7.8-mac-arm64.zip` | 122 MB | `35a2627e33c2debbfe768cc37bcfad4b75290c457f2a11a6a0d82a5db50790ae` |

`hdiutil verify` 与 `unzip -t` 均通过。当前候选包为本地 ad-hoc/未正式签名构建，不应直接作为公开下载包。

`.github/workflows/next-build-installers.yml` 提供四目标手动矩阵：

- macOS Apple Silicon：DMG + ZIP
- macOS Intel：DMG + ZIP
- Windows x64：NSIS + Portable
- Linux x64：AppImage + DEB

每个 Runner 先执行 `npm ci` 和 `npm run check`，再打包并上传 14 天构建产物。矩阵结构已完成 YAML 解析和四目标静态校验；远端执行结果不能在当前无 GitHub CLI/连接器的本机代替。

## 6. 发布清单

- [x] Next/Java 版本统一为 `1.7.8`。
- [x] 更新源、项目页和 Releases 跳转接通。
- [x] 更新、托盘、关闭策略、设置子窗口通过 E2E。
- [x] 旧数据只读扫描、备份、事务迁移、幂等与回滚通过。
- [x] 性能与大文本门槛通过。
- [x] macOS ARM64 App、DMG、ZIP 生成并验证。
- [x] 四平台 CI 打包矩阵已定义。
- [ ] 在 GitHub Runner 实跑 macOS Intel、Windows x64、Linux x64。
- [ ] 配置 macOS Developer ID、Hardened Runtime、公证与 stapling。
- [ ] 配置 Windows 代码签名并验证 SmartScreen 安装流程。
- [ ] 用 Java 版同尺寸截图完成 24 个工具人工布局签收。
- [ ] 使用真实第三方凭证验收翻译、代理和远程 Git 网络链路。

在上述未完成的发布门槛完成前，当前版本应视为内部 Release Candidate，不标记为正式公开发布。
