# DIFF-411：打开自动下载时续传可用更新

## 背景

Electron `UpdateManager.setAutoDownload(true)` 在已有 `activeResult` 且状态为 `available` 时立即 `download()`。Compose 仅在 `check(autoDownload=true)` 成功路径触发下载；用户先手动检查出可用版本、再打开「有更新时自动下载」时不会开始下载。

## 行为

- `UpdateAutoDownloadTrigger` 判定是否应启动下载。
- `UpdateCoordinator.applyAutoDownloadSetting(true)` 在设置从关→开时调用。
- `AppContainer.updateSettings` 检测 `autoDownloadUpdates` 由 false→true。

## 验证

- `UpdateAutoDownloadTriggerTest`
- `./gradlew :composeApp:desktopTest --offline`
