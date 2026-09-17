# Electron 托盘取色/截图 → Compose 对照

Electron 托盘入口经主进程调屏幕捕获；Compose 为 `TrayDesktopActions` + `ScreenColorSampler.captureAllScreens()` 重试与 macOS 隐私设置深链。

| 场景 | Electron（行为） | Compose |
| --- | --- | --- |
| 截屏权限被拒 | 系统/主进程错误提示 | `ScreenCaptureFailureMessages.trayCaptureMessage` + `ColorException(permission)`（[DIFF-527](../diff/527-tray-permission-pdf-outline-http-slice.md)） |
| 打开系统屏幕录制设置 | macOS 隐私面板 | `ScreenCaptureAccess.openPrivacySettingsIfNeeded` + `color.error.permissionSettings` |
| TCC 产品窗证据 | （手工 PNG；脚本恢复基线帧） | `prepare-tray-screencapture-evidence.sh` + `reference/57-color-baseline.png`（[DIFF-528](../diff/528-http-multipart-editor-tray-git-slice.md)） |
| 图片工具区域截图失败 | 工具页 error 行 | `ScreenCaptureFailureMessages.imageOperationMessage`（permission 与 `image.error.*` 同路径） |

手工：托盘菜单「屏幕取色」「区域截图」在**未授权**与**授权后**各走查一次（parity-gap 仍列未测）。
