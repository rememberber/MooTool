# 2026-09-14 布局 / 风格 / 编辑器 / 安装身份

- 已执行：`./gradlew :composeApp:desktopTest`（Zulu 21，`--offline`）
- 结果：`desktopTest` **167/167** 通过，0 skipped
- 相对上一刀（161）：+6（VaultMove、LayoutPaneSizes、VaultTree、ThemeContrast、EditorBuffer 5MiB、InstallIdentity）

## 本轮落地

- 随手记/JSON Vault 树拖放到目录或根，引擎 `move()` 真实执行
- 分栏宽度按工具写入 `layout.paneSizes`（随手记、JSON、Host、HTTP、图片、翻译、代码运行）
- 六套风格 raised/toolbar/inset/选中条；焦点环；线性图标
- 列编辑 IME 提交写入矩形选区；5 MiB 中英混排 `setText` 单测；状态栏超限提示
- DMG/MSI/DEB/RPM 声明；Windows per-user + 稳定 UpgradeCode；卸载不得碰其他产品路径

## 未测（不能标完成）

- 窗口截图：拆出窗口密度、960×640、明暗、焦点环观感
- IME / 列编辑桌面手势手工验收
- macOS arm64 / Windows x64 / Linux x64 安装、升级、卸载、公证
- 真实联网 Git 与外网工具

## 命令

```bash
cd next-compose
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
./gradlew :composeApp:desktopTest
```
