# ADR-001：macOS jpackage 版本映射

- 状态：已采用
- 日期：2026-09-09

## 背景

产品 `appVersion` 为 `0.1.0`。本机 JDK 21 `jpackage` 在生成 macOS app-image 时拒绝 `--app-version 0.1.0`，错误为“第一个数字不能为零或负数”。

## 决定

- UI、`product.json`、文档和 Git tag 继续使用 `gradle.properties` 的 `appVersion`。
- Compose `nativeDistributions.packageVersion` 在 `0.x.y` 时映射为 `1.x.y`（本轮 `0.1.0` → `1.1.0`）。
- 这只影响安装包元数据，不改变应用内显示版本。

## 后果

Windows/Linux 安装包若使用同一 `packageVersion` 也会显示 1.1.0。后续若 macOS 工具链允许 0.x，应改回单一版本并记录迁移。
