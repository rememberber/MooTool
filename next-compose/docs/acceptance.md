# 验收标准、进度与证据

> 更新：2026-09-15。对照 Electron 的缺口见 `docs/evidence/2026-09-14-parity-gap/`。本轮补上 DIFF-052 至应用内遮罩（DIFF-057）与历史/Git 遮罩避开 Swing 编辑器（DIFF-058）。完整产品与三平台发行仍未验收。

## 1. 状态规则

状态只使用：未开始、开发中、待验收、已验收、明确差异。已验收必须有具体证据；明确差异必须有 DIFF 记录并说明它影响哪些用例，不能用它隐藏尚未完成的编码。

功能拆分验收至少包括：控件/入口、正常流程、错误/取消、状态/持久化、语义 fixture、视觉/键盘、平台能力。某工具只有格式化或默认路径完成，应在备注列写具体范围，不能将整行设为已验收。

## 2. 当前进度

| 条目 | 目标 | 当前状态 | 证据/差异 |
| --- | --- | --- | --- |
| P0 | 工具链/编辑器/窗口/动态 proto 等实验 | 开发中 | 本机 Wrapper/JDK21/Compose1.12 构建与 app-image 启动见 `docs/evidence/2026-09-09-p0-p1/`。RSTA 已接入；列编辑引擎、Alt 拖选、IME 提交写入列选择、预编辑不落盘已落地；`JFrame` 派发单测已过，窗口手势未做手工验收。5 MiB 中英混排 `setText` 单测已过。protoc 4.29.3 已随 F07 按 OS/arch 捆绑。ADR-001/002/003 |
| P1 | 桌面壳/搜索/设置基础 | 开发中 | 26 入口、侧栏搜索按钮 + Cmd/Ctrl+K 命令盘（分组名/方向键/关闭）、六风格 token+装饰、折叠导航 tooltip、线性图标、语言、基础设置、JSON 分离窗口、AWT 托盘（不支持时说明）、工具栏密度 token 已有；侧栏 ⧉/▣ 与右键分离/收回，打开已分离工具聚焦既有窗口并显示占位；窗口管理器压到 960×640 以下显示最小尺寸提示；启动时按显示器工作区夹紧已保存坐标，标题条不可见则夹回最近屏。视觉窗口截图与完整键盘流程待验收。见 [DIFF-042](diff/042-detach-focus-minsize.md)、[DIFF-044](diff/044-window-copy-image-git.md)、[DIFF-056](diff/056-command-palette-sidebar-search.md) |
| P2 | 完整 JSON 基础工作流 | 开发中 | 格式化/压缩/查找/历史搜索删除/Vault CRUD/转换 + Git + 检查器/JSONPath filter-slice-union + 树拖放 + 分栏持久化。运行窗口见 `30-json.png` / 浅色 `65-json-light.png`。IME/列编辑手势未手工验收，**不能标 F04 已验收** |
| P3 | 文本与本地算法 | 开发中 | F02/F03/F06/F07/F12/F13/F15/F16/F18/F21 已有引擎单测与 UI |
| P4 | 媒体/加密 | 开发中 | F14/F17/F19/F22/F23/F24 已有引擎单测与 UI；macOS 录屏用途已写入 Info.plist，TCC 对话框手工、WebP、安装镜像未测 |
| P5 | 网络/系统 | 开发中 | F25 系统信息、F11 网络/IP、F08 环境变量、F10 Host、F09 HTTP、F20 翻译已有引擎单测与 UI；运行窗口已取，真实联网/提权对话框未测 |
| P6 | 文档/Git/运行台/备份 | 开发中 | F05 待验收；F01 文档库工作流已落地；Git 含 pull/push/丢弃/冲突与自动检查点/自动 pull；IME/窗口手势未做手工验收 |
| P7 | 完整产品/平台安装发行验收 | 开发中 | 代码已声明 DMG/MSI/DEB/RPM、Windows per-user 与 UpgradeCode、macOS Developer Tools 类别与 Info.plist（含 `NSScreenCaptureUsageDescription`）、卸载隔离名单，见 [DIFF-030](diff/030-layout-theme-editor.md)、[DIFF-033](diff/033-shell-theme-editor-packaging.md)、[DIFF-052](diff/052-json-inspector-screencapture-chrome.md)。三平台安装/升级/卸载/公证未测，不可标已支持 |
| F00 | 首页 | 待验收 | 已实现 Compose 品牌/0.1.0/链接；分区为标题+卡片。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/21-home.png`，浅色见 `64-home-light.png`。[DIFF-052](diff/052-json-inspector-screencapture-chrome.md)。其余视觉/键盘未签 |
| F01 | 随手记 | 待验收 | 左库/中编辑或预览/右快速替换、保存与切换写入、查找替换、历史搜索删除与分离窗口。编辑/分栏/预览三模式；commonmark 预览；附件；逻辑行列编辑（Alt+拖动或闩锁）；Vault Git；外部冲突；YAML frontmatter；路径/标题/正文检索；目录 CRUD/复制/移动；树拖放到目录；拖入编辑器；分栏宽度按工具保存；按语法格式化（含 SQL 方言）。工具栏可改当前笔记字体/字号/语法/颜色/行距，以及无序/有序列表前缀；颜色会着色文件树；行距改变编辑器行高。文档库可按最近修改（默认）/创建时间/名称排序并写入会话；展开模式与 JSON 库分开，侧栏可展开/折叠全部；方向键展开折叠并打开文件。右键菜单对目标节点执行重命名/移动/复制/导出/删除/在文件管理器显示/Git。搜索关键字过滤内存索引，不在每个字同步扫盘。内容宽 < 960 时文档库与快速替换按需互斥切换；内容宽 < 1440 时字号/行距/列编辑/图片/导入导出/历史/Git/分离收入「更多」。Cmd/Ctrl+F 打开查找，Cmd/Ctrl+S 保存，Cmd/Ctrl+Shift+F 格式化（编辑器焦点下走 RSTA InputMap）。默认 `data/vaults/quick-note`。差异见 [DIFF-021](diff/021-quick-note-replace-vault.md) 至 [DIFF-044](diff/044-window-copy-image-git.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/23-quicknote.png`；IME/列模式窗口手势未手工验收，不能标完整 F01 |
| F02 | 文本对比 | 待验收 | 行/字符 Myers 差异、三种高亮、忽略空白、独立滚动同步（短侧夹紧不回拉长侧）、统一补丁、上/下差异、导入/复制/清空/交换、历史与分离窗口。内容宽 < 1440 时清空/交换/复制/导入/历史/分离收入「更多」。与 Electron `diff` 样本对齐。见 [DIFF-043](diff/043-diff-sync-git-preview.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/01-diff.png`；同步滚动窗口手势未手工验收 |
| F03 | 格式化 | 待验收 | 文本/文件 Tab，Nginx/Java/XML/HTML 与缩进 2–6 用下拉（对照 Electron compact select），真实解析格式化、语法错误定位、另存不覆盖原文件、历史与分离窗口、Cmd/Ctrl+Shift+F。内容宽 < 1440 时复制/保存/清空收入「更多」，标题栏历史/分离同样收入「更多」；复制按钮短暂反馈。引擎差异见 [DIFF-005](diff/005-reformat-jvm.md)、[DIFF-045](diff/045-reformat-color-overflow.md)、[DIFF-051](diff/051-settings-chrome-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/40-reformat.png` |
| F04 | JSON | 开发中 | 检查器侧栏对照 Electron：顶栏关闭、缩进 2/4 分段、格式开关、转换两列网格、类名在转换区、JSONPath 后再到结果（路径树保留为额外能力）+ JSONPath 弹层（单击预览/双击查询）+ filter/slice/union/`$..`/转义键单测 + 转换结果对话框 + Vault Git + 目录 CRUD/重命名/复制/内容检索 + 树拖放/展开全部按钮 + 名称（默认）/修改时间排序写入会话 + 搜索走内存索引 + 文档库方向键 + 右键菜单（重命名/移动/复制/导出/删除/显示/Git）+ 分栏持久化 + 列编辑 + 历史搜索删除 + JSON 工具栏字体选择（系统字体列表写入 `editor.jsonFontName`）。复制成功按钮短暂显示「已复制」，失败显示「复制失败」，约 1400ms 后恢复。Cmd/Ctrl+F 查找、Cmd/Ctrl+Shift+F 格式化、Cmd/Ctrl+S 保存当前 Vault 文件（编辑器焦点下走 RSTA InputMap）。内容宽 < 960 时 Vault/检查器按需互斥切换；内容宽 < 1440 时换行/列编辑/导入导出/历史/Git/检查器/分离收入「更多」，≥ 1440 仍用「更多工具」开关检查器。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/30-json.png`，拆出窗见 `31-json-detached.png`，浅色见 `65-json-light.png`，JSONPath 全宽见 `45-json-path-fullwidth.png`。IME/列编辑手势未手工验收，不能标 F04 已验收。见 [DIFF-025](diff/025-git-cli-local-checkpoint.md) 至 [DIFF-052](diff/052-json-inspector-screencapture-chrome.md) |
| F05 | 代码运行 | 待验收 | 三 Tab + Java/Groovy、独立草稿、检测/手动路径、真运行停止与流式输出、参数 argv、超时/取消/截断、历史与分离窗口。输出增长默认跟随尾部，用户上翻后暂停，新一次运行重新跟尾。内容宽 < 1440 时检测/选项/格式化/清空/历史/分离收入「更多」，保留运行/停止。本机 Java/Python/Node 打印 42；Groovy 未安装标明不可用。差异见 [DIFF-020](diff/020-code-run-processbuilder.md)、[DIFF-040](diff/040-follow-tail-http-overflow.md)、[DIFF-048](diff/048-http-binary-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/41-code-run.png`；安装镜像与关闭应用杀树手工未测 |
| F06 | 配置转换 | 待验收 | Properties ↔ YAML、点路径/`[index]`、标量列表逗号合并、YAML 校验/格式化、导入导出、历史与分离窗口。类型冲突显式报错。内容宽 < 1440 时历史/清空/分离收入「更多」。差异见 [DIFF-006](diff/006-config-snakeyaml.md)、[DIFF-048](diff/048-http-binary-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/42-yml.png` |
| F07 | Protobuf | 待验收 | 捆绑 protoc 4.29.3 + DynamicMessage；JSON↔Hex/Base64、Wire 无 schema、定义格式化、nested/map/oneof/int64 单测、历史与分离窗口。内容宽 < 1440 时历史/复制/分离收入「更多」。差异见 [DIFF-007](diff/007-protobuf-jsonformat.md)、[DIFF-048](diff/048-http-binary-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/43-protobuf.png`；安装镜像内解出未测 |
| F08 | 环境变量 | 待验收 | 用户/系统/进程作用域、JVM 运行时 Tab、搜索/刷新/复制/导出、改前 diff 与备份、无权限保持原文件、无通用历史、分离窗口。用户路径为本产品 `data/environment`。内容宽 < 1440 时导出/分离收入「更多」，保留新增/刷新。差异见 [DIFF-016](diff/016-environment-compose-namespace.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/44-env.png`；系统提权对话框、Windows 注册表、新终端读取未测 |
| F09 | HTTP | 待验收 | 左集合/右 Method 下拉+URL+发送取消，Params/Headers/Cookies/Body（Body MIME 下拉），响应 Body/Headers/Cookies，冻结 GET/表单语义，cURL 只解析，4xx 真实正文，10 MiB 上限，超时/取消，集合 JSON，历史搜索删除与分离窗口，当前响应 Tab 可另存文件。二进制响应保留原始字节，Body 页另存写出这些字节（预览为十六进制摘要）。发送中以及取消/超时/网络失败时保留上次可用响应并标明「上次响应」。请求区与响应区可拖动分隔条并写入分栏高度。请求 Body 与响应正文为 `EditorHost` 语法高亮（JSON/XML/HTML/JS，响应只读）；Cmd/Ctrl+Enter 发送；Cmd/Ctrl+F 打开响应查找（只查找不替换，不搜占位文案，命中落到 RSTA 选区）；Cmd/Ctrl+Shift+F 在 Body 页格式化正文；Escape 关闭查找。复制按钮 1400ms 反馈。内容宽 < 1440 时另存/历史/分离收入「更多」。历史只存 URL（basic-auth 密码遮蔽）与响应正文摘要，不存 Header。差异见 [DIFF-018](diff/018-http-okhttp-repeat-headers.md) 至 [DIFF-050](diff/050-http-editorhost.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/24-http.png`；外网、自签证书、代理对话框、安装镜像未测 |
| F10 | Host | 待验收 | 左方案列表/右编辑，方案 CRUD 与导入导出、查找替换、查看系统 hosts、应用前 diff/备份/指纹冲突、无权限保持原文件、备份恢复、历史与分离窗口。方案列表右键可重命名/复制/导出/删除。方案在本产品 `data/hosts/profiles.json`。内容宽 < 1440 时历史/分离/当前 hosts/查找收入「更多」，保留名称/保存/应用。差异见 [DIFF-017](diff/017-host-compose-profiles.md)、[DIFF-041](diff/041-context-menu-panes.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/45-host.png`；真实 `/etc/hosts`、管理员对话框、Windows hosts、安装镜像未测 |
| F11 | 网络/IP | 待验收 | 左输出/右功能区，IPv4↔Long fixture、ping/网段/端口扫描/DNS/WHOIS/本机地址、argv 进程、流式输出可停止并默认跟尾（上翻暂停），历史与分离窗口。失败显示真原因。内容宽 < 1440 时历史/分离/复制/清空收入「更多」，保留 ifconfig/netstat 与停止。差异见 [DIFF-015](diff/015-net-process-charset.md)、[DIFF-040](diff/040-follow-tail-http-overflow.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/46-net.png`；WHOIS 在线、中文 Windows ping 编码、安装镜像未测 |
| F12 | UA | 待验收 | 预设 Chrome/Safari/Firefox/iPhone/Android、Googlebot、空/未知；版本与设备字段有单测。内容宽 < 1440 时历史/分离收入「更多」。引擎推断见 [DIFF-002](diff/002-ua-engine-inference.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/47-ua.png` |
| F13 | 编码解码 | 待验收 | 引擎覆盖 Unicode/emoji、URL UTF-8 与 GB2312 往返、Hex/ASCII、非法 Hex、截断字节、GB2312 不可映射拒绝问号。UI 含四分区、历史、分离窗口。URL 字符集与 ASCII 进制为下拉（对照 Electron compact select）；内容宽 < 1440 时历史/清空/分离收入「更多」。见 [DIFF-047](diff/047-encode-crypto-select.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/48-encode.png`。安装镜像需 `jdk.charsets`（已加入 jlink modules） |
| F14 | 加解密/随机 | 待验收 | AES/DES/SM4 ECB PKCS7 Hex、RSA/SM2 加解密签名、Electron 样本可消费、摘要/Base64/随机、历史与分离窗口。对称/非对称/摘要/Base 算法为下拉。私钥操作输入与生成密码输出写入历史时遮蔽。非 ASCII 密钥拒绝。内容宽 < 1440 时历史/分离收入「更多」。差异见 [DIFF-008](diff/008-crypto-key-bytes.md)、[DIFF-033](diff/033-shell-theme-editor-packaging.md)、[DIFF-047](diff/047-encode-crypto-select.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/49-crypto.png`；超大文件与哈希中途取消未测 |
| F15 | 正则 | 待验收 | Java Pattern + 独立 worker（2s/10000 上限）；21 条常用模式、flags、捕获/命名组、零宽前进、非法模式保留原文、收藏 JSON 重启恢复（命名/分组/查询）、历史搜索删除与分离窗口。内容宽 < 1440 时收藏/历史/分离收入「更多」。引擎差异见 [DIFF-003](diff/003-regex-java-pattern.md)、[DIFF-048](diff/048-http-binary-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/50-regex.png` |
| F16 | Cron | 待验收 | Quartz 6/7 字段、预设、IANA 时区、未来 10 次、年过滤、闰日、收藏（命名/分组/查询）与历史搜索、分离窗口。内容宽 < 1440 时收藏/历史/分离收入「更多」。与 Electron cron-parser 差异见 [DIFF-004](diff/004-cron-quartz.md)、[DIFF-048](diff/048-http-binary-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/51-cron.png` |
| F17 | 二维码 | 待验收 | ZXing 生成/识别 PNG，纠错 L/M/Q/H 为下拉、尺寸 120–2000、Logo、文件与剪贴板、中文往返、History Tab 搜索/删除/清空与分离窗口。历史不存 PNG。内容宽 < 1440 时分离收入「更多」。差异见 [DIFF-009](diff/009-qr-history-png.md)、[DIFF-032](diff/032-history-settings-sql.md)、[DIFF-051](diff/051-settings-chrome-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/52-qr.png`；剪贴板手工往返未测 |
| F18 | 时间 | 待验收 | 引擎单测覆盖 epoch/负值/毫秒/DST/闰年/显式单位；UI 含双向转换、时区、秒/毫秒下拉、历史、大屏时钟、分离窗口。内容宽 < 1440 时历史/分离收入「更多」，保留时钟。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/53-time.png`。单位语义见 [DIFF-001](diff/001-time-explicit-unit.md)、[DIFF-051](diff/051-settings-chrome-overflow.md) |
| F19 | 留言板 | 待验收 | 80 字 UTF-16、8 预设、6 主题、左/居中对齐下拉、字号 70–130 自动适配、会话恢复、沉浸展示 Esc 退出、唤醒 token。无通用历史。内容宽 < 1440 时分离收入「更多」。差异见 [DIFF-011](diff/011-message-board-wake.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/54-message.png`；显示器熄屏未测 |
| F20 | 翻译 | 待验收 | 翻译/单词本/历史 Tab，源/目标语言、Google/Bing 下拉、交换、自动 500ms debounce 与手动立即发，过期响应丢弃，回填不重复请求，分段/并发保序、fallback 与 10 分钟冷却、取消/超时，单词本 CRUD/搜索/重译，历史最多 500 条 JSON，无通用历史、分离窗口。内容宽 < 1440 时复制/存单词/清空/分离收入「更多」，保留语言/交换/自动/立即。差异见 [DIFF-019](diff/019-translation-okhttp-json.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/55-translation.png`；真实 Google/Bing 联网、代理对话框、安装镜像未测 |
| F21 | 计算器 | 待验收 | 引擎单测覆盖 `2*(3+4)=14`、负数、进制、GCD/LCM、排列组合与非法输入；UI 含等号计算、结果复制、会话与历史。内容宽 < 1440 时历史/复制/分离收入「更多」。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/56-calculator.png`。表达式按 IEEE Double 再按 14 位有效数字展示，与 Electron 一致，未改用任意精度小数。见 [DIFF-048](diff/048-http-binary-overflow.md) |
| F22 | 调色板 | 待验收 | HEX/RGB 往返、7 主题 + 10 标准色 SHA-256、五运算、主色/对比色、Shift 选对比色、Robot 冻结截图取色、JColorChooser、文件夹收藏、历史与分离窗口。内容宽 < 1440 时复制/收藏/收藏夹/历史/分离收入「更多」；复制按钮短暂反馈。macOS plist 已声明录屏用途，权限失败触发 TCC/`screencapture` 与系统设置，见 [DIFF-010](diff/010-color-screen-picker.md)、[DIFF-045](diff/045-reformat-color-overflow.md)、[DIFF-051](diff/051-settings-chrome-overflow.md)、[DIFF-052](diff/052-json-inspector-screencapture-chrome.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/57-color.png`；多屏/录屏权限对话框手工未测 |
| F23 | 图片 | 待验收 | 图片库文件持久化、导入/剪贴板/Base64、压缩与水印、ImageTracer SVG path、区域截图拒绝全黑、缩放/适应、历史搜索删除与分离窗口。内容宽 < 1440 时剪贴板/Base64/SVG/压缩/水印/历史/分离收入「更多」，保留列表开关/截图/导入/保存/复制。macOS 录屏用途声明与权限失败路径见 [DIFF-013](diff/013-imagetracer-svg.md)、[DIFF-032](diff/032-history-settings-sql.md)、[DIFF-044](diff/044-window-copy-image-git.md)、[DIFF-052](diff/052-json-inspector-screencapture-chrome.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/58-image.png`；多屏权限对话框手工、WebP、超 16MP、安装镜像未测 |
| F24 | PDF | 待验收 | 拆分/合并 Tab、最多 20 项、奇偶/自定义页码、token 顺序去重、`_split.pdf` 覆盖写出、合并保存对话框、历史搜索删除与分离窗口、取消删除半成品。内容宽 < 1440 时历史/帮助/分离收入「更多」。差异见 [DIFF-012](diff/012-pdfbox-import-page.md)、[DIFF-032](diff/032-history-settings-sql.md)、[DIFF-048](diff/048-http-binary-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/59-pdf.png`；加密样本、表单/书签/签名、安装镜像未测 |
| F25 | 系统信息 | 待验收 | 系统/CPU/内存/存储/网络 Tab、OSHI 真机采集、序列号默认遮蔽、JVM 与 OS 分区、复制当前 Tab、切走取消采集、无通用历史、分离窗口。内容宽 < 1440 时复制/分离收入「更多」，保留刷新。差异见 [DIFF-014](diff/014-oshi-system-info.md)、[DIFF-049](diff/049-host-net-ua-overflow.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/60-hardware.png`；安装镜像 JNA 未测 |
| A01 | 11 类设置 | 开发中 | general/appearance/layout/editor/data/about/runtime/network/tools/shortcuts 均有真实字段；设置页按 Electron 分组/分段/下拉/开关/色板布局，见 [DIFF-051](diff/051-settings-chrome-overflow.md)；统一背景、界面字体、SQL 方言、JSON/随手记字体字号（设置页与 JSON 工具栏同一系统字体列表）、JSON 与随手记 Vault 树展开各自独立、托盘开关、软换行默认会改变运行结果；自定义分组 CRUD 与侧栏拖宽已落地；管理器为独立弹层（草稿/保存/取消、开关勾选、空名/空组校验、删除确认），见 [DIFF-053](diff/053-custom-group-switches.md)、[DIFF-055](diff/055-custom-group-dialog.md)；六风格含渐变/inset/选中条、工作区径向高光与侧栏过渡与对比度单测，hero/miui/claude 色板对齐 Electron CSS 变量，强调色对齐 yellow/coral/blue/green/red/purple，仍非逐选择器 CSS 皮肤；线性图标已替换字形；折叠导航有完整名称 tooltip；About 展示安装身份与卸载隔离路径；Shortcuts 冲突时字段旁显示原因且不写入；帮助列出 JSON/随手记查找格式化保存、HTTP 发送/查找与 Body 格式化；主窗口与拆出窗口最小 960×640，窗口管理器强制更小时显示提示且不截掉保存/退出；启动时夹紧已保存窗口坐标；内容宽 < 1440 时 JSON/随手记/文本对比/图片/格式化/调色板/HTTP 另存/编码/加解密/正则/Cron/PDF/运行台/计算器/配置/Protobuf/Host/网络/UA/翻译/环境/系统/留言板/二维码/时间低频工具栏收入「更多」；拆出窗口跟随当前风格；编辑器使用主题 workspace 与语法色；工具栏使用 `dimens.toolbar`。见 [DIFF-032](diff/032-history-settings-sql.md) 至 [DIFF-056](diff/056-command-palette-sidebar-search.md)。运行窗口见 `docs/evidence/2026-09-15-inspector-screencapture/windows/25-settings.png`，布局/自定义分组见 `62-settings-layout.png` 与 `66-settings-custom-groups.png`，浅色外观见 `63-settings-light.png`；焦点环 Compose 聚焦像素见 `docs/evidence/2026-09-15-tray-density/captures/compact-960-focus-ring.png`，**产品主窗 Tab 走查未拍**；六套 CSS 皮肤未验收 |
| A02 | 历史/收藏/搜索 | 开发中 | 声明通用历史的工具均接到 `HistoryBrowser`（搜索/恢复/删除/清空）；二维码 History Tab 可搜可删；图片/PDF 有历史入口。环境变量/系统信息/留言板/翻译无通用历史（翻译用自有单词本与历史）；正则/Cron 收藏含分组查询，调色板收藏可搜索；命令搜索可从侧栏 `⌕` 或 Cmd/Ctrl+K 打开，结果显示分组名，输入框内方向键有效，遮罩点空白关闭（应用内 overlay，非系统半透明 Dialog），已分离工具走占位并聚焦既有窗口，首页不进入最近使用。加解密私钥操作与生成密码、HTTP basic-auth 密码默认遮蔽。见 [DIFF-031](diff/031-jsonpath-history-shortcuts.md)、[DIFF-032](diff/032-history-settings-sql.md)、[DIFF-033](diff/033-shell-theme-editor-packaging.md)、[DIFF-042](diff/042-detach-focus-minsize.md)、[DIFF-056](diff/056-command-palette-sidebar-search.md)、[DIFF-057](diff/057-in-app-overlay-dialogs.md)、[DIFF-058](diff/058-overlay-blocks-swing-editor.md) |
| A03 | 桌面/存储/备份/Git/更新 | 开发中 | 备份 zip 见 [DIFF-022](diff/022-backup-zip-manifest.md)；Git CLI 含远程与自动检查点见 [DIFF-025](diff/025-git-cli-local-checkpoint.md)、[DIFF-028](diff/028-git-remote-askpass.md)、[DIFF-029](diff/029-frontmatter-vault-git-import.md)；Git 差异为变更前/后分栏预览，提交记录可切换该提交全部文件见 [DIFF-043](diff/043-diff-sync-git-preview.md)、[DIFF-044](diff/044-window-copy-image-git.md)；跨产品导入见 DIFF-029；更新通道见 [DIFF-027](diff/027-update-channel-open-installer.md)；安装身份、RPM、macOS 类别与产物命名见 [DIFF-030](diff/030-layout-theme-editor.md)、[DIFF-033](diff/033-shell-theme-editor-packaging.md)；AWT 托盘见 [DIFF-034](diff/034-tray-density-softwrap.md)；窗口恢复夹紧见 [DIFF-044](diff/044-window-copy-image-git.md)；自动安装与三平台发行未做 |

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

本机 2026-09-15 结果：DIFF-058 后 `desktopTest` **240/240**。F00–F25 主窗运行截图已收入 `docs/evidence/2026-09-15-inspector-screencapture/windows/`。有窗口帧不等于整行已验收。产品主窗 Tab 焦点、系统 IME 手工、托盘 TCC、六套 CSS 皮肤、三平台安装未测。

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
| T05–T07 | 编辑器分离/IME/列编辑 | 见规格 | 列编辑引擎、5 MiB `setText`、短行粘贴补齐与 `JFrame` 派发拖选+IME 提交/预编辑不落盘单测已过；用户手工窗口手势/分离未测 |
| T08 | JSON 重复 key/大整数/filter | 真实执行 | 单测覆盖重复 key、9007199254740993、JSONPath filter/slice/union/`$..`/转义键与非法路径 |
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

视觉：本机已用 `screencapture -l` 截取 F00–F25 主窗及浅色首页/JSON/设置，见 `docs/evidence/2026-09-15-inspector-screencapture/windows/`。产品主窗 Tab 焦点环仍未拍。`compact-960-focus-ring.png` 是 Compose 测试对真实按钮 `requestFocus()` 的场景图；`70-column-edit-jframe.png` 是列编辑测试窗，二者都**不能代替**产品窗口手工验收。`docs/evidence/2026-09-14-shell-theme/boards/` 与 `jframe-paint-960.png` 同样不能代替安装镜像或主窗手势。

## 7. 证据记录模板

后续每阶段建立 `docs/evidence/YYYY-MM-DD-阶段/`。见既有证据目录，以及 `docs/evidence/2026-09-14-update/`、`docs/evidence/2026-09-14-parity-gap/`、`docs/evidence/2026-09-14-layout-theme/`、`docs/evidence/2026-09-14-jsonpath-history/`、`docs/evidence/2026-09-14-history-settings/`、`docs/evidence/2026-09-14-shell-theme/`、`docs/evidence/2026-09-15-tray-density/`、`docs/evidence/2026-09-15-compact-font-shortcuts/`、`docs/evidence/2026-09-15-note-toolbar/`、`docs/evidence/2026-09-15-vault-sort-shortcuts/`、`docs/evidence/2026-09-15-vault-search-index/`、`docs/evidence/2026-09-15-inspector-tree-keys/`、`docs/evidence/2026-09-15-follow-tail-overflow/`、`docs/evidence/2026-09-15-context-menu-panes/`、`docs/evidence/2026-09-15-detach-focus-minsize/`、`docs/evidence/2026-09-15-diff-sync-git-preview/`、`docs/evidence/2026-09-15-window-copy-image-git/`、`docs/evidence/2026-09-15-reformat-color-overflow/`、`docs/evidence/2026-09-15-http-find-copy/`、`docs/evidence/2026-09-15-encode-crypto-select/`、`docs/evidence/2026-09-15-http-binary-overflow/`、`docs/evidence/2026-09-15-host-net-ua-overflow/`、`docs/evidence/2026-09-15-http-editorhost/`、`docs/evidence/2026-09-15-settings-chrome/`、`docs/evidence/2026-09-15-inspector-screencapture/`。

## 8. 完成定义

阶段完成：退出条件满足并有证据；缺少条件的部分继续保留未验收。整个产品完成：F00–F25/A01–A03 的必要范围落实，平台发布矩阵与实际支持一致，安装/升级/卸载隔离真实通过，文档与软件状态一致。

当前交付是可运行的 0.1.0 开发切片，不是完整产品验收。
