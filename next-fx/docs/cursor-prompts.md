# Cursor 可复制任务提示词

## 1. 首次启动

```text
请在 /Users/zhoubo/IdeaProjectsCE/MooTool/next-fx 开发 MooTool Next FX。

先读 next-fx/AGENTS.md、README.md 和 docs/cursor-development-guide.md，再读 baseline.md、ui-spec.md、feature-parity.md、architecture.md、data-platform-release.md、acceptance.md。P0 Maven 工程已存在时沿用，不要重建；文档里尚未落地的拟建 API 仍不能当成已实现。

目标是 OpenJDK + JavaFX 桌面产品，布局、样式、功能尽量对齐 next/ Electron 1.1.4，同时改善现代桌面 UI。首页和25工具、11类设置、编辑器、多窗口、文件库、Git、平台与发行都在最终范围。

各版本是独立产品线，允许重复代码、允许独立演进。默认只修改next-fx，其他目录用于阅读；不继承根POM，不依赖相邻源码/构建产物，不共用数据库/凭据/锁/安装身份/更新通道。不修改或清理其他人的工作树。

请实际完成P0：冻结源码基线，建立独立Maven Wrapper工程和最小JavaFX应用、身份与路径、组件/编辑器实验、真实JSON操作、窗口分离/收回、SQLite和本机app-image。重点验证中文IME、列编辑/undo、3–5MiB文件、转移状态；原生编辑器有门槛失败时按规格测试备选并记录ADR。完成必要测试与打包启动检查，更新acceptance.md和证据；未测平台如实记录。不要只提交计划，也不要一轮堆满25个占位页面后声称完成。
```

## 2. 继续开发

```text
继续 next-fx。先读AGENTS.md、acceptance.md、最近证据和已有ADR，检查本产品及仓库工作树。沿用已经验证的技术决定，从最早未完成且前置条件齐备的阶段/子项继续，不重建工程、不重选全部依赖。

完成该批次真实功能、必要错误/取消/持久化/窗口场景，并对照Electron源码和UI规格。保持各产品独立，允许本地副本，不引入跨产品构建或运行依赖。完成后更新状态、证据和差异，说明实际通过的验证、未测平台和下一项。
```

## 3. 开发单个工具

将占位项替换成目标，例如`F09 / http`。

```text
在next-fx实现 <Fxx / Tool ID>，先读feature-parity.md对应条目以及源TSX、算法、服务/契约、测试，必要时读旧Java纯算法。把按钮/Tab/参数/默认值/快捷键/历史/收藏/错误/平台行为拆成子项。

按现有JavaFX壳、EditorHost、任务/存储契约实现真实操作，不新造一套界面主题，不引入另一产品依赖。为关键语义建立固定fixture和有效集成场景；完成空态、忙态、取消、错误保留输入、主题/语言、切页/分离保留状态。

实际测试、截图对齐并更新acceptance和子项证据。源行为与Java库默认不同不能静默改变，记录sourceObserved/fxExpected和FX差异编号；关键子项未完成不能把整个工具标通过。
```

## 4. UI对齐与现代化修订

```text
对next-fx当前已实现页面做UI修订。以冻结Electron版本和ui-spec.md为依据，先固定相同客户区、DPI、字体、数据、语言、风格和主题，保存成对截图。

先修导航/面板/工具栏的结构与主要动作位置，再修文字层级、间距、对比度、圆角、图标/命中区、焦点、空错忙状态；保留成熟桌面工具密度。真实JavaFX控件和公共Token实现，不能拿图片/整页WebView替代UI。

验证1440×920、1080×720及独立窗口760×560，modern浅/深、中文/英文/日文；菜单/弹窗/编辑器也随主题。主题、字号、布局调整不能清空文档、undo或滚动。截图和行为同时复核，记录有意改进和平台窗口装饰差异。
```

## 5. 编辑器和多窗口专项

```text
专项检查next-fx EditorHost/DocumentSession/ToolWindowCoordinator。复现中文IME、emoji/Tab/软换行、20行矩形编辑、查找替换、大文档和undo/redo，再执行50次分离/收回/切页/主题切换。

要求同一工具会话只在一个宿主容器中，同一编辑器转移不被销毁；关闭工具窗口收回，快捷键只执行一次，弹窗归正确Stage，旧任务结果不能覆盖新revision。找出实际失败和泄漏原因后修复，保留有效回归场景及真实桌面证据。若平台未能测试，明确剩余项。
```

## 6. 共存与发布准备

```text
对next-fx做P7发布准备与产品隔离检查。先读取本产品数据/平台/发布规格和当前根RELEASE_CONVENTIONS.md，保留他人改动；以增量方式准备FX自己的CI、版本、安装metadata、release-notes及清单更新逻辑。

核对productId=next-fx、MooTool Next FX应用名、Bundle/AppID、Windows UpgradeCode、Linux包/桌面文件、配置/数据库/Vault/凭据/锁/热键/文件关联和更新products.next-fx节点。安装包自带runtime，worker/驱动/原生库在无系统Java环境可用。

用干净用户/VM做与Java、Electron及可用其他产品的同时安装/运行/升级/卸载和数据保留测试。准备构建产物、hash/size、证据和三语发行说明，不填假下载链接，不读取全局Latest，不改其他产品节点。此任务是发布准备，除非另有明确发布授权，不推送tag或发布Release。
```

## 7. 只审查，不改代码

```text
审查next-fx的实际实现与开发规格。核对首页/25工具/11设置的细项，重点找假按钮/假数据、算法语义偏差、FX线程阻塞、输入法/undo丢失、窗口生命周期、数据覆盖及产品间依赖。

只报告有证据的问题：严重程度、文件/行、触发步骤、预期/实际、用户影响与建议修复；列出未能验证的平台。不要把相邻产品状态当FX状态，不因为总测试数大就认定全功能通过。本轮只审查，不修改文件。
```
