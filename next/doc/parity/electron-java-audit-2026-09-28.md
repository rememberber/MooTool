# Electron / Java 全局差异首轮审查

日期：2026-09-28。基线：develop 当前源码，包含 Cron 功能补齐与布局修复。

本轮为功能入口、主流程与关键实现的源码对照；AES 和正则补充了本机可执行样例。未逐页运行 Java/Electron 做完整视觉验收，未做联网、跨平台、全部算法和旧数据迁移的端到端验收。以下“已确认”表示可以从实现或样例确认，不表示整个模块已经全面验收。本轮仅新增审查文档。

## 优先处理的差异

| 优先级 | 模块 | 已确认差异 | 用户影响 |
| --- | --- | --- | --- |
| P1 | 加解密 | Electron 的 AES 密钥固定截断/补字符 0 到 16 个字符；Java/Hutool 使用传入字节构造 AES 密钥 | 合法 24/32 字节密钥不等价；短密钥的报错/接受行为也不同。旧密文不能假定可互解 |
| P1 | 正则 | Java 正则引擎换成 JS RegExp，没有兼容转换；输入变化不自动执行或在原文中标记匹配 | 旧表达式可能报错或静默得到不同结果；原文定位体验退化 |
| P1 | 正则收藏 | Electron 仍使用通用简化 FavoriteDialog，仅命名保存、应用、逐条删除 | Java 的分组 CRUD、条目编辑、多选删除、上下排序未接齐 |
| P2 | JSON / 随手记 | Electron 文件树状态/操作围绕单个 selectedPath/selectedEntry；Java 复制、导出遍历多选记录 | 缺少多选批量复制/导出流程。不能因单文件 CRUD 和 Git 已有就视为完全对齐 |
| P2 | 调色板收藏 | 已有收藏夹分组，但条目缺少重命名/编辑、上下排序及多选删除入口 | 收藏管理能力仍弱于 Java；新 Cron 收藏能力尚未复用到这里 |
| P2 | 草稿恢复 | 正则、计算器使用固定初始值及组件内 state；Java 恢复表达式与 t_func_content；迁移把旧草稿写入历史 | 重启恢复工作现场不等价；迁移后的草稿需要另外从历史寻找，不是自动恢复 |
| P2 | 图片批处理 | Java SVG 转换有逐项进度和取消；Electron processSvg 只有 busy + 最终返回。压缩/水印外层统一 catch，单项失败会中断后续 | 大批任务缺少可见进度和取消；部分失败的反馈、继续处理不如 Java |
| P2 | HTTP | Java 有发送并打开独立响应窗口；Electron 当前 HttpTool 只有内嵌响应面板 | 通用“工具独立窗口”不是单独响应窗口的等价替代 |

## 证据索引

路径均相对仓库根目录。

- AES：`next/src/features/crypto/cryptoTools.ts:17`、`:155`；`src/main/java/com/luoboduner/moo/tool/ui/listener/func/CryptoListener.java:58`。本机 Maven 的 Hutool 5.8.47 源码 `KeyUtil.generateKey(String, byte[])` 确认 AES 走 `new SecretKeySpec(key, algorithm)`。
- 正则：`next/src/features/regex/regexTools.ts:28`、`next/src/features/regex/RegexTool.tsx`；Java `RegexListener.java` 的 `MarkAllUpdater`、`findOrMarkAll`、`selectMatchNearCaret`。
- 正则收藏：`next/src/features/favorites/FavoriteDialog.tsx`；Java `FavoriteRegexForm.java`。
- 文件树：`next/src/features/quickNote/QuickNoteTool.tsx:700`、`:790`；`next/src/features/json/JsonVaultPanel.tsx:416`；Java `QuickNoteListener.java:652`、`:676` 与 `JsonBeautyListener.java:700`、`:724`。
- 颜色收藏：`next/src/features/color/ColorFavoriteDialogs.tsx:96`；Java `FavoriteColorForm.java` 的 moveUp/moveDown 和条目名称编辑监听。
- 草稿：`next/src/features/regex/RegexTool.tsx`、`next/src/features/calculator/CalculatorTool.tsx:13`；`next/electron/main/legacyMigrationService.ts:313`；Java `RegexForm.java:108`、`CalculatorForm.java:91`。
- 图片：`next/src/features/image/ImageTool.tsx:236`、`:246`；Java `ImageListener.java:565`（SVG 进度/取消）及 `watermarkImages`（逐项处理与错误汇总）。
- HTTP：Java `HttpRequestListener.java:285`；Electron `next/src/features/http/HttpTool.tsx` 请求工具栏及响应区域。

## 可执行兼容性样例

AES 输入 `MooTool parity`，密钥 `123456789012345678901234`：

- Java `Cipher.getInstance("AES")` + 原始 24 字节密钥：`a66c2ff48c6ded0ae657017c86bc4930`。
- 直接执行 Electron `symmetricEncrypt`：`3f4916b141b6d3442df1017170f155e8`。
- Node 原始 AES-192/ECB 交叉计算与 Java 结果一致。

正则 `\Qfoo.bar\E`，原文 `foo.bar`：

- Java `Pattern.matcher(...).find()`：true。
- 直接执行 Electron `matchRegex`：空数组。

这些样例使用临时探针，没有修改业务代码，也没有拿双方各自加解密往返通过当作跨版兼容证据。

## 其他模块的首轮判断

| 范围 | 判断与后续重点 |
| --- | --- |
| Cron | 本轮会话已集中补齐，不作为新缺项；特殊表达式仍应继续积累跨版样例 |
| 时间转换、编码解码、UA、配置转换、格式化、Protobuf、二维码 | 主要功能入口已有；尚未做完整输入输出等价性矩阵，不判定为完全对齐 |
| 计算器 | 基本运算种类已有。Java 输出最多 8 位小数，Electron 使用 14 位有效数字；除草稿恢复外，还有数值格式差异，暂不列为大功能缺失 |
| 文本 Diff | 已有编辑器内差异高亮、导航、同步滚动等主能力；需单独做同尺寸视觉与边界文本对照 |
| HTTP、翻译、Host、网络/IP、环境变量、系统信息 | 主工作流大多存在。HTTP Cookie 的 domain/path/expiry 在 Electron buildHeaders 中未参与拼接判断，应另做实际请求行为对照；在线服务、提权和平台行为未在本轮实测 |
| PDF | 拆分/合并、页码和规则已有；任务状态为 ready/running/done/error，Java 使用进度条。先列体验验收项，未确认与 Cron 同量级的大面积缺失 |
| 代码运行 | 已有多运行时、草稿、参数、停止和历史；Java/Groovy 脚本语义及外部运行时依赖需要样例对照，不能只按 Tab 数量评价 |
| 图片、JSON、随手记、调色板 | 主能力已有，具体缺项见上表，不应整体判为缺失 |
| 设置、备份、迁移与更新 | 已有实现及历史验收文档；本轮明确确认草稿迁移与恢复不等价，其他项目仍需平台与真实旧数据验收 |
| 留言板 | 本轮未进行服务端及联网行为验收 |

## 不应误列为遗漏

- 现有迁移计划明确记载 OCR 为已批准不迁移项，本轮不重新归为缺陷。
- Java 全局远程同步本身为禁用/未实现状态，不应要求 Electron 补一个原版没有的可用功能。
- 原文档的“P1–P7 实现完成”同时保留了“待 Java 对照验收”，不能解读为已证明行为和交互完全等价。

建议顺序：先修 AES / 正则兼容性；再统一 Cron、正则、调色板的收藏管理；然后补文件树多选、草稿恢复与图片任务进度/取消；最后逐页做 Java 同尺寸视觉验收。

## 2026-09-29 跟进：AES 与正则

本次已修复 AES 原始密钥兼容性，并提供仅用于旧 Electron 密文的显式解密选项；正则新增 Java 实际引擎（需要 JDK 11+，复用运行环境设置），保留 JavaScript 模式，补上实时高亮、定位、草稿保存和完整收藏管理。实现边界及验证见 `aes-regex-compatibility.md`。其他审查项尚未在本次修改。
