# 验收标准、进度与证据

> 更新：2026-09-09。P0/P1/JSON 之后已接入 F02 文本对比、F03 格式化、F12 UA、F13 编码、F15 正则、F16 Cron、F18 时间、F21 计算器；完整产品与三平台发行仍未验收。

## 1. 状态规则

状态只使用：未开始、开发中、待验收、已验收、明确差异。已验收必须有具体证据；明确差异必须有 DIFF 记录并说明它影响哪些用例，不能用它隐藏尚未完成的编码。

功能拆分验收至少包括：控件/入口、正常流程、错误/取消、状态/持久化、语义 fixture、视觉/键盘、平台能力。某工具只有格式化或默认路径完成，应在备注列写具体范围，不能将整行设为已验收。

## 2. 当前进度

| 条目 | 目标 | 当前状态 | 证据/差异 |
| --- | --- | --- | --- |
| P0 | 工具链/编辑器/窗口/动态 proto 等实验 | 开发中 | 本机 Wrapper/JDK21/Compose1.12 构建与 app-image 启动见 `docs/evidence/2026-09-09-p0-p1/`。RSTA 已接入，IME/列编辑未做桌面交互验收。protoc 未捆绑。ADR-001/002/003 |
| P1 | 桌面壳/搜索/设置基础 | 开发中 | 26 入口、搜索、modern 明暗、语言、基础设置、JSON 分离窗口代码已有；视觉截图与完整键盘流程待验收 |
| P2 | 完整 JSON 基础工作流 | 开发中 | 仅最小切片：格式化/压缩/查找/历史/Vault CRUD/转换。Git、冲突监视、完整检查器弹层未完成，**不能标 F04 已验收** |
| P3 | 文本与本地算法 | 开发中 | F02 文本对比、F03 格式化、F12 UA、F13 编码、F15 正则、F16 Cron、F18 时间、F21 计算器已有引擎单测与 UI；其余 P3 工具仍显示尚未实现 |
| P4 | 媒体/加密 | 未开始 | — |
| P5 | 网络/系统 | 未开始 | — |
| P6 | 文档/Git/运行台/备份 | 未开始 | — |
| P7 | 完整产品/平台安装发行验收 | 未开始 | — |
| F00 | 首页 | 待验收 | 已实现 Compose 品牌/0.1.0/链接；无运行截图 |
| F01 | 随手记 | 未开始 | 入口显示尚未实现 |
| F02 | 文本对比 | 待验收 | 行/字符 Myers 差异、三种高亮、忽略空白、统一补丁、上/下差异、导入/复制/清空/交换、历史与分离窗口。与 Electron `diff` 样本对齐。无运行截图 |
| F03 | 格式化 | 待验收 | 文本/文件 Tab，Nginx/Java/XML/HTML，缩进 2–6，真实解析格式化、语法错误定位、另存不覆盖原文件、历史与分离窗口、Cmd/Ctrl+Shift+F。引擎差异见 [DIFF-005](diff/005-reformat-jvm.md)。无运行截图 |
| F04 | JSON | 开发中 | 算法 7 项单测通过；UI 切片已能启动；非完整 F04 |
| F05 | 代码运行 | 未开始 | 入口显示尚未实现 |
| F06 | 配置转换 | 未开始 | 入口显示尚未实现 |
| F07 | Protobuf | 未开始 | 仅有 protoc 探测实验类，未捆绑二进制 |
| F08 | 环境变量 | 未开始 | 入口显示尚未实现 |
| F09 | HTTP | 未开始 | 入口显示尚未实现 |
| F10 | Host | 未开始 | 入口显示尚未实现 |
| F11 | 网络/IP | 未开始 | 入口显示尚未实现 |
| F12 | UA | 待验收 | 预设 Chrome/Safari/Firefox/iPhone/Android、Googlebot、空/未知；版本与设备字段有单测。引擎推断见 [DIFF-002](diff/002-ua-engine-inference.md)。无运行截图 |
| F13 | 编码解码 | 待验收 | 引擎覆盖 Unicode/emoji、URL UTF-8 与 GB2312 往返、Hex/ASCII、非法 Hex、截断字节、GB2312 不可映射拒绝问号。UI 含四分区、历史、分离窗口。无运行截图。安装镜像需 `jdk.charsets`（已加入 jlink modules） |
| F14 | 加解密/随机 | 未开始 | 入口显示尚未实现 |
| F15 | 正则 | 待验收 | Java Pattern + 独立 worker（2s/10000 上限）；21 条常用模式、flags、捕获/命名组、零宽前进、非法模式保留原文、收藏 JSON 重启恢复、历史与分离窗口。引擎差异见 [DIFF-003](diff/003-regex-java-pattern.md)。无运行截图 |
| F16 | Cron | 待验收 | Quartz 6/7 字段、预设、IANA 时区、未来 10 次、年过滤、闰日、收藏与历史、分离窗口。与 Electron cron-parser 差异见 [DIFF-004](diff/004-cron-quartz.md)。无运行截图 |
| F17 | 二维码 | 未开始 | 入口显示尚未实现 |
| F18 | 时间 | 待验收 | 引擎单测覆盖 epoch/负值/毫秒/DST/闰年/显式单位；UI 含双向转换、时区、历史、大屏时钟、分离窗口。无运行截图。单位语义见 [DIFF-001](diff/001-time-explicit-unit.md) |
| F19 | 留言板 | 未开始 | 入口显示尚未实现 |
| F20 | 翻译 | 未开始 | 入口显示尚未实现 |
| F21 | 计算器 | 待验收 | 引擎单测覆盖 `2*(3+4)=14`、负数、进制、GCD/LCM、排列组合与非法输入；UI 含等号计算、结果复制、会话与历史。无运行截图。表达式按 IEEE Double 再按 14 位有效数字展示，与 Electron 一致，未改用任意精度小数 |
| F22 | 调色板 | 未开始 | 入口显示尚未实现 |
| F23 | 图片 | 未开始 | 入口显示尚未实现 |
| F24 | PDF | 未开始 | 入口显示尚未实现 |
| F25 | 系统信息 | 未开始 | 入口显示尚未实现 |
| A01 | 11 类设置 | 开发中 | general/appearance/layout/editor/data/about 基础项生效；其余类别明确未实现 |
| A02 | 历史/收藏/搜索 | 开发中 | JSON、编码、UA、正则、Cron、文本对比、格式化、时间转换与计算器历史已有；正则/Cron 收藏已落地；调色板收藏未做 |
| A03 | 桌面/存储/备份/Git/更新 | 开发中 | 独立路径与 SQLite 已有；备份/Git/更新未做 |

## 3. 工程检查入口

P0 已核实下列任务存在，并在本机执行部分命令。Windows 使用 `gradlew.bat`。各 OS 打包任务在该 OS 执行。

```bash
cd /path/to/next-compose
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"   # macOS 示例
./gradlew --version
./gradlew :composeApp:printTooling
./gradlew :composeApp:desktopTest
./gradlew :composeApp:run
./gradlew :composeApp:createDistributable
./gradlew :composeApp:runDistributable
./gradlew :composeApp:packageDistributionForCurrentOS
```

本机 2026-09-09 结果：F03 接入后 `desktopTest` **56/56** 通过（含 ReformatEngine 6；此前 F02 为 50/50）。`createDistributable` 此前生成 `MooTool Next Compose.app`；本轮未重跑打包。`runDistributable` 与 `packageDistributionForCurrentOS` 未跑完。

测试层级：

| 层级 | 验证对象 | 不能代替 |
| --- | --- | --- |
| 纯逻辑/fixture | 算法、解析、模型、版本/路径、错误 | 输入法和真实文件系统 |
| 临时目录/DB 集成 | 设置、迁移、原子保存、WAL 备份、watcher、冲突 | 真正安装/卸载 |
| 受控网络/进程 | HTTP 参数、超限/取消、输出上限、停止后代 | 在线服务真实可达性 |
| Compose UI | 导航、语义、控件动作、焦点、分栏 | Swing 编辑器内部节点完整覆盖 |
| Swing/原生集成 | IME、undo、列编辑、快捷键、弹层、拖放 | headless 单测 |
| 安装镜像/目标 OS | runtime、native lib、权限、identity、升级卸载 | `run` 或 IDE 预览 |

## 4. 必须通过的关键场景

| 编号 | 场景 | 结果要求 | 本轮 |
| --- | --- | --- | --- |
| T01 | 只取出本产品目录构建 | 不读取相邻源码/资源/构建产物，正常第三方依赖可下载 | 本机构建通过；未做“拷贝到仓库外”复测 |
| T02 | 无外部 Java 安装的干净系统启动镜像 | 自带 runtime 可运行常规工具 | 本机 app-image 使用捆绑 runtime 启动成功；不是干净机器 |
| T03 | 26 项导航/搜索/隐藏/分组 | 不漏工具，隐藏仍可搜，分组删除不删数据 | 注册表单测通过；UI 待验收 |
| T04 | 切工具再返回、重启 | 输入/选项/Tab/路径恢复 | JSON 与时间转换会话可持久化；完整重启 UI 未测 |
| T05–T07 | 编辑器分离/IME/列编辑 | 见规格 | 未测 |
| T08 | JSON 重复 key/大整数/filter | 真实执行 | 单测覆盖重复 key 与 9007199254740993；JSONPath filter 未单列 |
| T09–T20 | 其余关键场景 | 见规格 | 未测 |

## 5. 性能目标与测量方式

初始目标未改，**尚未按发布镜像采样**。不要把本次开发机 `run`/单测时间当成冷启动指标。

## 6. 视觉与平台签收

| 平台 | 构建 | 镜像启动 | 安装/权限 | 交互/并存/升级卸载 | 当前结论 |
| --- | --- | --- | --- | --- | --- |
| macOS arm64 | 未测 | 未测 | 未测 | 未测 | 不可声明已支持 |
| macOS x64（条件） | 本机 `createDistributable` 通过 | 隔离 profile 进程存活 | 未做安装器/公证 | 未测 | 开发机可用，非正式支持 |
| Windows x64 | 未测 | 未测 | 未测 | 未测 | 不可声明已支持 |
| Linux x64 X11 | 未测 | 未测 | 未测 | 未测 | 不可声明已支持 |
| Linux x64 Wayland | 未测 | 未测 | 未测 | 未测 | 单列 portal/窗口限制 |

视觉截图本轮未取得（无窗口截图权限）。不以设计稿代替。

## 7. 证据记录模板

后续每阶段建立 `docs/evidence/YYYY-MM-DD-阶段/`。见 `docs/evidence/2026-09-09-p0-p1/`、`docs/evidence/2026-09-09-f18/`、`docs/evidence/2026-09-09-f21/`、`docs/evidence/2026-09-09-f13/`、`docs/evidence/2026-09-09-f12/`、`docs/evidence/2026-09-09-f15/`、`docs/evidence/2026-09-09-f16/`、`docs/evidence/2026-09-09-f02/`、`docs/evidence/2026-09-09-f03/`。

## 8. 完成定义

阶段完成：退出条件满足并有证据；缺少条件的部分继续保留未验收。整个产品完成：F00–F25/A01–A03 的必要范围落实，平台发布矩阵与实际支持一致，安装/升级/卸载隔离真实通过，文档与软件状态一致。

当前交付是可运行的 0.1.0 开发切片，不是完整产品验收。
