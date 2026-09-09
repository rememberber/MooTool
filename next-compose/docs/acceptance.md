# 验收标准、进度与证据

> 更新：2026-09-09。P0/P1/JSON 之后已接入 F02 文本对比、F03 格式化、F06 配置转换、F07 Protobuf、F12 UA、F13 编码、F14 加解密、F15 正则、F16 Cron、F17 二维码、F18 时间、F19 留言板、F21 计算器、F22 调色板、F23 图片助手、F24 PDF、F25 系统信息、F11 网络/IP、F08 环境变量、F10 Host、F09 HTTP、F20 翻译、F05 代码运行；完整产品与三平台发行仍未验收。

## 1. 状态规则

状态只使用：未开始、开发中、待验收、已验收、明确差异。已验收必须有具体证据；明确差异必须有 DIFF 记录并说明它影响哪些用例，不能用它隐藏尚未完成的编码。

功能拆分验收至少包括：控件/入口、正常流程、错误/取消、状态/持久化、语义 fixture、视觉/键盘、平台能力。某工具只有格式化或默认路径完成，应在备注列写具体范围，不能将整行设为已验收。

## 2. 当前进度

| 条目 | 目标 | 当前状态 | 证据/差异 |
| --- | --- | --- | --- |
| P0 | 工具链/编辑器/窗口/动态 proto 等实验 | 开发中 | 本机 Wrapper/JDK21/Compose1.12 构建与 app-image 启动见 `docs/evidence/2026-09-09-p0-p1/`。RSTA 已接入，IME/列编辑未做桌面交互验收。protoc 4.29.3 已随 F07 按 OS/arch 捆绑。ADR-001/002/003 |
| P1 | 桌面壳/搜索/设置基础 | 开发中 | 26 入口、搜索、modern 明暗、语言、基础设置、JSON 分离窗口代码已有；视觉截图与完整键盘流程待验收 |
| P2 | 完整 JSON 基础工作流 | 开发中 | 仅最小切片：格式化/压缩/查找/历史/Vault CRUD/转换。Git、冲突监视、完整检查器弹层未完成，**不能标 F04 已验收** |
| P3 | 文本与本地算法 | 开发中 | F02/F03/F06/F07/F12/F13/F15/F16/F18/F21 已有引擎单测与 UI；F04 Git 仍未做 |
| P4 | 媒体/加密 | 开发中 | F14/F17/F19/F22/F23/F24 已有引擎单测与 UI；截图权限、WebP、安装镜像未测 |
| P5 | 网络/系统 | 开发中 | F25 系统信息、F11 网络/IP、F08 环境变量、F10 Host、F09 HTTP、F20 翻译已有引擎单测与 UI；P5 引擎层闭环，截图/真实联网未测 |
| P6 | 文档/Git/运行台/备份 | 开发中 | F05 代码运行待验收；F01 随手记、Git、备份未开始 |
| P7 | 完整产品/平台安装发行验收 | 未开始 | — |
| F00 | 首页 | 待验收 | 已实现 Compose 品牌/0.1.0/链接；无运行截图 |
| F01 | 随手记 | 未开始 | 入口显示尚未实现 |
| F02 | 文本对比 | 待验收 | 行/字符 Myers 差异、三种高亮、忽略空白、统一补丁、上/下差异、导入/复制/清空/交换、历史与分离窗口。与 Electron `diff` 样本对齐。无运行截图 |
| F03 | 格式化 | 待验收 | 文本/文件 Tab，Nginx/Java/XML/HTML，缩进 2–6，真实解析格式化、语法错误定位、另存不覆盖原文件、历史与分离窗口、Cmd/Ctrl+Shift+F。引擎差异见 [DIFF-005](diff/005-reformat-jvm.md)。无运行截图 |
| F04 | JSON | 开发中 | 算法 7 项单测通过；UI 切片已能启动；非完整 F04 |
| F05 | 代码运行 | 待验收 | 三 Tab + Java/Groovy、独立草稿、检测/手动路径、真运行停止与流式输出、参数 argv、超时/取消/截断、历史与分离窗口。本机 Java/Python/Node 打印 42；Groovy 未安装标明不可用。差异见 [DIFF-020](diff/020-code-run-processbuilder.md)。无运行截图；安装镜像与关闭应用杀树手工未测 |
| F06 | 配置转换 | 待验收 | Properties ↔ YAML、点路径/`[index]`、标量列表逗号合并、YAML 校验/格式化、导入导出、历史与分离窗口。类型冲突显式报错。差异见 [DIFF-006](diff/006-config-snakeyaml.md)。无运行截图 |
| F07 | Protobuf | 待验收 | 捆绑 protoc 4.29.3 + DynamicMessage；JSON↔Hex/Base64、Wire 无 schema、定义格式化、nested/map/oneof/int64 单测、历史与分离窗口。差异见 [DIFF-007](diff/007-protobuf-jsonformat.md)。无运行截图；安装镜像内解出未测 |
| F08 | 环境变量 | 待验收 | 用户/系统/进程作用域、JVM 运行时 Tab、搜索/刷新/复制/导出、改前 diff 与备份、无权限保持原文件、无通用历史、分离窗口。用户路径为本产品 `data/environment`。差异见 [DIFF-016](diff/016-environment-compose-namespace.md)。无运行截图；系统提权对话框、Windows 注册表、新终端读取未测 |
| F09 | HTTP | 待验收 | 左集合/右 Method+URL+发送取消，Params/Headers/Cookies/Body，响应 Body/Headers/Cookies，冻结 GET/表单语义，cURL 只解析，4xx 真实正文，10 MiB 上限，超时/取消，集合 JSON，历史与分离窗口。差异见 [DIFF-018](diff/018-http-okhttp-repeat-headers.md)。无运行截图；外网、自签证书、代理对话框、响应另存、安装镜像未测 |
| F10 | Host | 待验收 | 左方案列表/右编辑，方案 CRUD 与导入导出、查找替换、查看系统 hosts、应用前 diff/备份/指纹冲突、无权限保持原文件、备份恢复、历史与分离窗口。方案在本产品 `data/hosts/profiles.json`。差异见 [DIFF-017](diff/017-host-compose-profiles.md)。无运行截图；真实 `/etc/hosts`、管理员对话框、Windows hosts、安装镜像未测 |
| F11 | 网络/IP | 待验收 | 左输出/右功能区，IPv4↔Long fixture、ping/网段/端口扫描/DNS/WHOIS/本机地址、argv 进程、流式输出可停止、历史与分离窗口。失败显示真原因。差异见 [DIFF-015](diff/015-net-process-charset.md)。无运行截图；WHOIS 在线、中文 Windows ping 编码、安装镜像未测 |
| F12 | UA | 待验收 | 预设 Chrome/Safari/Firefox/iPhone/Android、Googlebot、空/未知；版本与设备字段有单测。引擎推断见 [DIFF-002](diff/002-ua-engine-inference.md)。无运行截图 |
| F13 | 编码解码 | 待验收 | 引擎覆盖 Unicode/emoji、URL UTF-8 与 GB2312 往返、Hex/ASCII、非法 Hex、截断字节、GB2312 不可映射拒绝问号。UI 含四分区、历史、分离窗口。无运行截图。安装镜像需 `jdk.charsets`（已加入 jlink modules） |
| F14 | 加解密/随机 | 待验收 | AES/DES/SM4 ECB PKCS7 Hex、RSA/SM2 加解密签名、Electron 样本可消费、摘要/Base64/随机、历史与分离窗口。非 ASCII 密钥拒绝。差异见 [DIFF-008](diff/008-crypto-key-bytes.md)。无运行截图；超大文件与哈希中途取消未测 |
| F15 | 正则 | 待验收 | Java Pattern + 独立 worker（2s/10000 上限）；21 条常用模式、flags、捕获/命名组、零宽前进、非法模式保留原文、收藏 JSON 重启恢复、历史与分离窗口。引擎差异见 [DIFF-003](diff/003-regex-java-pattern.md)。无运行截图 |
| F16 | Cron | 待验收 | Quartz 6/7 字段、预设、IANA 时区、未来 10 次、年过滤、闰日、收藏与历史、分离窗口。与 Electron cron-parser 差异见 [DIFF-004](diff/004-cron-quartz.md)。无运行截图 |
| F17 | 二维码 | 待验收 | ZXing 生成/识别 PNG，纠错 L/M/Q/H、尺寸 120–2000、Logo、文件与剪贴板、中文往返、历史与分离窗口。历史不存 PNG。差异见 [DIFF-009](diff/009-qr-history-png.md)。无运行截图；剪贴板手工往返未测 |
| F18 | 时间 | 待验收 | 引擎单测覆盖 epoch/负值/毫秒/DST/闰年/显式单位；UI 含双向转换、时区、历史、大屏时钟、分离窗口。无运行截图。单位语义见 [DIFF-001](diff/001-time-explicit-unit.md) |
| F19 | 留言板 | 待验收 | 80 字 UTF-16、8 预设、6 主题、左/居中、字号 70–130 自动适配、会话恢复、沉浸展示 Esc 退出、唤醒 token。无通用历史。差异见 [DIFF-011](diff/011-message-board-wake.md)。无运行截图；显示器熄屏未测 |
| F20 | 翻译 | 待验收 | 翻译/单词本/历史 Tab，源/目标语言、Google/Bing、交换、自动 500ms debounce 与手动立即发，过期响应丢弃，回填不重复请求，分段/并发保序、fallback 与 10 分钟冷却、取消/超时，单词本 CRUD/搜索/重译，历史最多 500 条 JSON，无通用历史、分离窗口。差异见 [DIFF-019](diff/019-translation-okhttp-json.md)。无运行截图；真实 Google/Bing 联网、代理对话框、安装镜像未测 |
| F21 | 计算器 | 待验收 | 引擎单测覆盖 `2*(3+4)=14`、负数、进制、GCD/LCM、排列组合与非法输入；UI 含等号计算、结果复制、会话与历史。无运行截图。表达式按 IEEE Double 再按 14 位有效数字展示，与 Electron 一致，未改用任意精度小数 |
| F22 | 调色板 | 待验收 | HEX/RGB 往返、7 主题 + 10 标准色 SHA-256、五运算、主色/对比色、Shift 选对比色、Robot 冻结截图取色、JColorChooser、文件夹收藏、历史与分离窗口。差异见 [DIFF-010](diff/010-color-screen-picker.md)。无运行截图；多屏/录屏权限对话框未测 |
| F23 | 图片 | 待验收 | 图片库文件持久化、导入/剪贴板/Base64、压缩与水印、ImageTracer SVG path、区域截图拒绝全黑、缩放/适应、历史与分离窗口。差异见 [DIFF-013](diff/013-imagetracer-svg.md)。无运行截图；多屏权限对话框、WebP、超 16MP、安装镜像未测 |
| F24 | PDF | 待验收 | 拆分/合并 Tab、最多 20 项、奇偶/自定义页码、token 顺序去重、`_split.pdf` 覆盖写出、合并保存对话框、历史与分离窗口、取消删除半成品。差异见 [DIFF-012](diff/012-pdfbox-import-page.md)。无运行截图；加密样本、表单/书签/签名、安装镜像未测 |
| F25 | 系统信息 | 待验收 | 系统/CPU/内存/存储/网络 Tab、OSHI 真机采集、序列号默认遮蔽、JVM 与 OS 分区、复制当前 Tab、切走取消采集、无通用历史、分离窗口。差异见 [DIFF-014](diff/014-oshi-system-info.md)。无运行截图；安装镜像 JNA 未测 |
| A01 | 11 类设置 | 开发中 | general/appearance/layout/editor/data/about/runtime 基础项生效；Network/Vault/Tools/Shortcuts 明确未实现 |
| A02 | 历史/收藏/搜索 | 开发中 | JSON、编码、UA、正则、Cron、文本对比、格式化、配置转换、Protobuf、加解密、二维码、调色板、时间转换、计算器、PDF、图片助手、网络/IP、Host 应用、HTTP 发送与代码运行历史已有；环境变量/系统信息/留言板/翻译无通用历史（翻译用自有单词本与历史）；正则/Cron/调色板收藏已落地 |
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

本机 2026-09-09 结果：F05 接入后 `desktopTest` **116/116** 通过（含 CodeRunEngineTest 4；此前 F20 为 112/112）。`createDistributable` 此前生成 `MooTool Next Compose.app`；本轮未重跑打包。`runDistributable` 与 `packageDistributionForCurrentOS` 未跑完。

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

后续每阶段建立 `docs/evidence/YYYY-MM-DD-阶段/`。见 `docs/evidence/2026-09-09-p0-p1/`、`docs/evidence/2026-09-09-f18/`、`docs/evidence/2026-09-09-f21/`、`docs/evidence/2026-09-09-f13/`、`docs/evidence/2026-09-09-f12/`、`docs/evidence/2026-09-09-f15/`、`docs/evidence/2026-09-09-f16/`、`docs/evidence/2026-09-09-f02/`、`docs/evidence/2026-09-09-f03/`、`docs/evidence/2026-09-09-f06/`、`docs/evidence/2026-09-09-f07/`、`docs/evidence/2026-09-09-f14/`、`docs/evidence/2026-09-09-f17/`、`docs/evidence/2026-09-09-f22/`、`docs/evidence/2026-09-09-f19/`、`docs/evidence/2026-09-09-f24/`、`docs/evidence/2026-09-09-f23/`、`docs/evidence/2026-09-09-f25/`、`docs/evidence/2026-09-09-f11/`、`docs/evidence/2026-09-09-f08/`、`docs/evidence/2026-09-09-f20/`、`docs/evidence/2026-09-09-f09/`、`docs/evidence/2026-09-09-f05/`。

## 8. 完成定义

阶段完成：退出条件满足并有证据；缺少条件的部分继续保留未验收。整个产品完成：F00–F25/A01–A03 的必要范围落实，平台发布矩阵与实际支持一致，安装/升级/卸载隔离真实通过，文档与软件状态一致。

当前交付是可运行的 0.1.0 开发切片，不是完整产品验收。
