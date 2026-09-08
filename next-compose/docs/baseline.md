# 源码基线与参考地图

## 1. 本次事实范围

编写日期：2026-09-08；仓库 HEAD：`9fdd13087c9c4da6f01872e3215e66053010cb59`。本次读取当前工作树源码，未启动 Electron/Java 做实时视觉验收，未创建 Compose 工程。仓库存在其他产品及根发布文档的未提交修改，本次不修改它们。

| 项目 | 当前观察 | 证据（相对仓库根） |
| --- | --- | --- |
| Electron 版本 | 1.1.4 | `next/package.json` |
| Java 版本 | Maven 1.8.6；UI 为 v1.8.6 | `pom.xml`、`src/main/java/com/luoboduner/moo/tool/ui/UiConsts.java` |
| Compose 状态 | 编写文档前为空目录 | `next-compose/` |
| 导航 | 26 入口：1 首页 + 25 工具；6 个功能分组 | `next/src/app/toolRegistry.ts` |
| 沉浸布局 | 除 `mootool` 首页外全部工具 | `next/src/features/workbench/immersiveTools.ts` |
| 主窗口 | 默认 bounds 1440×920，最小 1080×720 | `next/electron/main/index.ts` |
| 视觉默认 | modern、system、blue、UI 13、编辑器 14、统一背景 | `next/src/shared/contracts/settings.ts` |
| 导航默认 | classic、展开、显示分隔线；最近使用默认不显示 | 同上；`next/src/app/appStore.ts` |
| 导航宽度参照 | 展开 248，隐藏标题 84；CSS 后续覆盖也影响视觉 | `next/src/shared/styles/global.css` |
| 设置 | 主工作区内 `SettingsPage`，11 类；schemaVersion 12 | `Workbench.tsx`、`SettingsWindow.tsx`、`settings.ts` |
| 文本编辑 | CodeMirror 公共组件；随手记支持列编辑 | `next/src/shared/components/TextCodeEditor.tsx` |
| 历史 | 通用历史按 func_type 各保留最多 200 条 | `next/electron/main/historyRepository.ts` |
| 代码运行限制 | 源码 1 MiB、输出 2 MiB | `next/electron/main/runtimeExecutionService.ts` |
| HTTP 限制 | 响应上限 10 MiB | `next/electron/main/networkService.ts` |

当前注册表里的 `in-progress/parity-review/complete` 是 Electron 作者的状态，不是 Compose 完成证据。README 中的“原型”和旧迁移计划中的“24 工具”也不能替代当前源码。

## 2. 证据优先级与差异处理

1. 用户当前目标与明确决定。
2. 本产品已记录的设计决定及对应验收。
3. 冻结版本的 Electron 实际运行、源码和有效测试，互相矛盾时先复现。
4. Java 对应功能、工具类及测试，补充 JVM 算法和 Electron 未明确部分。
5. 同设置/尺寸的截图；旧迁移清单、README、旧截图仅辅助。

优先对齐 **入口 → 面板关系 → 操作顺序 → 参数/默认值 → 输出语义 → 键盘与状态 → 视觉细节**。Java 与 Electron 冲突时，默认选择 Electron 用户工作流；JVM 算法可选择更正确的 Java 行为，但要保留源样本、写出差异，不能声称两边完全兼容。

开发开始时将 commit、相关工作树 diff、OS/架构、窗口尺寸、主题、语言、字体和 DPI 保存到 `docs/evidence/<日期>/environment.md`。后续只吸收有意选择的变化，不随参考版本更新而自动改写冻结基线。

## 3. 关键阅读路径

| 范围 | Electron | Java 补充 |
| --- | --- | --- |
| 壳、导航、会话 | `next/src/app/`、`features/workbench/`、`shared/contracts/app.ts` | `ui/form/MainWindow.java`、`ui/startup/`、`util/FuncGroupUtil.java` |
| UI 与主题 | `next/src/shared/styles/global.css`、`shared/components/` | `ui/UiConsts.java`、`util/EditorFontUtil.java` |
| 设置 | `next/src/features/settings/`、`shared/contracts/settings.ts` | `util/ConfigBaseUtil.java`；按页面追踪具体设置读写 |
| 文档编辑 | `TextCodeEditor.tsx`、`findReplace.ts`、`codeEditorViewState.ts` | `util/TextAreaUtil.java`、`util/UndoUtil.java`、`ui/form/func/QuickNoteForm.java` |
| Vault / Git | `next/electron/main/*VaultRepository.ts`、`vaultGitService.ts`、`vaultGitCheckpointScheduler.ts` | `util/QuickNote*`、`util/JsonBeauty*`、`util/Vault*` |
| 网络、系统、进程 | `networkService.ts`、`systemService.ts`、`runtimeExecutionService.ts` | `service/`、`util/EnvironmentVariableService.java`、`util/HostFileUtil.java` |
| 历史、集合、图片 | `historyRepository.ts`、`favoriteRepository.ts`、`p5Repository.ts`、`imageRepository.ts` | `domain/`、`dao/`、`util/SqliteUtil.java` |
| 备份与更新 | `backupService.ts`、`legacyMigrationService.ts`、`update*.ts` | `util/UpdateDownloadManager.java`、`util/DownloadLinkSelector.java` |
| 回归 | `next/src/**/*.test.ts`、`next/tests/electron/` | `src/test/` 中相关工具测试 |

上表 Java 简写从 `src/main/java/com/luoboduner/moo/tool/` 开始；Electron 未带目录的 service 文件从 `next/electron/main/` 开始。各工具精确入口见 [功能规格](feature-parity.md)。**这些路径仅用于阅读，不能直接写入 Compose 的 sourceSets/resources/classpath。**

## 4. 已识别的规格陷阱

- `java` 是持久 Tool ID，Electron 显示的代码运行还包括 Groovy、Python 和 Node.js。
- `messageBoard` 是本地展示牌；不是联网留言系统。
- HTTP 当前正文下拉为 JSON、plain text、XML、text XML、HTML、JavaScript 六种 MIME。Params 是否进入 URL/表单由 `networkService.ts` 分支决定，不能杜撰 multipart 文件上传 Tab。
- Cron 的 `splitCron` 接受 **6 或 7 字段**；`?` 和 week/year 经过适配，不等同完整 Quartz 引擎。5 字段 Unix 模式只能作为独立新增模式。
- Crypto 默认兼容路径是 ECB + PKCS#7；key 的字符截断/补零后 UTF-8 行为必须用字节样本确认。
- Electron Protobuf `keepCase: true`，输出 defaults、long/enum/bytes 字符串；Java `JsonFormat` 有独立命名和未知字段处理。不可直接假定 JSON 输出一致。
- 图片包含真正的 SVG 矢量化；颜色运算枚举为 invert/intersect/add/difference/average 五种，不能沿用旧摘要中的错误数量。
- 所有工具支持的历史/收藏标志并不相同；收藏、HTTP 请求集合、单词本、Vault 不是同一个实体。
- 根发布文档尚未登记 `next-compose`，不能把拟定的 ID/清单节点写成已经上线。

## 5. 官方技术资料与核验日期

以下资料于 2026-09-08 查询。库版本和 API 会变化；P0 要保存实际锁定组合，不能把本文当作编译通过的模板。

| 官方资料 | 本项目采用的信息 |
| --- | --- |
| [Compose 兼容与版本](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html) | 当前页列出 1.12.0；Desktop 走 JVM、打包 JDK 至少 17；Compose compiler 插件与 Kotlin 插件同版本。当前平台表仅列 macOS 13 arm64，Intel 不能直接承诺 |
| [1.12.0 发布说明](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.12.0) | `SwingPanel.background` 弃用；部分 Material3/导航组件仍为预发布，不应看到 Compose 稳定就假定所有关联库稳定 |
| [Gradle/Kotlin 兼容](https://kotlinlang.org/docs/gradle-configure-project.html) | 用受支持组合锁定 Gradle、Kotlin 与 JVM target |
| [原生安装包](https://kotlinlang.org/docs/multiplatform/compose-native-distribution.html) | jpackage/jlink、自带 runtime、各 OS 本机打包、JDK 模块和平台包版本约束 |
| [Swing 互操作](https://kotlinlang.org/docs/multiplatform/compose-desktop-swing-interoperability.html) | 可封装 Swing 编辑器；默认叠层有限制，experimental blending 不能作为未经验证的生产前提 |
| [桌面 UI 测试](https://kotlinlang.org/docs/multiplatform/compose-desktop-ui-testing.html) | JUnit + Compose 测试 API；KMP desktop source set 可运行 desktopTest |
| [桌面可访问性](https://kotlinlang.org/docs/multiplatform/compose-desktop-accessibility.html) | macOS 支持、Windows 经 Java Access Bridge，页面目前将 Linux 列为不支持；需按平台如实验收 |
| [RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea) | 成熟 Swing 语法高亮/折叠组件候选；列编辑需本项目实验确认，不假定自带完整多光标 |
| [Protobuf DynamicMessage](https://protobuf.dev/reference/java/api-docs/com/google/protobuf/DynamicMessage.html) | 可以根据 Descriptor 处理动态类型，但不会自行把 `.proto` 源文本编译为 Descriptor |
| [Cursor Rules](https://prod.cursor.com/docs/rules) | 项目规则使用 `.cursor/rules/*.mdc` 与 frontmatter；本产品采用简短入口规则，详细规范保存在普通文档中 |

UI Token、性能目标、架构分层和分阶段安排是本项目建议值，非上述官网承诺。
