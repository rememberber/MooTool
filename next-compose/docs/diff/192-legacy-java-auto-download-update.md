# DIFF-192：Java 自动下载更新迁入

## 背景

Java `setting.common.autoDownloadUpdate` 对应 Compose `general.autoDownloadUpdates`（见 [DIFF-027](027-update-channel-open-installer.md)）。

## 行为

`LegacyJavaSettings.applyPatch` 在存在该键时写入布尔值（默认 false，与 Compose 产品默认一致；Java 默认 true 仅在键缺失时由 Java 运行时决定，迁移以配置中显式值为准）。

## 证据

- `LegacyJavaSettingsTest.parsesIniGroupsAndAppliesLanguageAndLayout`
