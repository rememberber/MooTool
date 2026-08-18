# MooTool Next Tauri 独立发布手册

## 产品边界

- 应用名：`MooTool Next Tauri`
- Bundle ID：`com.rememberber.mootool.next.tauri`
- Git 标签：`next-tauri-v{version}`
- 版本 Release：Draft + Pre-release，显式使用 `--latest=false`
- 稳定更新通道：`next-tauri-updater`，同样不竞争仓库级 `Latest`
- 根清单：只允许发布脚本修改 `update-manifest.json` 的 `products.next-tauri`
- Java/Electron 的版本、标签、资产、数据和清单节点不在本流程范围内

架构决策见 [ADR-008](adr/008-updater-manifest.md) 与 [ADR-009](adr/009-code-signing.md)。

## 本地门禁

在 `next-tauri` 目录执行：

```bash
npm ci
npm run check
npm audit --audit-level=high
```

`npm run check` 覆盖发布边界、TypeScript、全部前端和 Rust 测试、生产 Web 构建、Rustfmt 与 Clippy。版本必须在以下位置一致：

- `package.json`
- `src-tauri/Cargo.toml`
- `src-tauri/tauri.conf.json`
- `release-notes/{version}.md`

本地 macOS updater 签名构建示例：

```bash
TAURI_SIGNING_PRIVATE_KEY="$HOME/.tauri/mootool-next-tauri.key" \
TAURI_SIGNING_PRIVATE_KEY_PASSWORD="$(<"$HOME/.tauri/mootool-next-tauri.key.password")" \
APPLE_SIGNING_IDENTITY=- \
npx tauri build --bundles app,dmg
```

私钥和密码不得进入仓库、终端日志、构建 Artifact 或诊断包。`APPLE_SIGNING_IDENTITY=-` 仅为 ad-hoc 开发预览；受信任的 macOS 发行必须配置 Developer ID 和公证。

## CI、Secrets 与产物

- `next-tauri-ci.yml` 在 macOS、Windows、Linux 对 `next-tauri/**` 变更执行完整门禁。
- `next-tauri-release.yml` 对现有 `next-tauri-v*` 标签构建 macOS x64/arm64、Windows x64、Linux x64。
- 每个平台在上传前执行包完整性与首次启动冒烟；Windows 还执行静默安装和卸载。
- 四个平台完成后统一命名安装包，生成 `latest.json` 和 `next-tauri-release.json`，再创建或更新独立 Draft Pre-release。
- `next-tauri-promote-update.yml` 只在该 Draft 被人工发布后运行，验证公开 URL、更新稳定 `latest.json`，最后提交根清单的 Tauri 节点。

必需 Secrets：

- `TAURI_SIGNING_PRIVATE_KEY`
- `TAURI_SIGNING_PRIVATE_KEY_PASSWORD`

可选于 0.x 预览、受信任正式发行必需的 Secrets：

- Apple：`APPLE_CERTIFICATE`、`APPLE_CERTIFICATE_PASSWORD`、`APPLE_SIGNING_IDENTITY`、`APPLE_ID`、`APPLE_PASSWORD`、`APPLE_TEAM_ID`
- Windows：`WINDOWS_CERTIFICATE`、`WINDOWS_CERTIFICATE_PASSWORD`、`WINDOWS_TIMESTAMP_URL`

将本地 updater 密钥配置到 GitHub 时使用标准输入，避免把内容放入命令参数或输出：

```bash
gh secret set TAURI_SIGNING_PRIVATE_KEY < "$HOME/.tauri/mootool-next-tauri.key"
gh secret set TAURI_SIGNING_PRIVATE_KEY_PASSWORD < "$HOME/.tauri/mootool-next-tauri.key.password"
```

仓库中的 `src-tauri/tauri.conf.json` 只保存公钥。修改公钥必须走密钥轮换方案，不能直接替换后发布。

## 发布步骤

1. 更新三个版本文件和 `release-notes/{version}.md`；Release Notes 必须明确 Tauri 与 Java/Electron 独立安装、独立更新、不替代其他实现。
2. 执行本地门禁；至少在当前开发平台完成一次带 updater 签名的实际 bundle 构建。
3. 确认必需 Secrets 已配置；正式受信任发行同时确认 Apple/Windows 平台证书与公证材料。
4. 创建并推送 `next-tauri-v{version}` 标签，或对该现有标签手工触发 Release 工作流。
5. 检查四个平台任务、安装/启动冒烟、规范化资产、SHA-512、`.sig`、`latest.json` 和 `next-tauri-release.json`。
6. 审核 Draft 正文、平台限制和下载清单。不得将 Release 改为仓库级 `Latest`。
7. 人工发布为 Pre-release。该事件自动触发 updater 通道提升；不要手工提前把根清单状态改为 `active`。
8. 确认提升任务验证了全部公开资产，`next-tauri-updater/latest.json` 与已发布版本一致，并且根清单仅新增或更新 `products.next-tauri`。
9. 在已安装的上一版本中执行一次检查、下载、取消、重试、安装和重启验收；确认相同或更低版本不会提示为更新。

## 回滚与故障处理

- Draft 未发布：修复源码后重新运行同一标签，工作流只允许覆盖仍为 Draft 的 Release。
- 已发布但尚未提升：修复公开资产或发布更高补丁版本；不要手工激活根清单。
- 已提升：不覆盖既有安装包、不把稳定通道降级到旧版本；撤下有问题的 Release，并发布更高补丁版本修复。
- updater 私钥丢失：停止发布，执行经过评审的密钥轮换和人工安装方案；不得无提示更换公钥。
- 数据恢复：在设置窗口选择完整备份目录。恢复前生成的 `restore-rollbacks` 可用于人工回滚；JSON Vault 恢复到 Tauri 自有 `imported-vaults`，不会覆盖原外部 Vault。
