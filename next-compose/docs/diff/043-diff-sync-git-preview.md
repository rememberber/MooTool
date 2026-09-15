# DIFF-043：文本对比同步滚动、Git 前后分栏与 1080 工具栏溢出

- 编号：DIFF-043
- 影响：F02 文本对比、F01/F04 Vault Git 差异预览、ui-spec 1080 工具栏密度
- 日期：2026-09-15

## 原行为（Electron）

左右编辑器独立滚动，复制绝对偏移并用锁避免回写抖动；较短一侧夹到末尾时不会把较长一侧拽回去。Git 差异是变更前/变更后两个只读编辑器，二进制与超 512 KiB 明确不可预览。1080–1439 把低频工具栏收入「更多」。

## 本产品行为

- 文本对比不再共用一个 `ScrollState`。两侧独立滚动，按绝对像素同步；短侧夹紧时不回拉长侧。
- Vault Git 对话框按 HEAD/工作区或提交 blob 读取前后文本，并排高亮预览，滚动规则与文本对比相同。
- 内容宽 < 1440 时，对比页保留比较/上一条/下一条，清空/交换/复制/导入/历史/分离收入「更多」。

## 理由

规格要求「同步滚动不会相互抖动」。共用滚动状态会把两侧绑到同一 max，短文一侧会来回拉长文一侧。Git 原先只贴 unified 文本，无法对照变更前后。

## 证据

`SyncScrollPolicyTest`、`GitEngineTest.fileDiffShowsWorkingTreeBeforeAndAfter`、`parseNameStatusAndRejectsPathTraversal`。`desktopTest` **222/222**，见 `docs/evidence/2026-09-15-diff-sync-git-preview/`。真实窗口滚动手势、IME 手工、三平台安装仍未测。

## 受影响范围

- 预览上限 512 KiB，与 Electron `maxDiffPreviewBytes` 一致；更大或含 NUL 的文件只提示，不解码乱码。
- 提交记录预览当前展示该提交的第一个变更文件；多文件下拉见 [DIFF-044](diff/044-window-copy-image-git.md)。
- 仍非 Electron 六套 CSS 逐选择器移植。
