# DIFF-176：遗留 QrCode 草稿与导航显隐单测

## 行为

- **A03**：`LegacyToolDraftApplierTest.appliesQrCodeDraftFromJavaFuncName` 覆盖 Java `FuncConsts.QR_CODE` 草稿迁入首行内容。
- **A01**：`NavigationToolVisibility` 集中 25 个导航工具 ID，`NavigationToolVisibilityTest` 锁定 showAll/hideAll 语义。

## 验证

- `./gradlew :composeApp:desktopTest --offline`（**326/326**）
