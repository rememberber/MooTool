# DIFF-514：设置网络代理失焦提交与 Vault Git remote 校验

## 背景

DIFF-509 已为 Vault Git **设置页** remote 做 `SettingCommitTextField` + `commitGitRemote`；DIFF-509「未做」仍列 JSON/随手记 **Vault Git 面板** remote 行即时校验，以及网络代理/超时仍逐字写入（非 Electron `TextSetting` 失焦提交）。本条补 **A01 网络** 代理与超时失焦语义，并统一 **A03 Git** 面板保存 remote 的前置校验；不重复产品窗证据脚本或 P7 安装。

## 行为

### A01 设置 · 网络

- `SettingsNetworkNormalize`：加载/Electron 迁入时 trim 代理 host/用户名/密码，非法端口清空（对齐 `HttpEngine`/`TranslationEngine` 1–65535）。
- 代理 host/端口/用户名/密码与 HTTP/翻译超时改用 `SettingCommitTextField`；端口非法或超时非整数时 `toastError` 并恢复已保存值；超时失焦按 `SettingsNumericBounds` 夹紧 1000–120000 ms。

### A03 Vault Git 面板

- 保存/删除 remote 前调用 `SettingsVaultGitNormalize.commitGitRemote`；非法非空 URL 拒绝 `GitEngine.setRemote` 并 toast（与设置页同一文案键）。

## 验证

- `SettingsNetworkNormalizeTest`
- `./gradlew :composeApp:desktopTest --offline`（JDK 21）

## 未做

产品主窗冲突/Git merge/IME 截图、六套 CSS 皮肤、P7 Win/Linux 安装、`runDistributable` 烟雾、其余 F-tool 引擎大切片、设置分类逐组 UI 像素差。
