# 结果

## 命令

```text
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
export PATH="$HOME/sdk/flutter/bin:$PATH"
cd next-flutter
./scripts/package.sh macos
```

```text
✓ Built build/macos/Build/Products/Release/MooTool Next Flutter.app (56.5MB)
created: dist/MooTool-Next-Flutter-0.1.0-mac-x64.dmg
ok MooTool-Next-Flutter-0.1.0-mac-x64.dmg sha512-b64=yNVRxlZbJOF79jliKN0W1d5hZ+Yu22gKb6NUOFtiO6uRi3MK0ALSCfYf5aZ7nezt3Cu7v6hbgdCPnAzGqGkG+g== size=25041818
```

Release `.app` 用 `--data-dir` 临时目录启动约 6 秒后仍在运行，并写入 `product.json`（`productId=next-flutter`）。随后结束进程。未打开用户真实数据目录。

`codesign`：adhoc，无 TeamIdentifier。`spctl --assess`：rejected。符合未签名/不公证策略，不是 Developer ID。

DMG 不提交进 git（`dist/` 已忽略）。

编译时补了缺失的 `quitFromTray`，否则 Release 构建失败。
