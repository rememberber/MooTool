# 结果

## 命令

```text
export PATH="$HOME/sdk/flutter/bin:$PATH"
cd next-flutter
python3 scripts/check-release.py --root .
dart format --output=none --set-exit-if-changed lib test
flutter analyze
flutter test test/unit --reporter compact
./scripts/package.sh macos
```

```text
ok version=0.1.0 notes=0.1.0.md
Analyzing next-flutter...
No issues found! (ran in 3.1s)
All tests passed!
```

`flutter test test/unit`：**94** 个通过。

`./scripts/package.sh macos`：失败，提示需要完整 Xcode。这是预期，不是伪成功。

`flutter run -d macos` 仍未作为验收。

## 范围

更新通道（只读 next-flutter、SHA-512、手动打开安装包）、未签名打包脚本、CI workflow、差异审查。明确未完成：本机/CI 实机安装包、update-manifest 节点、第二 engine、ScreenCaptureKit overlay。
