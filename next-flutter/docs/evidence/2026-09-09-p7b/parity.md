# 对照

已对齐：本机产出未签名 `MooTool-Next-Flutter-0.1.0-mac-x64.dmg`；应用 ID `com.rememberber.mootool.next.flutter`；临时 `--data-dir` 可启动。

已知差异：

- 二进制为 universal（x86_64 + arm64），文件名按本机 `uname -m` 记为 `mac-x64`。
- 本地签名是 adhoc，Gatekeeper 会拒绝；需在 Finder 中右键打开。
- Windows/Linux 安装包仍未在本机构建。
- 未写入 `update-manifest.json` 的 `next-flutter` 节点。
- 未做拖入 /Applications 的完整安装/卸载循环。
