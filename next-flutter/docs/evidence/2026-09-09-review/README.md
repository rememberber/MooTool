# 2026-09-09 独立审查证据

审查结论和代码位置见 [完整报告](../../review-2026-09-09.md)。本目录由本轮审查新增，不是 Cursor 先前交付的通过报告。

## 环境

- 工作区 `/Users/zhoubo/IdeaProjectsCE/MooTool/next-flutter`。
- HEAD `dd9555554f91e4037d94398e40eced34a8f31c34`。
- 隔离构建归档提交 `693b50b6f5b040830d5b68e9605d4ea88f6b0653`；两提交之间的 `next-flutter/` 无差异。
- macOS 26.7（25G224），x86_64。
- Flutter 3.47.2 / Dart 3.13.2；SDK `/Users/zhoubo/sdk/flutter`。
- Xcode 路径 `/Applications/Xcode.app/Contents/Developer`；没有修改全局 xcode-select。
- 探针源目录 `/tmp/mootool-flutter-review.6VnMrU`；业务测试数据由每个用例另建临时目录。
- 审查不包含用户真实笔记、token、密码或其他产品数据。子进程测试有 finally 清理。

## 文件与结果

| 文件 | 内容 |
| --- | --- |
| [review-probes.txt](review-probes.txt) | R02–R10 共 9 个用例的原始失败日志 |
| [extra-review.txt](extra-review.txt) | R13 子进程取消、R15 外产品目录保护的失败日志 |
| [editor-review.txt](editor-review.txt) | 单独执行 R12，期望选区 6–10，实际仍为 0 |
| [theme-review.txt](theme-review.txt) | R17 字号 14 引发主题构建断言 |
| [desktop-binding.txt](desktop-binding.txt) | 本机 Flutter SDK 对 Release 中 debugBindingType 返回值的源码说明 |
| [widget-review-pages.txt](widget-review-pages.txt) | 两种宽度的页面检查日志截取；每种都遍历 26 个工具和设置首页，发现导航 Material 层级问题 |
| [release-build.txt](release-build.txt) | 原工作区直接 Release 构建失败输出；存在共享生成目录干扰，不作为稳定构建失败结论 |
| [package-build.txt](package-build.txt) | 原工作区 `package.sh macos` 失败输出；同样需隔离复核 |
| [isolated-package-build.txt](isolated-package-build.txt) | Git 归档提取到全新目录后，正式打包脚本通过，退出码 0；这是最终构建判断依据 |
| [isolated-build-path.txt](isolated-build-path.txt) | 本轮临时构建目录，内含新生成的 `.app` 与 DMG；未安装或发布 |
| `*_test.dart.txt` | 与上述问题对应的审查探针；保存为文本，未加入默认测试集合 |
| [screenshots/](screenshots/) | 1440×900、DPR=1、默认浅色中文的真实 Widget 渲染图 |

Widget 图在测试进程中显式加载 Arial Unicode 与 MaterialIcons，避免 Flutter 测试默认占位字体影响阅读。它们用于查看组件结构，不代表原生窗口截图或系统排版验收。工具页面以控制器设置入口后构建；没有用截图宣称已验证导航点击、原生插件或 IME。

`widget-review-pages.txt` 仅保留页面检查两项输出。编辑器 R12 在字号为默认 13 的条件下单独复核，最终证据以 `editor-review.txt` 为准；非默认字号造成的独立问题另列为 R17。

隔离构建生成 `MooTool-Next-Flutter-0.1.0-mac-x64.dmg`，25,420,217 字节，SHA-512（Base64）：`tZ02CuPmGFjFv9EI/EGJxBfY51DLSHHWkE5xo2I5iXMXDahoU0iNKLk1+lrbfAUJAlD3B5WM6VxYEC3m3qBu1g==`。干净构建已通过；先前共享生成目录时的两份失败日志不能作为源码无法构建的证据。

## 原有检查通过记录

本轮执行 `./scripts/check.sh` 返回退出码 0。工具输出关键行如下；此处是结果摘录，不是完整 stdout 存档：

```text
Formatted 95 files (0 changed) in 2.76 seconds.
No issues found! (ran in 577.2s)
00:16 +94: All tests passed!
```

这些通过结果与新增失败探针并不矛盾：原有测试未覆盖对应场景。未因这些失败修改实现或更改原有单测预期。

## 重跑方式

从 Flutter 产品根目录执行。源文件带 `.txt` 后缀，先复制到自己的临时目录：

```sh
cd /Users/zhoubo/IdeaProjectsCE/MooTool/next-flutter
review_tmp="$(mktemp -d /tmp/mootool-review-rerun.XXXXXX)"
cp docs/evidence/2026-09-09-review/review_probes_test.dart.txt "$review_tmp/review_probes_test.dart"
cp docs/evidence/2026-09-09-review/extra_review_test.dart.txt "$review_tmp/extra_review_test.dart"
cp docs/evidence/2026-09-09-review/widget_review_test.dart.txt "$review_tmp/widget_review_test.dart"
cp docs/evidence/2026-09-09-review/theme_review_test.dart.txt "$review_tmp/theme_review_test.dart"
/Users/zhoubo/sdk/flutter/bin/flutter test "$review_tmp/review_probes_test.dart" --reporter expanded
/Users/zhoubo/sdk/flutter/bin/flutter test "$review_tmp/extra_review_test.dart" --reporter expanded
/Users/zhoubo/sdk/flutter/bin/flutter test "$review_tmp/widget_review_test.dart" --plain-name 'R12 selection-only find result reaches visible editor' --reporter expanded
/Users/zhoubo/sdk/flutter/bin/flutter test "$review_tmp/theme_review_test.dart" --reporter expanded
```

运行时探针使用本机 `/usr/bin/python3` 和 `/bin/kill`，不适用于 Windows。页面渲染探针有本机字体/SDK/截图输出路径；重跑整套页面前，将其中截图输出目录换成自己的 `review_tmp`，并按本机路径调整字体资源，然后运行：

```sh
/Users/zhoubo/sdk/flutter/bin/flutter test "$review_tmp/widget_review_test.dart" --plain-name 'all tool pages fit' --reporter expanded
```

源码修复前预期这些探针以失败退出。修复后应保留“正确行为”的断言，并迁入合适的正式 unit/widget/integration 测试位置；不要将失败日志数量当作产品缺陷数量或通过率。
