#!/usr/bin/env python3
"""Generate README.md (EN), README_zh_CN.md, README_ja.md from README_zh_CN.md.

Edit README_zh_CN.md (body under ## headings) and scripts/readme_feature_map.md
(feature map in heading/list form), then run:
  python3 scripts/build_readme_i18n.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from readme_zh_ja_map import ZH_JA_MAP

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "README_zh_CN.md"
FEATURE_MAP_SRC = Path(__file__).resolve().parent / "readme_feature_map.md"
OUT_EN = ROOT / "README.md"
OUT_ZH = ROOT / "README_zh_CN.md"
OUT_JA = ROOT / "README_ja.md"

FEATURE_MAP_STARTS = (
    "# MooTool全功能地图",
    "# MooTool feature map",
    "# MooTool 機能一覧",
)
FEATURE_MAP_ENDS = (
    "## 特别感谢",
    "## Acknowledgements",
    "## 謝辞",
)

SWITCHER = {
    "en": (
        '<p align="center">\n'
        '  <strong>English</strong> ·\n'
        '  <a href="README_zh_CN.md">简体中文</a> ·\n'
        '  <a href="README_ja.md">日本語</a>\n'
        "</p>\n\n"
    ),
    "zh": (
        '<p align="center">\n'
        '  <a href="README.md">English</a> ·\n'
        '  <strong>简体中文</strong> ·\n'
        '  <a href="README_ja.md">日本語</a>\n'
        "</p>\n\n"
    ),
    "ja": (
        '<p align="center">\n'
        '  <a href="README.md">English</a> ·\n'
        '  <a href="README_zh_CN.md">简体中文</a> ·\n'
        '  <strong>日本語</strong>\n'
        "</p>\n\n"
    ),
}

LOGO = "![logo](assets/logo/logo-128.png)"

BADGES = """\
[![码云Gitee](https://gitee.com/zhoubochina/MooTool/badge/star.svg?theme=blue)](https://gitee.com/zhoubochina/MooTool)
[![GitHub stars](https://img.shields.io/github/stars/rememberber/MooTool.svg)](https://github.com/rememberber/MooTool)
[![GitHub release](https://img.shields.io/github/v/release/rememberber/MooTool)](https://github.com/rememberber/MooTool/releases)
[![GitHub license](https://img.shields.io/github/license/rememberber/MooTool)](https://github.com/rememberber/MooTool/blob/master/LICENSE.txt)

<a href="https://hellogithub.com/repository/4e5f287079734f98890a69d56000b361" target="_blank"><img src="https://api.hellogithub.com/v1/widgets/recommend.svg?rid=4e5f287079734f98890a69d56000b361&claim_uid=0UhXFJvP9ndHtiB" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>"""

# Longer phrases first.
EN_MAP = [
    ("Handy tool set for developers.   \n开发者常备小工具", "Handy desktop toolset for developers."),
    ("## 支持的平台", "## Supported platforms"),
    ("## 截图速览", "## Screenshots"),
    ("> 以下为主要功能界面截图。翻译、UA分析、图片助手、PDF、环境变量、系统信息、配置文件转换、Protobuf 等模块暂未单独配图，可在安装后于对应 Tab 中体验。",
     "> Main feature screenshots below. Translation, UA analysis, Image assistant, PDF, environment variables, system info, config conversion, Protobuf, and other modules do not have dedicated screenshots yet — open the corresponding tabs after install."),
    ("![随手记-快捷替换]", "![Quick Note - quick replace]"),
    ("![随手记]", "![Quick Note]"),
    ("![主题与外观]", "![Theme & appearance]"),
    ("## 下载", "## Download"),
    ("## 鼓励/赞赏", "## Support the author"),
    ("**如果MooTool项目对您有所帮助或带来便利，  \n欢迎对我每天下班和周末时光的努力进行肯定，  \n您的赞赏将会给我带来更多动力**",
     "**If MooTool helps you, consider supporting the evenings and weekends spent building it — your appreciation keeps me motivated.**"),
    ("# MooTool全功能地图", "# MooTool feature map"),
    ("> 多个工具模块提供统一的「历史记录」子 Tab，支持搜索、应用、复制输入/输出、删除与清空。",
     "> Many modules include a **History** sub-tab: search, apply, copy input/output, delete, and clear all."),
    ("## 随手记", "## Quick Note"),
    ("### 多语言语法高亮支持", "### Multi-language syntax highlighting"),
    ("### 常见语言代码格式化", "### Common code formatting"),
    ("### 有序/无序列表", "### Ordered / unordered lists"),
    ("### Markdown 实时预览", "### Markdown live preview"),
    ("### Markdown 插入图片", "### Insert images in Markdown"),
    ("### 导出/批量导出，搜索与全局查找", "### Export / batch export, search, global find"),
    ("### 文档信息（创建/更新时间、字数等）", "### Document info (created/updated time, word count, etc.)"),
    ("### 超链接识别与点击跳转", "### Link detection and click-to-open"),
    ("### 字体、字号、列表颜色", "### Font, font size, list colors"),
    ("### 快捷替换面板", "### Quick replace panel"),
    ("侧栏集中提供以下批量文本操作，支持仅处理选中行：", "Batch text operations in the side panel (selected lines only):"),
    ("- 自动保存", "- Auto save"),
    ("- 去掉空格", "- Trim spaces"),
    ("- 去掉空行", "- Remove empty lines"),
    ("- 去掉Tab(\\t)", "- Remove tabs (\\t)"),
    ("- 科学计数法->普通数字", "- Scientific notation → plain number"),
    ("- 普通数字->科学计数法", "- Plain number → scientific notation"),
    ("- 普通数字->千分位", "- Plain number → thousands separator"),
    ("- 千分位->普通数字", "- Thousands separator → plain number"),
    ("- 下划线命名->驼峰命名", "- snake_case → camelCase"),
    ("- 驼峰命名->下划线命名", "- camelCase → snake_case"),
    ("- 大写->小写", "- UPPER → lower"),
    ("- 小写->大写", "- lower → UPPER"),
    ("- 去掉换行", "- Remove line breaks"),
    ("- 换行->,", "- Line break → comma"),
    ("- 换行->','", "- Line break → ','"),
    ("- 换行->\",\"", "- Line break → \",\""),
    ("- ,->换行", "- Comma → line break"),
    ("- ','->换行", "- ',' → line break"),
    ("- \",\"->换行", "- \",\" → line break"),
    ("- Tab(\\t)->换行", "- Tab (\\t) → line break"),
    ("- 按行去重并统计出现次数", "- Deduplicate lines and count occurrences"),
    ("- 按行去重", "- Deduplicate lines"),
    ("- 转义", "- Escape"),
    ("- 反转义", "- Unescape"),
    ("- 按行倒序", "- Reverse lines"),
    ("- 按行A->Z排序", "- Sort lines A→Z"),
    ("- 按行Z->A排序", "- Sort lines Z→A"),
    ("- 按拼音排序", "- Sort by pinyin"),
    ("## 时间转换", "## Time convert"),
    ("### 时间戳转换", "### Timestamp conversion"),
    ("- 时间->时间戳(毫秒)", "- Date/time → timestamp (ms)"),
    ("- 时间戳(毫秒)->时间", "- Timestamp (ms) → date/time"),
    ("- 时间戳->时间(秒)", "- Timestamp (s) → date/time"),
    ("- 时间->时间戳(秒)", "- Date/time → timestamp (s)"),
    ("### 历史记录", "### History"),
    ("### 大屏时钟", "### Fullscreen clock"),
    ("### 时区选择与快捷时区", "### Timezone picker and quick shortcuts"),
    ("## JSON", "## JSON"),
    ("### JSON格式化", "### JSON format"),
    ("- Key 按字母顺序排序", "- Sort keys alphabetically"),
    ("- 忽略 key 大小写", "- Ignore key case"),
    ("- 检查重复 key", "- Check duplicate keys"),
    ("### JSON压缩", "### JSON minify"),
    ("### 导出/批量导出，查找", "### Export / batch export, find"),
    ("### 字体、字号", "### Font and size"),
    ("### JSON Key Value互转", "### Swap JSON keys and values"),
    ("### JSON转XML", "### JSON → XML"),
    ("### XML转JSON", "### XML → JSON"),
    ("### JavaBean转JSON", "### JavaBean → JSON"),
    ("### JSON转JavaBean", "### JSON → JavaBean"),
    ("### 转义", "### Escape"),
    ("### 反转义", "### Unescape"),
    ("### 通过JsonPath获取JSON数据", "### Get JSON via JSON Path"),
    ("### 可视化获取JsonPath", "### Visual JSON Path picker"),
    ("## 翻译", "## Translation"),
    ("### 多语言互译（中/英/日/韩/法/西/德/俄等 20+ 语言，支持自动检测）",
     "### 20+ languages with auto-detect (Chinese, English, Japanese, Korean, French, Spanish, German, Russian, …)"),
    ("### Google / Bing 翻译源，失败时自动降级", "### Google / Bing translators with automatic fallback"),
    ("### 收藏到单词本", "### Save to word book"),
    ("### 单词本", "### Word book"),
    ("- 搜索、新建、编辑、删除", "- Search, create, edit, delete"),
    ("- 重新翻译", "- Retranslate"),
    ("### 翻译历史记录", "### Translation history"),
    ("## Host", "## Host"),
    ("### Host格式化/语法高亮", "### Host formatting / syntax highlight"),
    ("### 本机Host管理/查看", "### Manage / view system hosts"),
    ("### Host导入/导出", "### Import / export hosts"),
    ("### 搜索与查找/替换", "### Search, find & replace"),
    ("## HTTP", "## HTTP"),
    ("### HTTP请求，支持GET/POST/PUT/DELETE/HEAD/PATCH/OPTIONS", "### HTTP requests: GET/POST/PUT/DELETE/HEAD/PATCH/OPTIONS"),
    ("### cURL 导入", "### Import cURL"),
    ("### HTTP Header/Body格式化", "### Format HTTP header/body"),
    ("### 请求管理", "### Request management"),
    ("### 请求历史记录", "### Request history"),
    ("### 搜索", "### Search"),
    ("## UA分析", "## UA analysis"),
    ("### User-Agent 解析（浏览器、引擎、操作系统、设备类型/品牌/型号等）",
     "### Parse User-Agent (browser, engine, OS, device type/brand/model, …)"),
    ("### 识别移动端 / 爬虫 Bot", "### Detect mobile / bot crawlers"),
    ("### 预设 UA 快速选择（Chrome、Firefox、Safari、Edge、微信内置浏览器、curl 等）",
     "### Preset UAs (Chrome, Firefox, Safari, Edge, WeChat in-app browser, curl, …)"),
    ("### 粘贴 / 清空", "### Paste / clear"),
    ("## 编码转换", "## Encode / decode"),
    ("### Native->Unicode", "### Native → Unicode"),
    ("### Unicode->Native", "### Unicode → Native"),
    ("### URL编码/解码", "### URL encode / decode"),
    ("### Native->十六进制", "### Native → hex"),
    ("### 十六进制->Native", "### Hex → Native"),
    ("### Native->ASCII（十进制/十六进制字符码）", "### Native → ASCII (decimal/hex code points)"),
    ("### ASCII->Native", "### ASCII → Native"),
    ("## 二维码", "## QR code"),
    ("### 二维码生成", "### Generate QR code"),
    ("- 尺寸自定义", "- Custom size"),
    ("- 纠错等级自定义", "- Custom error correction level"),
    ("- logo自定义", "- Custom logo"),
    ("### 二维码解析", "### Decode QR code"),
    ("### 从剪贴板识别", "### Read from clipboard"),
    ("## 加解密/随机", "## Crypto / random"),
    ("> 支持国密 SM2 / SM3 / SM4", "> Supports Chinese national crypto SM2 / SM3 / SM4"),
    ("### 对称加密/解密", "### Symmetric encrypt/decrypt"),
    ("### 非对称加密/解密", "### Asymmetric encrypt/decrypt"),
    ("- SM2（加密/解密、私钥签名、公钥验签）", "- SM2 (encrypt/decrypt, sign, verify)"),
    ("### 摘要算法（文件/文本摘要）", "### Digest (file/text)"),
    ("### Base64编码/解码", "### Base64 encode/decode"),
    ("### Base32编码/解码", "### Base32 encode/decode"),
    ("### 随机UUID生成", "### Random UUID"),
    ("### 随机生成只包含数字/字母/数字字母的字符串，位数自定义", "### Random numeric/alpha/alphanumeric strings (custom length)"),
    ("### 随机生成复杂密码，位数自定义", "### Random strong passwords (custom length)"),
    ("## 计算", "## Calculator"),
    ("### 四则运算", "### Arithmetic"),
    ("### 进制转换", "### Base conversion"),
    ("### 最大公约数", "### Greatest common divisor"),
    ("### 最小公倍数", "### Least common multiple"),
    ("### 排列组合数", "### Permutations & combinations"),
    ("## 网络/IP", "## Network / IP"),
    ("### IP查询", "### IP lookup"),
    ("### 域名查询", "### Domain lookup"),
    ("### ipv4-Long互相转换", "### IPv4 ↔ long conversion"),
    ("### WHOIS 查询", "### WHOIS lookup"),
    ("### 刷新DNS", "### Flush DNS"),
    ("## 调色板", "## Color board"),
    ("### 主题颜色/标准颜色", "### Theme / standard palettes"),
    ("### 屏幕取色器", "### Screen color picker"),
    ("### 自由选色", "### Free-form color pick"),
    ("### 颜色格式转换", "### Color format conversion"),
    ("### 颜色收藏", "### Favorite colors"),
    ("### 颜色混合计算（取反、相交、相加、差值、平均）", "### Color ops (invert, intersect, add, difference, average)"),
    ("## 图片助手", "## Image assistant"),
    ("### 本地图床", "### Local image host"),
    ("### 截图", "### Screenshot"),
    ("### 从剪贴板获取 / 复制到剪贴板", "### Clipboard import / export"),
    ("### 图片缩放工具栏（放大/缩小/原始尺寸/适应窗口）", "### Zoom toolbar (in/out/original/fit)"),
    ("### 图片Base64编码/解码", "### Image Base64 encode/decode"),
    ("### 图片压缩", "### Image compression"),
    ("### 图片加水印", "### Image watermark"),
    ("### 图片OCR识别（基于 Tesseract）", "### Image OCR (Tesseract)"),
    ("## Cron", "## Cron"),
    ("### Cron表达式生成", "### Cron expression builder"),
    ("### Cron表达式解析（支持 Linux 5 段 / Quartz 6、7 段）", "### Parse cron (Linux 5-field / Quartz 6–7 field)"),
    ("### Cron表达式校验", "### Validate cron"),
    ("### Cron 转自然语言", "### Cron to natural language"),
    ("### 最近 10 次运行时间", "### Next 10 run times"),
    ("### Cron表达式收藏", "### Favorite cron expressions"),
    ("### 常用Cron表达式", "### Common cron examples"),
    ("## 正则", "## Regex"),
    ("### 正则表达式匹配测试", "### Regex match test"),
    ("### 收藏正则表达式", "### Favorite regex"),
    ("### 常用正则表达式", "### Common regex patterns"),
    ("## Java", "## Java"),
    ("### Java/groovy代码格式化、高亮", "### Java/Groovy format & highlight"),
    ("### Java/groovy代码解释执行", "### Java/Groovy interpret & run"),
    ("## 格式化", "## Reformat"),
    ("### 上传文件格式化", "### Format uploaded files"),
    ("- Nginx 配置文件", "- Nginx config"),
    ("### 粘贴文本直接格式化（Nginx / Java / XML / HTML）", "### Paste & format (Nginx / Java / XML / HTML)"),
    ("## PDF", "## PDF"),
    ("### PDF拆分", "### Split PDF"),
    ("### PDF合并", "### Merge PDF"),
    ("## 环境变量", "## Environment"),
    ("### 系统环境变量（表格浏览）", "### System environment variables (table)"),
    ("### 刷新 / 导出", "### Refresh / export"),
    ("## 系统信息", "## System info"),
    ("基于 OSHI 采集本机系统与硬件信息，首次进入 Tab 或点击刷新时按需加载：",
     "Collects local system/hardware info via OSHI; loads on first visit or refresh:"),
    ("### 系统（操作系统、计算机、固件、主板等）", "### System (OS, computer, firmware, motherboard, …)"),
    ("### 处理器", "### CPU"),
    ("### 内存", "### Memory"),
    ("### 存储", "### Storage"),
    ("### 网络", "### Network"),
    ("## 配置文件转换", "## Config conversion"),
    ("### Properties->YAML", "### Properties → YAML"),
    ("### YAML->Properties", "### YAML → Properties"),
    ("### YAML 校验（语法校验、错误行号定位）", "### YAML validate (syntax & line numbers)"),
    ("### YAML 格式化", "### YAML format"),
    ("### JSON->YAML（TODO）", "### JSON → YAML (TODO)"),
    ("### YAML->JSON（TODO）", "### YAML → JSON (TODO)"),
    ("## 文本对比", "## Text diff"),
    ("### 并排对比（左右滚动同步）", "### Side-by-side diff (sync scroll)"),
    ("### 统一差异", "### Unified diff"),
    ("### 复制差异", "### Copy diff"),
    ("## Protobuf", "## Protobuf"),
    ("### JSON ↔ Protobuf 二进制互转", "### JSON ↔ Protobuf binary"),
    ("- 支持 Hex / Base64 输出", "- Hex / Base64 output"),
    ("- `.proto` 定义格式化", "- Format `.proto` definitions"),
    ("### Wire Format 解码（无需 `.proto` 定义）", "### Decode wire format (no `.proto` required)"),
    ("### Hex / Base64 互转", "### Hex / Base64 conversion"),
    ("## 应用与设置", "## App & settings"),
    ("### 同步和备份（Git 同步、数据导出）", "### Sync & backup (Git sync, data export)"),
    ("### 快捷键说明", "### Keyboard shortcuts"),
    ("### 数据文件位置自定义", "### Custom data directory"),
    ("### 启动时自动检查更新", "### Check for updates on startup"),
    ("### SQL 方言设置", "### SQL dialect"),
    ("### 系统托盘", "### System tray"),
    ("### 窗口行为", "### Window behavior"),
    ("- macOS / Windows：点击关闭按钮时隐藏到 Dock / 任务栏，应用继续在后台运行",
     "- macOS / Windows: close button hides to Dock/taskbar; app keeps running"),
    ("- Linux：点击关闭按钮退出应用", "- Linux: close button quits the app"),
    ("### 外观", "### Appearance"),
    ("- 多主题风格（Flat Light/Dark、macOS、One Dark、Monokai 等）", "- Many themes (Flat Light/Dark, macOS, One Dark, Monokai, …)"),
    ("- 强调色", "- Accent color"),
    ("- 主题颜色跟随系统", "- Follow system accent"),
    ("- 窗口颜色沉浸式", "- Immersive window background"),
    ("- 默认最大化窗口", "- Maximize on startup"),
    ("- 功能 Tab 仅图标模式", "- Tab icons only mode"),
    ("### 使用习惯", "### Layout"),
    ("- 功能面板位置（菜单栏上/下/左/右）", "- Panel position (top/bottom/left/right)"),
    ("- 全局字体与字号", "- Global font & size"),
    ("## 特别感谢", "## Acknowledgements"),
    ("## 开发温馨提示", "## Developer notes"),
    ("最低JDK版本要求：**21**", "Minimum JDK: **21**"),
    ("在你开始开发之前, **请按下图设置IntelliJ IDEA**, 然后 **maven clean**:",
     "Before you start, **configure IntelliJ IDEA as shown below**, then run **maven clean**:"),
    ("### 打包与 CI", "### Packaging & CI"),
    ("项目现在支持把打包 JDK 下载并缓存到仓库本地目录：", "Packaging JDKs can be downloaded and cached locally:"),
    ("- JDK 压缩包缓存：`downloads/jdks/`", "- JDK archives: `downloads/jdks/`"),
    ("- 解压后的 JDK：`jdks/<os>/<arch>/home`", "- Extracted JDKs: `jdks/<os>/<arch>/home`"),
    ("下载脚本默认使用 Eclipse Temurin 21，并且如果本地已经存在对应 JDK，就不会重复下载。",
     "The download script uses Eclipse Temurin 21 and skips re-download when already present."),
    ("#### 先准备本地打包 JDK", "#### Prepare local packaging JDKs"),
    ("也可以一次性查看全部目标会下载到哪里：", "Resolve all download targets:"),
    ("#### 本地打包命令", "#### Local package commands"),
    ("默认 `mvn clean package` 仍然会使用当前运行 Maven 的 JDK 打一个 macOS universal 包。",
     "Default `mvn clean package` uses your current JDK and builds a macOS universal package."),
    ("对于 `mac-intel`、`mac-apple-silicon`、`windows-x64`、`linux-x64` 这几个 profile，Maven 会在 `validate` 阶段先检查 `jdks/` 下是否已经准备好对应 JDK；如果缺失，会直接失败并提示先执行 `scripts/prepare_jdks.py`，避免打出“假成功”的安装包。",
     "Profiles `mac-intel`, `mac-apple-silicon`, `windows-x64`, and `linux-x64` validate cached JDKs under `jdks/` during `validate`; run `scripts/prepare_jdks.py` first or the build fails fast."),
    ("如果要使用仓库内缓存 JDK 打指定平台包：", "Package for a specific platform using cached JDKs:"),
    ("对应产物目录：", "Output directories:"),
    ("- 默认包：`target/`", "- Default: `target/`"),
    ("- Intel Mac：`target/mac-intel/`", "- Intel Mac: `target/mac-intel/`"),
    ("- Apple Silicon Mac：`target/mac-apple-silicon/`", "- Apple Silicon Mac: `target/mac-apple-silicon/`"),
    ("- Windows x64：`target/windows-x64/`", "- Windows x64: `target/windows-x64/`"),
    ("- Linux x64：`target/linux-x64/`", "- Linux x64: `target/linux-x64/`"),
    ("#### GitHub Actions", "#### GitHub Actions"),
    ("仓库内新增了工作流：`.github/workflows/build-installers.yml`", "Workflow: `.github/workflows/build-installers.yml`"),
    ("> 建议在对应原生 runner 上产出对应平台安装包：mac 安装包在 macOS runner，Windows 安装包在 Windows runner，Linux 安装包在 Linux runner。",
     "> Build each platform on its native runner when possible."),
    ("特点：", "Features:"),
    ("- 支持 `workflow_dispatch`", "- Supports `workflow_dispatch`"),
    ("- 推送 `v*` 标签时自动执行", "- Runs on `v*` tag push"),
    ("- 推送 `v*` 标签时，默认在 GitHub Hosted runner 上打包：", "- On `v*` tags, hosted runners build:"),
    ("- `mac-intel` 改为单独手动触发，并使用 `self-hosted`, `macOS`, `X64` 标签的自托管 runner，避免长期卡在 `macos-13` 队列上",
     "- `mac-intel` is manual-only on a self-hosted runner (`self-hosted`, `macOS`, `X64`)"),
    ("- 使用 `actions/cache` 缓存 `downloads/jdks/` 和 `jdks/`", "- Caches `downloads/jdks/` and `jdks/` via `actions/cache`"),
    ("- 每个 job 会把产物重命名为统一格式后再上传，例如：`MooTool-1.7.0-mac-intel.dmg`、`MooTool-1.7.0-windows-x64.zip`",
     "- Artifacts are renamed before upload, e.g. `MooTool-1.7.0-mac-intel.dmg`, `MooTool-1.7.0-windows-x64.zip`"),
    ("- Actions Summary 会列出“原始文件名 -> Release 文件名”的对照表，方便核对每个平台实际产物",
     "- Actions Summary lists original → release filename mapping"),
    ("- 推送 `v*` 标签时，会自动创建或更新对应 GitHub Release，并上传构建出的安装包附件",
     "- `v*` tags create/update GitHub Release with installer assets"),
    ("手动触发 `Build installers` 时，可以在页面中选择 `target`：", "Manual **Build installers** workflow `target` options:"),
    ("- `all`：运行 Hosted 平台构建，并额外尝试运行自托管的 `mac-intel`", "- `all`: hosted platforms + optional self-hosted `mac-intel`"),
    ("- `mac-intel`：仅在自托管 Intel Mac runner 上打包", "- `mac-intel`: self-hosted Intel Mac only"),
    ("- `mac-apple-silicon` / `windows-x64` / `linux-x64`：只跑对应 Hosted 平台",
     "- `mac-apple-silicon` / `windows-x64` / `linux-x64`: single hosted platform"),
    ("如果手动运行时还填写了 `release_tag`（例如 `v1.7.0`），那么本次运行成功产出的安装包会在任务结束后自动追加上传到这个已有的 GitHub Release，适合在正式发布完成后再单独补齐 `mac-intel` 产物。",
     "If you set `release_tag` (e.g. `v1.7.0`) on manual runs, successful artifacts are appended to that existing Release — useful to add `mac-intel` after the main release."),
    ("如果要支持 `mac-intel`，需要先准备一台 Intel Mac，并把 GitHub Actions Runner 注册为仓库级自托管 runner，标签至少包含：",
     "For `mac-intel`, register a self-hosted Intel Mac runner with labels:"),
]

def _parse_feature_map_lines(lines: list[str]) -> list[dict]:
    root: dict = {"name": "MooTool", "children": []}
    current_module: dict | None = None
    current_feature: dict | None = None

    for raw in lines:
        line = raw.rstrip()
        if not line.strip():
            continue
        if line.startswith("## "):
            current_module = {"name": line[3:].strip(), "children": []}
            root["children"].append(current_module)
            current_feature = None
        elif line.startswith("### "):
            current_feature = {"name": line[4:].strip(), "children": []}
            if current_module is not None:
                current_module["children"].append(current_feature)
        elif line.startswith("- "):
            item = {"name": line[2:].strip(), "children": []}
            parent = current_feature or current_module
            if parent is not None:
                parent["children"].append(item)
        elif line.startswith("> "):
            note = {"name": line[2:].strip(), "children": []}
            if current_module is not None:
                current_module["children"].append(note)
        else:
            note = {"name": line.strip(), "children": []}
            parent = current_feature or current_module
            if parent is not None:
                parent["children"].append(note)
    return [root]


def _render_tree_lines(node: dict, prefix: str = "", is_last: bool = True, is_root: bool = True) -> list[str]:
    lines: list[str] = []
    if is_root:
        lines.append(node["name"])
        children = node["children"]
        for index, child in enumerate(children):
            lines.extend(_render_tree_lines(child, "", index == len(children) - 1, False))
        return lines

    branch = "└── " if is_last else "├── "
    lines.append(f"{prefix}{branch}{node['name']}")
    child_prefix = prefix + ("    " if is_last else "│   ")
    children = node["children"]
    for index, child in enumerate(children):
        lines.extend(_render_tree_lines(child, child_prefix, index == len(children) - 1, False))
    return lines


def feature_map_to_tree_block(text: str, root_name: str = "MooTool") -> str:
    nodes = _parse_feature_map_lines(text.splitlines())
    nodes[0]["name"] = root_name
    tree = "\n".join(_render_tree_lines(nodes[0]))
    return f"```text\n{tree}\n```"


def inject_feature_map_tree(body: str, feature_map_body: str, root_name: str = "MooTool") -> str:
    lines = body.splitlines()
    start = end = None
    for index, line in enumerate(lines):
        if any(line.startswith(title) for title in FEATURE_MAP_STARTS):
            start = index
        elif start is not None and any(line.startswith(marker) for marker in FEATURE_MAP_ENDS):
            end = index
            break
    if start is None or end is None:
        return body

    header: list[str] = []
    for line in lines[start:end]:
        if line.startswith("# MooTool") or line.startswith(">"):
            header.append(line)
        elif not line.strip() and header:
            header.append(line)
        else:
            break

    while header and not header[-1].strip():
        header.pop()

    tree_block = feature_map_to_tree_block(feature_map_body, root_name)
    replacement = header + ["", tree_block, ""]
    return "\n".join(lines[:start] + replacement + lines[end:])


def apply_map(text: str, mapping: list[tuple[str, str]]) -> str:
    multiline = [(s, d) for s, d in mapping if "\n" in s]
    line_maps = [(s, d) for s, d in mapping if "\n" not in s]
    for src, dst in sorted(multiline, key=lambda x: len(x[0]), reverse=True):
        text = text.replace(src, dst)
    ordered = sorted(line_maps, key=lambda x: len(x[0]), reverse=True)
    out: list[str] = []
    for line in text.splitlines():
        new_line = line
        for src, dst in ordered:
            if src.startswith("#"):
                if new_line.startswith(src):
                    new_line = dst + new_line[len(src):]
                    break
            elif new_line == src:
                new_line = dst
                break
        else:
            for src, dst in ordered:
                if not src.startswith("#") and src in new_line and new_line == line:
                    new_line = new_line.replace(src, dst)
                    break
        out.append(new_line)
    return "\n".join(out)


def build_body(src: str, lang: str) -> str:
    skip_patterns = {
        "Handy tool set for developers.",
        "开发者常备小工具",
        "Handy desktop toolset for developers.",
        "開発者向けデスクトップツールセット",
    }
    out: list[str] = []
    for line in src.splitlines():
        stripped = line.strip()
        if stripped in skip_patterns:
            continue
        if stripped == "Handy tool set for developers." or stripped.startswith("Handy tool set for developers"):
            continue
        out.append(line)
    body = "\n".join(out)
    if lang == "en":
        body = apply_map(body, EN_MAP)
    elif lang == "ja":
        body = apply_map(body, ZH_JA_MAP)
    return body


def prepend_header(body: str, lang: str) -> str:
    tagline = {
        "en": "Handy desktop toolset for developers.",
        "zh": "开发者常备小工具",
        "ja": "開発者向けデスクトップツールセット",
    }[lang]
    return (
        f"{LOGO}\n\n"
        f"{SWITCHER[lang]}"
        f"# MooTool\n\n"
        f"{tagline}\n\n"
        f"{BADGES}\n\n"
        f"{body.lstrip()}"
    )


def strip_preamble(text: str) -> str:
    """Drop logo, language switcher, title, tagline, and badges; keep from first ##."""
    match = re.search(r"^## ", text, re.MULTILINE)
    return text[match.start() :] if match else text


def main() -> None:
    src = strip_preamble(SRC.read_text(encoding="utf-8"))
    feature_map_src = FEATURE_MAP_SRC.read_text(encoding="utf-8")

    zh_body = inject_feature_map_tree(build_body(src, "zh"), feature_map_src)
    en_body = inject_feature_map_tree(
        build_body(src, "en"),
        apply_map(feature_map_src, EN_MAP),
    )
    ja_body = inject_feature_map_tree(
        build_body(src, "ja"),
        apply_map(feature_map_src, ZH_JA_MAP),
    )

    OUT_ZH.write_text(prepend_header(zh_body, "zh"), encoding="utf-8")
    OUT_EN.write_text(prepend_header(en_body, "en"), encoding="utf-8")
    OUT_JA.write_text(prepend_header(ja_body, "ja"), encoding="utf-8")
    print(f"Wrote {OUT_EN.name}, {OUT_ZH.name}, {OUT_JA.name}")


if __name__ == "__main__":
    main()
