# P5 网络与系统工具 Parity Checklist

> 阶段：P5
> 状态：实现完成，待 Java 对照验收
> Java 基线：当前 `github-develop-ts` 分支 Java 版
> 最后更新：2026-07-16

## 1. 工具清单

| 工具 | Java 布局/能力基线 | Next 实现 | 状态 |
| --- | --- | --- | --- |
| HTTP 请求 | 请求集合、URL/Method、Params/Header/Cookie/Body、响应三 Tab、cURL、保存和历史 | 完整请求编辑、主进程 Undici 客户端、代理、超时/取消、10 MB 响应上限、cURL 导入导出和 Java 兼容 SQLite 表 | 待对齐验收 |
| 翻译 | 翻译/单词本/历史三 Tab、源/目标语言、翻译器、自动翻译 | 500 ms 自动翻译、Google/Bing 轻量双实现与 fallback、代理/超时/取消、单词本 CRUD/重译和历史 | 待对齐验收 |
| Host | 方案列表、文本编辑、查找替换、导入导出、当前系统 Hosts、切换 | 方案 CRUD、编辑/查找替换、文件导入导出、系统 Hosts 查看、按平台提权写入和 DNS 刷新 | 待对齐验收 |
| 网络/IP | 命令输出与 IPv4、ping、DNS、地址解析、WHOIS、本机地址 | 白名单系统命令、IPv4/Long 双向转换、DNS 解析、WHOIS、本机 IPv4/IPv6、停止与错误码 | 待对齐验收 |
| 环境变量 | 系统变量/Java Properties Tab、搜索、表格、导出 | 系统环境变量/Electron 运行属性 Tab、搜索、逐项复制、刷新和完整导出 | 待对齐验收 |
| 系统信息 | 系统/CPU/内存/存储/网络五 Tab | `systeminformation` 结构化采集、五 Tab、刷新、复制、序列号脱敏 | 待对齐验收 |

## 2. 主进程与设置边界

- Renderer 只使用 typed preload API；HTTP、翻译、SQLite、Hosts、DNS、WHOIS、系统命令和硬件采集均在 Electron 主进程执行。
- `p5Validation.ts` 校验请求方法、URL/文本长度、Header/Cookie 数量、语言、超时、ID、Host 内容和系统命令枚举。
- HTTP 与翻译接入统一代理设置；代理密码从 `safeStorage` 读取，不进入 Renderer 和普通设置 JSON。
- 设置 Schema 升级到版本 2，提供 HTTP 请求超时、翻译超时、默认翻译器和源/目标语言。
- HTTP 与翻译共享可取消请求生命周期；网络命令使用白名单参数数组，不执行 Renderer 传入的脚本或 Shell 字符串。
- Hosts 先尝试普通写入，权限不足时分别使用 macOS `osascript`、Windows PowerShell `RunAs`、Linux `pkexec`。
- HTTP 请求、请求历史、Host 方案、翻译单词本和翻译历史使用 Java 表名兼容的 SQLite Repository。

## 3. 布局与行为

- [x] 六个页面仅保留主标题，无小标题和面包屑。
- [x] HTTP 保留“左侧请求集合 + 上方 URL/Method + 请求/响应上下分区”的 Java 工作流。
- [x] 翻译保留翻译/单词本/历史 Tab 顺序，单词本或历史回填不会触发重复在线翻译。
- [x] Host 保留“左侧方案 + 右侧工具栏/编辑器”，系统 Hosts 通过独立对话框查看。
- [x] 网络/IP 保留“左侧命令结果 + 右侧功能区”的桌面宽屏结构。
- [x] 环境变量和系统信息保留 Java 版 Tab、表格/键值结构与刷新、复制、导出操作。
- [x] 所有页面具有简体中文、英文、日文文案，并跟随系统明暗主题。
- [x] 1440 x 920 浅色与 1080 x 720 深色共 12 张页面截图已建立。
- [x] 两组视口下六页均无窗口级或工作区级横向溢出、控件遮挡和空白渲染。

截图位置：`doc/screenshots/p5-<tool>-<viewport-theme>.png`

## 4. 自动化证据

- `networkService.integration.test.ts` 使用本地 HTTP 服务覆盖 Query、Header、Cookie、表单 Body、响应格式化和翻译长文本切分。
- `p5Repository.integration.test.ts` 使用临时 SQLite 覆盖 HTTP 新增/更新、请求历史、Host、单词本和翻译历史。
- `systemService.test.ts` 覆盖 IPv4/Long 边界与 Hosts 换行、NUL 校验；HTTP 与网络页面纯逻辑另有单元测试。
- Electron E2E 使用本地 HTTP 服务覆盖真实主进程请求、保存集合与历史，并覆盖 Host 方案持久化、单词本、DNS 解析、环境变量和硬件信息。
- 全量 Vitest：28 个测试文件、83 项测试通过。
- Electron Playwright：10 项 E2E 通过。
- TypeScript 类型检查、Electron/Vite 生产构建和 `git diff --check` 通过。
- React Doctor：`100 / 100`，无诊断问题。

## 5. 已批准差异

| 差异 | 原因 | 状态 |
| --- | --- | --- |
| 环境变量第二个 Tab 展示 Electron 运行属性，不再显示 Java Properties | Next 不运行在 JVM 中；保留相同的运行时属性检查心智 | 不阻塞 |
| 翻译直接维护 Google/Bing 两个轻量实现，不建立 Provider 接口层 | 用户明确要求避免过重的 Provider 架构；当前仍保留首选服务和自动 fallback | 不阻塞 |
| 系统信息由 `systeminformation` 提供，字段文案与 Java OSHI 结果允许平台等价 | 两套底层库字段命名不同，核心系统/CPU/内存/存储/网络信息一致 | 不阻塞 |

## 6. 共享验收跟踪

- [ ] 使用 Java 版同尺寸截图完成六个工具的逐页人工 parity sign-off。
- [ ] 在 Windows 与 Linux 安装包中复测 `ipconfig`/`ip`、ping、DNS flush、Hosts 提权和系统信息字段。
- [ ] 使用真实代理账号验证 HTTP 与两个翻译服务的代理、认证、超时和 fallback。
- [ ] 为保护开发机和 CI，自动化不实际覆盖系统 Hosts 写入和 DNS flush；发布前需在隔离环境执行权限矩阵。
- [ ] 用旧版真实数据库样本验证五张 P5 业务表迁移；统一放在 P7 数据迁移阶段完成。

以上项目属于发布前共享 parity、外部服务和跨平台验收，不影响 P5 实现范围已完成的结论。
