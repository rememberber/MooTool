# DIFF-306：Host 单次替换成功清空状态栏 notice

## 背景

[DIFF-301](301-host-quicknote-replace-all-notice.md) 已让 Host「全部替换」成功时 `notice = ""`（不写 matches 文案）。单次「替换」成功此前只更新正文与 `findReplacedCount`，仍可能保留旧的「已保存/已导出」等 `notice`；JSON 查找「替换」成功会清空 `notice` 与 `copyState`（对齐 Electron `replaceCurrent`）。

Electron `HostTool` 无状态栏 `notice` 字段；Compose 在替换类操作后统一清空提示，与 Host 全部替换及 JSON 查找条一致。

## 行为

- **F10**：查找条单次「替换」成功：`session.notice = ""`（与全部替换相同）。

## 验证

- 对照 `next/src/features/host/HostTool.tsx` `replaceCurrent`（无 notice）与 Compose JSON `FindBar` 替换分支
- `./gradlew :composeApp:desktopTest --offline`
