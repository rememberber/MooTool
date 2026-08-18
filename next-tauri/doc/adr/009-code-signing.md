# ADR-009：代码签名策略

> 状态：已采纳  
> 日期：2026-08-17

## 背景

Tauri updater 签名用于验证下载内容，Apple Developer ID/公证与 Windows Authenticode 用于建立操作系统信任。三者解决的问题不同，不能互相替代。开发预览还需要在没有商业平台证书时持续生成可测试产物。

## 决策

1. 所有可更新包必须使用 Tauri 产品线独占的 updater 私钥签名；缺少私钥、密码或 `.sig` 时发布流水线立即失败。
2. updater 私钥只保存在受限离线备份和 GitHub Actions Secrets 中。仓库只保存对应公钥；Java/Electron 不共用该密钥。
3. macOS 0.x 开发预览允许 ad-hoc 签名，Windows 0.x 开发预览允许缺少 Authenticode，但 Release Notes 必须明确系统信任警告；Linux 继续通过包完整性与 updater 签名校验。
4. 面向普通用户的正式受信任发行必须配置 Apple Developer ID、macOS 公证和 Windows Authenticode。工作流仅从 Secrets 注入证书，不把证书或密码写入仓库。
5. 发布矩阵在上传前验证 updater 签名文件，并执行 macOS DMG/签名/首次启动、Windows 安装/首次启动/卸载、Linux 包检查/AppImage 首次启动冒烟。
6. updater 密钥丢失时不得静默更换。必须发布带旧密钥信任链的过渡版本，或通过独立安全公告和人工安装流程轮换。

## 结果

- updater 完整性是不可关闭的发布门禁。
- 开发预览不因外部证书采购阻断，但用户能明确知道操作系统层的信任限制。
- 正式发行的证书准备属于发布运营条件，签名与验收逻辑已经在产品线代码和 CI 中独立实现。
