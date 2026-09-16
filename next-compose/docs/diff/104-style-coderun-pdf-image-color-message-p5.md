# DIFF-104：运行台 / PDF / 图片 / 调色板 / 留言板 P5 按钮

- 编号：DIFF-104
- 影响：F05 代码运行；F19 留言板；F22 调色板；F23 图片；F24 PDF；`OverflowActionCluster`
- 日期：2026-09-15

## 原行为（Electron）

- P5 工具 Tab 行与主操作条为 `panel-command`（约 30px）；溢出菜单触发钮同密度

## 本产品行为

- `OverflowActionCluster` 增加可选 `p5Toolbar`，展开/平铺按钮均传递
- **运行台**：Java/Groovy 模式、运行/停止、检测/选项/格式化/清空溢出、工作目录
- **PDF**：添加任务/文件、取消、开始拆分/合并、拆分规则下拉
- **图片**：主工具栏、列表操作、缩放条
- **调色板**：取色/格式/应用/复制收藏、对比交换、五运算、主题切换
- **留言板**：对齐下拉、展示/退出沉浸

## 证据

`desktopTest` 见 `docs/acceptance.md`。

## 未做

弹层内确认/取消仍为默认密度；file-drop 拖放、产品窗 Tab/IME、三平台安装仍未测。
