# DIFF-053：自定义分组开关与删除确认

- 编号：DIFF-053
- 影响：A01 布局/自定义分组；对照 Electron `CustomGroupManager`
- 日期：2026-09-15

## 原行为（Electron）

自定义分组在管理器里用 checkbox 按内置组勾选工具；空组提示；名称为空或未选工具有校验；删除先确认，只改导航不删数据。

## 本产品行为

- 设置 → 布局里的自定义分组：名称用设置行输入；工具按内置分组列出，开关勾选。
- 空列表、缺名称、未选工具有可见提示。
- 删除弹出确认，取消不改数据。

## 理由

此前工具成员是 `"名称: 是/否"` 按钮，删除无确认，与 Electron 检查器/设置 chrome 不一致。

## 证据

`desktopTest` **236/236**。运行窗口：布局隐藏工具见 `docs/evidence/2026-09-15-inspector-screencapture/windows/62-settings-layout.png`；自定义分组开关与空组校验见 `66-settings-custom-groups.png`。

## 受影响范围

- 仍不是独立弹层管理器（Electron 是 Dialog）；设置页内嵌同一字段。已由 [DIFF-055](055-custom-group-dialog.md) 改为弹层。
- IME/列编辑手工、托盘 TCC、三平台安装未测。
