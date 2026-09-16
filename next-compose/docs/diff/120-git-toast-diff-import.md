# DIFF-120：Vault Git toast 与文本对比导入 toast

对照 Electron `VaultGitDialog.tsx` 的 `toast.success` / `toast.error` 与全局导入反馈。

## Vault Git（JSON / 随手记）

- `runAction` 成功：`toastSuccess(git.done)`（保留对话框内 notice）。
- `runAction` 失败：`toastError`（含 pull 失败仍 `onVaultRefresh` 的行为不变）。
- `load` / `runAction` 前 flush 失败：`toastError`。

## F02 文本对比

- 左/右导入（工具栏与溢出菜单）成功：`toastSuccess(json.notice.imported)` + 状态栏文案。

## 文件

- `VaultGitDialog.kt`、`TextDiffScreen.kt`
