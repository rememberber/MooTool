# 结果

平台：macOS 26.7 / x86_64。日期：2026-09-09。命令均在 `next-fx/` 下、`JAVA_HOME` 指向 Zulu 25.0.4.1 执行（smoke 除外）。

## 命令

| 命令 | exit | 观察 |
| --- | --- | --- |
| `./mvnw test` | 0 | 26 tests, 0 fail, 0 error, 0 skip |
| `./scripts/package.sh --type app-image` | 0 | 产出 `dist/MooTool Next FX.app`（约 91MiB；runtime 约 75MiB） |
| `./scripts/verify-package.sh "dist/MooTool Next FX.app"` | 0 | Bundle ID `com.rememberber.mootool.next.fx`；bundled `java` 25.0.4.1；classpath 含 jackson-databind / sqlite-jdbc / richtextfx |
| 无 `JAVA_HOME`、`PATH=/usr/bin:/bin:/usr/sbin:/sbin` 启动 app-image | 0（进程存活 ≥10s） | 见下方 smoke |

macOS 上 `/usr/bin/java` 仍是系统 stub，本机解析到同一 Zulu。判定 bundled runtime 的证据是日志加载 `jrt:/javafx.graphics` 与 `Contents/app/lib/sqlite-jdbc-3.53.2.1.jar`，而不是“机器上没有 java 可执行文件”。未在清空 `java_home` 的干净用户账户复测。

Windows `package.ps1` / `verify-package.ps1` 仅占位（exit 2）。Linux / macOS arm64 未构建。

## 单元测试覆盖

- 26 个稳定 Tool ID、6 分组、历史/收藏标志；24 个 PLACEHOLDER，仅首页与 JSON 为 IN_PROGRESS
- JSON 格式化/压缩、重复 key、`9007199254740993`、3MiB blob、JSONPath `$.store.books[1].title`、XML/JavaBean
- Find/replace 与矩形列编辑（无 GUI）
- AppPaths 隔离；SQLite window_state/history 往返；Jackson pretty-print 产品标记二次打开；拒绝外产品 `productId`

## app-image smoke

1. 首次启动（数据目录尚无本产品标记）：进程存活 8s，加载 JavaFX native 与 sqlite-jdbc。日志：`smoke-first-launch.txt`。随后 SIGTERM 结束。
2. 二次启动（修复前）：读取已有 `product.json`（`"productId" : "next-fx"`，冒号两侧空格）时字符串匹配误判为外产品，`Application.start` 抛 `IllegalStateException`。日志：`smoke-marker-false-positive.txt`。
3. 解析 JSON 修复后重新打包，对同一 release 数据目录再启动：进程存活 10s，RSS 约 562MiB，无异常。日志：`smoke-relaunch.txt`。

截图：`screencapture` 返回 `could not create image from display`（本代理环境无屏幕录制权限）。IME、50 次窗口转移、1440×920 / 1080×720 主题截图未做。

## 已知限制（不记为通过）

- 桌面 IME / 列编辑手势 / 50 次 Stage 转移：仅有单元测试或代码路径，无桌面证据
- TestFX `ui-tests` profile 已声明，尚无用例
- P0 表中的 protoc 动态 schema、Crypto 互通、JS/Java regex 差异样本未做，放到对应工具阶段
- JDK 25 `--enable-native-access` 警告仍出现在 JavaFX 与 sqlite-jdbc
