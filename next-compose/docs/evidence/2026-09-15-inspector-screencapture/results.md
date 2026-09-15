# 证据：JSON 检查器 chrome、录屏权限、风格底与窗口对照（2026-09-15）

- 命令：`JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew :composeApp:desktopTest --offline`
- 结果：`desktopTest` **240/240**（0 skipped / 0 failed）
- 本机：macOS Darwin 25.6.0 x86_64，Zulu 21
- DIFF：[052-json-inspector-screencapture-chrome.md](../../diff/052-json-inspector-screencapture-chrome.md) 至 [057-in-app-overlay-dialogs.md](../../diff/057-in-app-overlay-dialogs.md)

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

有窗口帧不等于该工具整行已验收。

## 未测

- 产品主窗 Tab 焦点环走查
- 系统输入法预编辑窗口手势（`70-column-edit-jframe.png` 不能代替）
- 其余工具在 960 宽度的逐页帧（仅有主窗 960 黄条）
- 托盘取色/截图 TCC 对话框手工
- IME/列编辑窗口手势
- Electron 六套 CSS 逐选择器皮肤
- 三平台安装/升级/卸载/公证
