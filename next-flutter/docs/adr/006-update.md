# ADR 006：更新

- 状态：未实现
- 日期：2026-09-08

首个可运行版本不启用更新通道，不修改仓库 `update-manifest.json`，不创建 GitHub Release。未来 tag 为 `next-flutter-v{semver}`，Release `make_latest: false`。安装包命名遵循主指南。签名策略独立记录；未签名预览不得提示自动安装成功。
