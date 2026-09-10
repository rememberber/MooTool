# 对照

已对齐：更新只读 `next-flutter`；无节点视为未发布；稳定/预发布隔离；无匹配资产只给本产品 Release；下载校验 SHA-512 与长度；手动打开安装包；未签名不得报自动安装成功。

已知差异：

- 仓库 `update-manifest.json` 尚未增加 `next-flutter` 节点（等资产可访问）。
- 本机已产出未签名 `MooTool-Next-Flutter-0.1.0-mac-x64.dmg`（见 `docs/evidence/2026-09-09-p7b/`）；Windows/Linux 未在本机构建。
- AppImage / Windows setup.exe 依赖 CI 上的 appimagetool / Inno Setup，缺工具时脚本跳过该产物并说明。
- 未做五产品并存实机安装/卸载。
- 仍无第二 Flutter engine。
