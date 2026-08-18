# ADR-008：自动更新清单

> 状态：已采纳  
> 日期：2026-08-17

## 背景

仓库同时维护 Java、Electron 与 Tauri 产品。Tauri Updater 需要平台级静态 `latest.json`，仓库又需要统一入口表达不同产品的状态和历史。直接让 Tauri 读取 Electron 清单或让多个产品共用同一稳定更新 URL，会破坏独立产品边界。

## 决策

采用两层清单：

1. 根 `update-manifest.json` 只负责产品发现。Tauri 客户端固定读取 `products.next-tauri`，验证产品名、状态和专属更新 URL；不解析其他产品节点。
2. `next-tauri-updater` Release 中的 `latest.json` 是 Tauri Updater 的稳定静态清单，包含版本、发布日期、发行说明、Release URL，以及 macOS x64/arm64、Windows x64、Linux x64 的下载 URL 与 updater 签名。
3. `next-tauri-v{version}` 首先生成 Draft Pre-release。只有人工发布后，`next-tauri-promote-update.yml` 才验证所有公开资产 URL、提升 `latest.json`，随后只更新根清单的 `products.next-tauri`。
4. 提升脚本拒绝把稳定通道指向低于现有最高版本的 Release；相同版本允许幂等重试。
5. Tauri Release 和 updater 通道始终使用 `--latest=false`，不竞争仓库级 `Latest`。

## 结果

- Java、Electron 和 Tauri 可以独立发布、回滚和演进。
- Draft 或资产不完整的 Release 不会被客户端发现。
- 首次正式提升前，`products.next-tauri.status` 保持 `planned`；首次成功提升后改为 `active`。
- 更新源固定且由 Rust Core 校验，不能由渲染层注入任意端点。
