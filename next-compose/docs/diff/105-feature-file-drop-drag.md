# DIFF-105：file-drop 系统文件拖放

- 编号：DIFF-105
- 影响：F03 格式化；F14 加密摘要；F17 二维码；`FileDropRow` / `desktopFileDropTarget`
- 日期：2026-09-15

## 原行为（Electron）

- `.file-drop-row` 以选择按钮 + 文件名为主；本产品此前仅按钮选文件，无 OS 拖放

## 本产品行为

- 新增 `desktopFileDropTarget`（Compose `dragAndDropTarget` + `DragData.FilesList`），拖入时 accent 描边
- `FileDropRow` 支持 `onDropFiles` / `acceptMultiple` / `p5Toolbar`
- **格式化**文件 Tab：拖入单文件加载 UTF-8 源（与选文件同 `loadSourceFile`）
- **二维码** Logo / 识别：拖入图片走 `applyLogoFile` / `applyRecognitionFile`
- **加密摘要**工具栏行：拖入文件走 `digestPickedFile`（与「文件摘要」按钮同路径）

## 证据

`desktopTest` 见 `docs/acceptance.md`（拖放手势需产品窗手工验收）。

## 未做

PDF/图片库等未使用 `FileDropRow` 的入口仍未统一拖放；产品窗拖放录屏、三平台安装仍未测。
