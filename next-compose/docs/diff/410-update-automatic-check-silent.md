# DIFF-410：后台更新检查不打扰关于页 UI

## 背景

Electron `checkForUpdatesAndBroadcast(true)` 仅在 `status === 'available'` 时 `broadcast('update:checked')`；已是最新、未发布或网络失败时后台检查不推送到 UI。Compose `UpdateCoordinator.check` 此前每次定时检查都会改写 `UpdateUiState`，关于页可能被动刷新为「当前版本」或错误。

## 行为

- `UpdateCheckSurfacing`：`automatic` 时仅 `Available` 写入 UI；错误不写入。
- `UpdateAutoCheckScheduler` 调用 `check(automatic = true)`；设置页「检查更新」仍为 `automatic = false`。
- `autoDownload` 在后台发现 `Available` 时仍会下载（不依赖 UI 状态是否刷新）。

## 验证

- `UpdateCheckSurfacingTest`
- `./gradlew :composeApp:desktopTest --offline`
