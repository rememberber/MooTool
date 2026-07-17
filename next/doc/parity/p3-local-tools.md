# P3 纯本地工具 Parity Checklist

> 阶段：P3
> 状态：实现完成，待 Java 对照验收
> Java 基线：当前 `github-develop-ts` 分支 Java 版
> 最后更新：2026-07-15

## 1. 工具清单

| 工具 | Java 布局/能力基线 | Next 实现 | 状态 |
| --- | --- | --- | --- |
| 时间转换 | 双向时间戳、时区、快捷时间、历史、全屏钟 | 秒/毫秒双向转换、时区与 DST、实时/快捷时间、历史、全屏钟 | 待对齐验收 |
| 编码解码 | 多转换 Tab、上下输入输出、历史 | Unicode、URL UTF-8/GB2312、UTF-8 Hex、ASCII 十/十六进制、历史 | 待对齐验收 |
| UA 分析 | 输入、解析结果、预设、历史 | 浏览器、引擎、系统、设备、移动端/Bot 判断、预设与历史 | 待对齐验收 |
| 计算器 | 表达式、进制、GCD/LCM、排列组合、历史 | 安全表达式求值、进制转换、GCD/LCM、排列组合、本地记录 | 待对齐验收 |
| 正则 | 匹配测试、常用正则、收藏、历史 | flags、匹配/分组结果、21 个常用 Java 正则、SQLite 收藏与历史 | 待对齐验收 |
| Cron | 秒到年构建器、解析、运行时间、收藏 | Quartz 字段、反向解析、快捷预设、时区/DST、后续 10 次运行、自然语言、收藏/历史 | 待对齐验收 |
| 文本 Diff | 左右输入、同步滚动、Unified Tab、操作栏 | 编辑器内行/字符双层高亮、三种高亮模式、Unified、忽略空白、同步滚动、差异导航、历史 | 待对齐验收 |
| 配置转换 | Properties/YAML 转换、校验、历史 | 点路径/数组路径双向转换、YAML 校验/格式化、历史 | 待对齐验收 |

## 2. 共享基础设施

- 八个页面由 `toolRegistry` 懒加载，并统一使用 Tool Page、Toolbar、Tabs、Tooltip 和 Toast。
- 历史复用 Java 兼容的 SQLite `t_func_history` Repository。
- 正则和 Cron 收藏写入 SQLite `t_next_favorite`，通过 typed IPC 暴露给 Renderer。
- 所有新增界面均提供简体中文、英文和日文文案，并支持明暗主题跟随系统。
- 1080 宽度下 Cron、Diff 等密集页面响应式重排，核心操作不隐藏。

## 3. 布局与等价说明

- [x] 页面只保留主标题，无小标题或面包屑。
- [x] 编码与配置转换保留“左输入、中操作、右输出”的旧版操作心智。
- [x] UA 保留输入与结构化结果分区；计算器保留紧凑的多运算面板。
- [x] Cron 保留秒到年字段，但使用响应式字段网格替代 Swing 的密集单选/复选面板；表达式反向解析支持 `L`、`#`、范围和步长。
- [x] Diff 在左右可编辑区内直接渲染行级与字符级差异；Unified 模式保留双栏并追加统一差异区，同时支持同步滚动和循环差异导航。
- [x] UA 解析由标准库完成，个别浏览器/引擎名称会按库规则规范化，语义能力不低于旧版。
- [x] 1440 浅色、1080 x 720 最小窗口和代表性深色页面已建立视觉基线。

截图位置：`doc/screenshots/p3/`

## 4. 自动化证据

- P3 logic 单元测试覆盖时间、编码、UA、计算器、正则、Cron、Diff 和配置转换。
- `src/shared/favoriteRepository.integration.test.ts` 覆盖 SQLite 收藏新增、读取、去重与删除。
- `tests/electron/app.spec.ts` 覆盖八个工具的关键工作流、正则/Cron 收藏和 Diff 同步滚动。
- `npm run typecheck`、`npm run test`、`npm run test:e2e`、`npm run build` 已通过。
- Vitest：15 个测试文件、42 项测试通过。
- Electron Playwright：6 项 E2E 通过。
- React Doctor：`100 / 100`，无诊断问题。
- 浏览器/CDP 检查：八页在 1080 x 720 下无窗口级溢出，明暗主题与中英日标题切换正常，控制台无错误。

## 5. 共享验收跟踪

- [ ] 使用 Java 版同尺寸截图完成八个工具的逐页人工 parity sign-off。
- [ ] 用旧版边界输入样本补充 golden fixtures，记录库实现导致的等价输出差异。
- [ ] 旧版收藏 schema 与用户数据迁移纳入全局数据迁移阶段，不重复耦合到各工具页面。

以上项目属于发布前共享 parity 验收和数据迁移工作，不影响 P3 功能实现阶段已完成的结论。
