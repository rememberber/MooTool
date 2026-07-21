![logo](assets/logo/logo-128.png)

<p align="center">
  <strong>English</strong> ·
  <a href="README_zh_CN.md">简体中文</a> ·
  <a href="README_ja.md">日本語</a>
</p>

# MooTool

Handy desktop toolset for developers.

[![码云Gitee](https://gitee.com/zhoubochina/MooTool/badge/star.svg?theme=blue)](https://gitee.com/zhoubochina/MooTool)
[![GitHub stars](https://img.shields.io/github/stars/rememberber/MooTool.svg)](https://github.com/rememberber/MooTool)
[![GitHub release](https://img.shields.io/github/v/release/rememberber/MooTool)](https://github.com/rememberber/MooTool/releases)
[![GitHub license](https://img.shields.io/github/license/rememberber/MooTool)](https://github.com/rememberber/MooTool/blob/master/LICENSE.txt)

<a href="https://hellogithub.com/repository/4e5f287079734f98890a69d56000b361" target="_blank"><img src="https://api.hellogithub.com/v1/widgets/recommend.svg?rid=4e5f287079734f98890a69d56000b361&claim_uid=0UhXFJvP9ndHtiB" alt="Featured｜HelloGitHub" style="width: 250px; height: 54px;" width="250" height="54" /></a>

## Supported platforms
Windows • Linux • macOS

## Screenshots

> Main feature screenshots below. Translation, UA analysis, Image assistant, PDF, environment variables, system info, config conversion, Protobuf, and other modules do not have dedicated screenshots yet — open the corresponding tabs after install.

![Quick Note](screen_shoot/quick_note_2_mac.png)

![Quick Note - quick replace](screen_shoot/quick_replace_mac.png)

![Overview](screen_shoot/time_mac_2.png)

![Overview](screen_shoot/json_mac_2.png)

![Overview](screen_shoot/host_mac_2.png)

![Overview](screen_shoot/http_mac_2.png)

![Overview](screen_shoot/encode_mac_2.png)

![Overview](screen_shoot/qr_code_mac_2.png)

![Overview](screen_shoot/crypto_mac_2.png)

![Overview](screen_shoot/calculator_mac_2.png)

![Overview](screen_shoot/net_mac_2.png)

![Overview](screen_shoot/color_board_mac_2.png)

![Overview](screen_shoot/regex_mac_2.png)

![Overview](screen_shoot/cron_mac_2.png)

![Overview](screen_shoot/java_mac_2.png)

![Overview](screen_shoot/diff_mac.png)

![Overview](screen_shoot/mt-favoriteColor.png)

![Overview](screen_shoot/quick_note_light_mac.png)

![Theme & appearance](screen_shoot/theme.png)

## Download

- [MooTool Next Electron 1.0.0 (recommended)](https://github.com/rememberber/MooTool/releases/tag/next-electron-v1.0.0)
- [MooTool Java 1.8.1](https://github.com/rememberber/MooTool/releases/tag/v1.8.1)
- [All GitHub Releases](https://github.com/rememberber/MooTool/releases)
- [MooTool Java releases on Gitee](https://gitee.com/zhoubochina/MooTool/releases)

## Support the author

**If MooTool helps you, consider supporting the evenings and weekends spent building it — your appreciation keeps me motivated.**

![zanshang](assets/material/wx-zanshang.jpg)

# MooTool feature map

> Many modules include a **History** sub-tab: search, apply, copy input/output, delete, and clear all.

```text
MooTool
├── Quick Note
│   ├── Multi-language syntax highlighting
│   ├── Common code formatting
│   │   ├── SQL
│   │   ├── JSON
│   │   └── Java
│   ├── Ordered / unordered lists
│   ├── Markdown live preview
│   ├── Insert images in Markdown
│   ├── Export / batch export, search, global find
│   ├── Document info (created/updated time, word count, etc.)
│   ├── Link detection and click-to-open
│   ├── Font, font size, list colors
│   └── Quick replace panel
│       ├── Batch text operations in the side panel (selected lines only):
│       ├── Auto save
│       ├── Trim spaces
│       ├── Remove empty lines
│       ├── Remove tabs (\t)
│       ├── Scientific notation → plain number
│       ├── Plain number → scientific notation
│       ├── Plain number → thousands separator
│       ├── Thousands separator → plain number
│       ├── snake_case → camelCase
│       ├── camelCase → snake_case
│       ├── UPPER → lower
│       ├── lower → UPPER
│       ├── Remove line breaks
│       ├── Line break → comma
│       ├── Line break → ','
│       ├── Line break → ","
│       ├── Comma → line break
│       ├── ',' → line break
│       ├── "," → line break
│       ├── Tab (\t) → line break
│       ├── Deduplicate lines
│       ├── Deduplicate lines and count occurrences
│       ├── Escape
│       ├── Unescape
│       ├── Reverse lines
│       ├── Sort lines A→Z
│       ├── Sort lines Z→A
│       └── Sort by pinyin
├── Time convert
│   ├── Timestamp conversion
│   │   ├── Date/time → timestamp (ms)
│   │   ├── Timestamp (ms) → date/time
│   │   ├── Timestamp (s) → date/time
│   │   └── Date/time → timestamp (s)
│   ├── History
│   ├── Fullscreen clock
│   └── Timezone picker and quick shortcuts
├── JSON
│   ├── JSON format
│   │   ├── Sort keys alphabetically
│   │   ├── Ignore key case
│   │   └── Check duplicate keys
│   ├── JSON minify
│   ├── Export / batch export, find
│   ├── Font and size
│   ├── Swap JSON keys and values
│   ├── JSON → XML
│   ├── XML → JSON
│   ├── JavaBean → JSON
│   ├── JSON → JavaBean
│   ├── Escape
│   ├── Unescape
│   ├── Get JSON via JSON Path
│   └── Visual JSON Path picker
├── Translation
│   ├── 20+ languages with auto-detect (Chinese, English, Japanese, Korean, French, Spanish, German, Russian, …)
│   ├── Google / Bing translators with automatic fallback
│   ├── Save to word book
│   ├── Word book
│   │   ├── Search, create, edit, delete
│   │   └── Retranslate
│   └── Translation history
├── Host
│   ├── Host formatting / syntax highlight
│   ├── Manage / view system hosts
│   ├── Import / export hosts
│   └── Search, find & replace
├── HTTP
│   ├── HTTP requests: GET/POST/PUT/DELETE/HEAD/PATCH/OPTIONS
│   ├── Import cURL
│   ├── Format HTTP header/body
│   ├── Request management
│   ├── Request history
│   └── Search
├── UA analysis
│   ├── Parse User-Agent (browser, engine, OS, device type/brand/model, …)
│   ├── Detect mobile / bot crawlers
│   ├── Preset UAs (Chrome, Firefox, Safari, Edge, WeChat in-app browser, curl, …)
│   ├── Paste / clear
│   └── History
├── Encode / decode
│   ├── Native → Unicode
│   ├── Unicode → Native
│   ├── URL encode / decode
│   ├── Native → hex
│   ├── Hex → Native
│   ├── Native → ASCII (decimal/hex code points)
│   ├── ASCII → Native
│   └── History
├── QR code
│   ├── Generate QR code
│   │   ├── Custom size
│   │   ├── Custom error correction level
│   │   └── Custom logo
│   ├── Decode QR code
│   ├── Read from clipboard
│   └── History
├── Crypto / random
│   ├── Supports Chinese national crypto SM2 / SM3 / SM4
│   ├── Symmetric encrypt/decrypt
│   │   ├── AES
│   │   ├── DES
│   │   └── SM4
│   ├── Asymmetric encrypt/decrypt
│   │   ├── RSA
│   │   └── SM2 (encrypt/decrypt, sign, verify)
│   ├── Digest (file/text)
│   │   ├── MD5
│   │   ├── SHA1
│   │   ├── SHA256
│   │   ├── SHA384
│   │   ├── SHA512
│   │   └── SM3
│   ├── Base64 encode/decode
│   ├── Base32 encode/decode
│   ├── Random UUID
│   ├── Random numeric/alpha/alphanumeric strings (custom length)
│   ├── Random strong passwords (custom length)
│   └── History
├── Calculator
│   ├── Arithmetic
│   ├── Base conversion
│   ├── Greatest common divisor
│   ├── Least common multiple
│   ├── Permutations & combinations
│   └── History
├── Network / IP
│   ├── IP lookup
│   ├── Domain lookup
│   ├── netstat
│   ├── ping
│   ├── IPv4 ↔ long conversion
│   ├── WHOIS lookup
│   └── Flush DNS
├── Color board
│   ├── Theme / standard palettes
│   ├── Screen color picker
│   ├── Free-form color pick
│   ├── Color format conversion
│   ├── Favorite colors
│   ├── Color ops (invert, intersect, add, difference, average)
│   └── History
├── Image assistant
│   ├── Local image host
│   ├── Screenshot
│   ├── Clipboard import / export
│   ├── Zoom toolbar (in/out/original/fit)
│   ├── Image Base64 encode/decode
│   ├── Image compression
│   ├── Image watermark
│   └── Image OCR (Tesseract)
├── Cron
│   ├── Cron expression builder
│   ├── Parse cron (Linux 5-field / Quartz 6–7 field)
│   ├── Validate cron
│   ├── Cron to natural language
│   ├── Next 10 run times
│   ├── Favorite cron expressions
│   ├── History
│   └── Common cron examples
├── Regex
│   ├── Regex match test
│   ├── Favorite regex
│   ├── Common regex patterns
│   └── History
├── Java
│   ├── Java/Groovy format & highlight
│   ├── Java/Groovy interpret & run
│   └── History
├── Reformat
│   ├── Format uploaded files
│   │   ├── Nginx config
│   │   ├── XML
│   │   ├── HTML
│   │   └── Java
│   ├── Paste & format (Nginx / Java / XML / HTML)
│   └── History
├── PDF
│   ├── Split PDF
│   └── Merge PDF
├── Environment
│   ├── System environment variables (table)
│   ├── Java properties
│   └── Refresh / export
├── System info
│   ├── Collects local system/hardware info via OSHI; loads on first visit or refresh:
│   ├── System (OS, computer, firmware, motherboard, …)
│   ├── CPU
│   ├── Memory
│   ├── Storage
│   └── Network
├── Config conversion
│   ├── Properties → YAML
│   ├── YAML → Properties
│   ├── YAML validate (syntax & line numbers)
│   ├── YAML format
│   ├── History
│   ├── JSON → YAML (TODO)
│   └── YAML → JSON (TODO)
├── Text diff
│   ├── Side-by-side diff (sync scroll)
│   ├── Unified diff
│   └── Copy diff
├── Protobuf
│   ├── JSON ↔ Protobuf binary
│   │   ├── Hex / Base64 output
│   │   └── Format `.proto` definitions
│   ├── Decode wire format (no `.proto` required)
│   ├── Hex / Base64 conversion
│   └── History
└── App & settings
    ├── Sync & backup (Git sync, data export)
    ├── Keyboard shortcuts
    ├── Custom data directory
    ├── Check for updates on startup
    ├── SQL dialect
    ├── System tray
    ├── Window behavior
    │   ├── macOS / Windows: close button hides to Dock/taskbar; app keeps running
    │   └── Linux: close button quits the app
    ├── Appearance
    │   ├── Many themes (Flat Light/Dark, macOS, One Dark, Monokai, …)
    │   ├── Accent color
    │   ├── Follow system accent
    │   ├── Immersive window background
    │   ├── Maximize on startup
    │   └── Tab icons only mode
    └── Layout
        ├── Panel position (top/bottom/left/right)
        └── Global font & size
```

## Acknowledgements

[Hutool](http://hutool.cn/)  
[FlatLaf](https://github.com/JFormDesigner/FlatLaf)  
[vscode-icons](https://github.com/microsoft/vscode-icons)  
[iconfont](https://www.iconfont.cn/)

## Developer notes

See the [multi-product release conventions](RELEASE_CONVENTIONS.md) for version, Git tag, GitHub Release, `Latest`, and CI isolation rules.

Minimum JDK: **21**  
Before you start, **configure IntelliJ IDEA as shown below**, then run **maven clean**:
![considerations](assets/material/gui_build.png)

### Packaging & CI

Packaging JDKs can be downloaded and cached locally:

- JDK archives: `downloads/jdks/`
- Extracted JDKs: `jdks/<os>/<arch>/home`

The download script uses Eclipse Temurin 21 and skips re-download when already present.

#### Prepare local packaging JDKs

```bash
python3 scripts/prepare_jdks.py --targets mac-x64
python3 scripts/prepare_jdks.py --targets mac-arm64
python3 scripts/prepare_jdks.py --targets windows-x64
python3 scripts/prepare_jdks.py --targets linux-x64
```

Resolve all download targets:

```bash
python3 scripts/prepare_jdks.py --targets all --resolve-only
```

#### Local package commands

Default `mvn clean package` uses your current JDK and builds a macOS universal package.

Profiles `mac-intel`, `mac-apple-silicon`, `windows-x64`, and `linux-x64` validate cached JDKs under `jdks/` during `validate`; run `scripts/prepare_jdks.py` first or the build fails fast.

Package for a specific platform using cached JDKs:

```bash
mvn clean package -Pmac-intel -Dmaven.test.skip=true
mvn clean package -Pmac-apple-silicon -Dmaven.test.skip=true
mvn clean package -Pwindows-x64 -Dmaven.test.skip=true
mvn clean package -Plinux-x64 -Dmaven.test.skip=true
```

Output directories:

- Default: `target/`
- Intel Mac: `target/mac-intel/`
- Apple Silicon Mac: `target/mac-apple-silicon/`
- Windows x64: `target/windows-x64/`
- Linux x64: `target/linux-x64/`
