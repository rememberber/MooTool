# 证据：JSON 检查器 chrome、录屏权限、风格底与窗口对照（2026-09-15）

- 命令：`JAVA_HOME="/Users/zhoubo/Library/Java/JavaVirtualMachines/azul-21.0.12.1/Contents/Home" ./gradlew :composeApp:desktopTest --offline`
- 结果：DIFF-097 后 `desktopTest` **263/263**（含 `ToolbarFocusCaptureTest`）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21
- DIFF：[052-json-inspector-screencapture-chrome.md](../../diff/052-json-inspector-screencapture-chrome.md) 至 [060-close-overlay-chrome-tokens.md](../../diff/060-close-overlay-chrome-tokens.md)

## 覆盖

- JSON 检查器分段缩进、开关选项、两列转换网格、类名位置；JSONPath 输入全宽
- Info.plist `NSScreenCaptureUsageDescription` 与 `InstallIdentity.MACOS_SCREEN_CAPTURE_USAGE` 一致
- 权限失败文案含打开系统设置提示（`openedSettings`）
- 工作区/侧栏风格底与首页分区卡片（Compose token）
- 设置 → 布局自定义分组：开关勾选、空组校验、删除入口

## 真实窗口截图

`screencapture -l` 截取运行中的 `MainKt` 窗口，见 `windows/`。第一次按 AX `entire contents` 扫侧栏会超时；有效帧改用一次性 AX 坐标 dump 后 `click at` 中心点。误标为其他工具的 JSON 重复帧已删除。

| 文件 | 内容 |
| --- | --- |
| `21-home.png` | 首页标题 + 分区卡片（深色） |
| `64-home-light.png` | 浅色首页 |
| `30-json.png` | JSON Vault/编辑器/检查器（分段 2/4、开关、两列转换） |
| `45-json-path-fullwidth.png` | JSONPath 输入全宽 |
| `65-json-light.png` | 浅色 JSON |
| `31-json-detached.png` | 拆出窗 `JSON · MooTool Next Compose` |
| `32-main-after-detach.png` | 主窗 JSON 占位（定位/收回） |
| `33-compact-960.png` | 主窗 960×640；内容区低于最小尺寸时显示黄条提示 |
| `23-quicknote.png` | 随手记三栏 |
| `01-diff.png` | 文本对比 |
| `40-reformat.png` | 格式化 |
| `41-code-run.png` | 代码运行 |
| `42-yml.png` | 配置文件转换 |
| `43-protobuf.png` | Protobuf |
| `44-env.png` | 环境变量 |
| `24-http.png` | HTTP 集合/请求/响应 |
| `45-host.png` | Host |
| `46-net.png` | 网络/IP |
| `47-ua.png` | UA 分析 |
| `48-encode.png` | 编码解码 |
| `49-crypto.png` | 加解密/随机 |
| `50-regex.png` | 正则 |
| `51-cron.png` | Cron |
| `52-qr.png` | 二维码 |
| `53-time.png` | 时间转换 |
| `54-message.png` | 留言板 |
| `55-translation.png` | 翻译 |
| `56-calculator.png` | 计算器 |
| `57-color.png` | 调色板 |
| `58-image.png` | 图片助手 |
| `59-pdf.png` | PDF |
| `60-hardware.png` | 系统信息（OSHI 真机数据） |
| `25-settings.png` / `61-settings.png` | 设置通用 |
| `62-settings-layout.png` | 设置 → 布局（隐藏工具开关） |
| `66-settings-custom-groups.png` | 自定义分组开关与「至少选择一个工具」校验 |
| `63-settings-light.png` | 浅色外观 |
| `70-column-edit-jframe.png` | 列编辑测试窗（派发拖选 + IME 提交「中」）；**不是**产品主窗 |
| `71-home-search-button.png` | 侧栏标题栏折叠 / 搜索 ⌕ / 分组 `+`（DIFF-056） |
| `72-command-palette.png` | 命令盘：搜索功能、关闭、结果与分组名 |
| `73-group-manager.png` | 自定义分组弹层：左列表/右开关/删除取消保存 |
| `75-settings-tab-focus.png` | 设置通用：界面语言下拉打开，主按钮可见焦点环 |
| `76-json-960.png` | 960×640 JSON：黄条、折叠导航、工具栏「更多」、无卡住弹层 |
| `77-quicknote-960.png` | 960×640 随手记：库折叠进工具栏、编辑区可用 |
| `78-http-960.png` | 960×640 HTTP：集合/请求/响应，无卡住弹层 |
| `79-json-group-overlay.png` | 960 JSON 上分组管理 overlay：实底面板、取消/保存可点 |
| `81-settings-before-tab.png` | 设置通用默认态 |
| `83-diff-960.png` | 960×640 文本对比 |
| `84-net-960.png` | 960×640 网络/IP |
| `85-settings-appearance.png` | 设置 → 外观（深色）；「外观」导航有焦点环 |
| `87-settings-light.png` | 浅色外观；「浅色」分段有焦点环 |
| `88-quicknote-light.png` | 浅色随手记三栏 |
| `89-http-light.png` | 浅色 HTTP |
| `90-json-light-detached.png` | 浅色拆出窗 `JSON · MooTool Next Compose` |
| `91-main-after-detach-light.png` | 浅色主窗；侧栏 JSON 显示已分离标记 |

| `92-home-960.png` | 960×640 首页 |
| `93-reformat-960.png` | 960×640 格式化 |
| `94-coderun-960.png` | 960×640 代码运行 |
| `95-yml-960.png` | 960×640 配置转换 |
| `96-protobuf-960.png` | 960×640 Protobuf |
| `97-env-960.png` | 960×640 环境变量 |
| `98-host-960.png` | 960×640 Host |
| `99-ua-960.png` | 960×640 UA |
| `100-encode-960.png` | 960×640 编码解码 |
| `101-crypto-960.png` | 960×640 加解密 |
| `102-regex-960.png` | 960×640 正则 |
| `103-cron-960.png` | 960×640 Cron |
| `104-qr-960.png` | 960×640 二维码 |
| `105-time-960.png` | 960×640 时间 |
| `106-message-960.png` | 960×640 留言板 |
| `107-translation-960.png` | 960×640 翻译 |
| `108-calculator-960.png` | 960×640 计算器 |
| `109-color-960.png` | 960×640 调色板 |
| `110-image-960.png` | 960×640 图片 |
| `111-pdf-960.png` | 960×640 PDF |
| `112-hardware-960.png` | 960×640 系统信息（布局；采集被切走/取消后可能空表） |
| `113-settings-960.png` | 960×640 设置通用 |
| `114-close-ask-overlay.png` | 关闭确认应用内 overlay：隐藏到后台 / 退出 MooTool / 取消 |
| `115-json-column-edit-product.png` | 产品 JSON 主窗：Alt 拖选后 `stack` 数组第 3–7 行出现半透明矩形列高亮。这是列**选择**，不是多行写入，也不是系统 IME。曾被 HTTP 页误覆盖，本帧已重拍 |
| `116-json-tab-focus.png` | 与 `123` 同时段：JSON「复制」可见外描边焦点环；Tab 一次后环仍在「复制」 |
| `117-http-tab-focus.png` | HTTP 页，发送为主按钮，工具栏焦点环不明显 |
| `118-http-search-tab-focus.png` | HTTP 集合「搜索」可见焦点环 |
| `120-json-tab-focus-more.png` | JSON Vault「搜索功能…」输入框可见焦点环 |
| `122-json-column-type.png` | 产品 JSON 主窗：Alt 拖选后键入 ASCII `x`，`stack` 多行同一列出现 `x`。这是列选**写入**，不是系统 IME |
| `123-json-copy-focus.png` | 与 `122` 同画面：「复制」外描边焦点环；状态栏「已复制到剪贴板」 |
| `124-quicknote-column-type.png` | 随手记产品窗：粘贴 `alpha/bravo/charlie/delta`；Alt 拖选帧，写入尚未落在这一张 |
| `125-quicknote-column-latch.png` | 随手记闩锁列编辑：状态栏「列编辑按逻辑行…」，多行被写成 `x`/`xie`。ASCII 键入，不是系统 IME |
| `126-home-tab-focus.png` | 首页 `mootool.luoboduner.com` 外描边焦点环（AX focus，不是 Unicode 注入） |
| `127-compose-command-search-tab-focus.png` | Compose 场景命令盘搜索占位焦点环（`ToolbarFocusCaptureTest`，非产品窗） |
| `128-compose-json-copy-tab-focus.png` | Compose 场景 JSON「复制」按钮焦点环（同上，与 `123` 互补） |
| `129-compose-http-send-tab-focus.png` | Compose 场景 HTTP「发送」`p5Toolbar` 焦点环（[DIFF-107](../../diff/107-git-remote-sync-tab-focus-evidence.md)） |
| `130-compose-translation-now-tab-focus.png` | Compose 场景翻译「翻译」`p5Toolbar` 焦点环（同上） |
| `131-compose-json-vault-search-tab-focus.png` | Compose 场景 `MooCompactSearch`（JSON Vault 搜索）焦点环（[DIFF-427](../../diff/427-json-vault-search-tab-focus-evidence.md)，与 `120` 互补） |
| `132-compose-http-collection-search-tab-focus.png` | Compose 场景 HTTP 集合 `MooCompactSearch`（210dp，[DIFF-428](../../diff/428-http-collection-search-tab-focus-evidence.md)，与 `118` 互补） |
| `133-compose-host-profile-search-tab-focus.png` | Compose 场景 Host 方案列表 `MooCompactSearch`（220dp，[DIFF-429](../../diff/429-electron-jsontools-fixture-host-search-focus.md)） |
| `134-compose-settings-appearance-nav-tab-focus.png` | Compose 场景设置「外观」导航项焦点环（[DIFF-431](../../diff/431-settings-nav-focusable-tab-evidence.md)，与 `85` 互补） |
| `135-compose-settings-language-select-tab-focus.png` | Compose 场景设置语言 `MooSelect` 触发钮焦点环（同上） |
| `136-compose-sidebar-language-chip-tab-focus.png` | Compose 场景侧栏语言「中」芯片焦点环（[DIFF-432](../../diff/432-sidebar-language-focusable-tab-evidence.md)） |
| `137-compose-sidebar-search-ghost-tab-focus.png` | Compose 场景侧栏 ⌕ `MooGhostButton` 焦点环（同上，与 `127` 命令盘输入互补） |
| `138-compose-sidebar-nav-item-tab-focus.png` | Compose 场景侧栏工具导航项（选中态）焦点环（[DIFF-433](../../diff/433-sidebar-nav-accent-swatch-tab-focus.md)） |
| `139-compose-settings-accent-swatch-tab-focus.png` | Compose 场景设置强调色色片焦点环（同上，与 `85`/`134` 互补） |
| `140-compose-command-palette-result-tab-focus.png` | Compose 场景命令盘结果行焦点环（[DIFF-434](../../diff/434-command-palette-result-tab-focus.md)，与 `127` 搜索框互补） |
| `141-compose-vault-conflict-reload-tab-focus.png` | Compose 场景 Vault 外部冲突「重新加载」钮焦点环（[DIFF-436](../../diff/436-vault-external-conflict-copy-focus.md)；产品主窗冲突叠层仍待拍） |
| `142-compose-git-conflict-ours-tab-focus.png` | Compose 场景 Vault Git 冲突「使用本地版本」钮焦点环（[DIFF-437](../../diff/437-git-conflict-actions-quicknote-copy.md)；产品主窗 Git 面板仍待拍） |
| `143-compose-vault-conflict-savecopy-tab-focus.png` | Compose 场景 Vault 外部冲突「保存副本」钮焦点环（[DIFF-438](../../diff/438-vault-conflict-session-wiring-evidence.md)） |
| `144-compose-git-conflict-theirs-tab-focus.png` | Compose 场景 Vault Git 冲突「使用远端版本」钮焦点环（[DIFF-439](../../diff/439-vault-monitor-conflict-integration.md)） |
| `145-compose-vault-conflict-keep-tab-focus.png` | Compose 场景 Vault 外部冲突「保留编辑」钮焦点环（同上） |
| `146-compose-vault-conflict-deleted-savecopy-tab-focus.png` | Compose 场景外部删除冲突（无「载入磁盘」）「另存副本」钮焦点环（[DIFF-441](../../diff/441-vault-conflict-reload-keep-deleted-ui.md)） |
| `147-compose-json-inspector-inline-path-query.png` | Compose 场景 JSON 检查器内联路径树双击查询结果 + 选中行预览（[DIFF-456](../../diff/456-json-inspector-inline-path-capture.md)；非产品主窗） |
| `148-compose-json-path-picker-use-tab-focus.png` | Compose 场景 JSONPath 选择器弹层（[DIFF-457](../../diff/457-json-path-picker-dialog-extract.md)；非产品主窗） |
| `149-compose-command-palette-close-tab-focus.png` | Compose 场景命令盘关闭钮 Tab 焦点环（[DIFF-492](../../diff/492-command-palette-close-tab-focus.md)） |
| `150-compose-modern-flatten-tool-shell.png` | Compose 场景 modern 非 p5 双栏 `mooToolShell` 压平（[DIFF-510](../../diff/510-settings-group-command-shell-capture.md)；非产品主窗） |
| `151-compose-command-palette-search-row-tab.png` | Compose 场景命令盘搜索行（输入 + 关闭钮焦点环，[DIFF-510](../../diff/510-settings-group-command-shell-capture.md)；与 `140`/`149` 互补） |

有窗口帧不等于该工具整行已验收。Vault/Git 冲突**产品主窗**步骤见 [2026-09-17-vault-conflict-product-window](../2026-09-17-vault-conflict-product-window/results.md)（未执行）。曾用 Unicode `type "中"` 插入 JSON，那不是系统 IME 预编辑，不能当 IME 验收。

## 未测

- 首页以外其余工具页的产品窗 Tab 走查仍缺；已有设置语言/外观（产品窗 `85` + Compose `134`/`135`/`139`）、侧栏语言/搜索/导航项（Compose `136`–`138`）、命令盘结果行（Compose `140`）、Vault 外部冲突（Compose `141`/`143`）、Git 冲突操作（Compose `142`，产品主窗仍缺）、JSON Vault 搜索、HTTP 集合搜索、JSON「复制」、首页站点链接外描边，以及 Compose 回归帧 `127`–`148`（不能代替产品窗）
- 系统输入法预编辑窗口手势（`70-column-edit-jframe.png` 不能代替；产品窗 Unicode 注入也不能代替）
- 托盘取色/截图 TCC 对话框手工
- Electron 六套 CSS 逐选择器皮肤；DIFF-088～093 已补 P5 密度/壳/Host 正文/HTTP 响应/翻译/UA/环境作用域；系统 IME、托盘 TCC、产品窗 Tab 焦点帧仍待
- 三平台安装/升级/卸载/公证
