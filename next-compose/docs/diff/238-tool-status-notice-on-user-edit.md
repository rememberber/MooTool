# DIFF-238：多工具用户编辑清空状态栏 notice

## 背景

JSON（DIFF-227）与随手记（DIFF-230）已在用户编辑时清空状态栏 `notice`，避免「已保存/已复制」等旧提示盖住新输入。Electron 编辑 `onChange` 同样清空 `notice`。

## 行为

- 抽取 `applyUserEditClearingStatusNotice`（`ToolStatusNotice.kt`）。
- Host、编码、UA、正则、格式化、文本对比、配置、Cron、计算器、翻译源文、二维码内容等主输入 `onChange` 接入。
- 续：加解密（`LabeledField`）、Protobuf 各栏、HTTP URL/Body/Params/Headers/Cookies、时间戳/本地时间、调色板色码、格式化文件 Tab 原文、环境变量编辑框；时间状态栏 notice 非空才显示。
- 代码运行：用户改源码时清空 `error`（`clearErrorOnUserEdit`），无 notice 字段。
- 续见 [DIFF-239](239-tool-status-notice-pdf-image-message.md)（PDF / 图片 / 留言板）。

## 验证

- `ToolStatusNoticeTest`
- `./gradlew :composeApp:desktopTest --offline`
