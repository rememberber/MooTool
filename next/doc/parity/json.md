# JSON 工具 Parity Checklist

> 工具：`json` / JSON  
> 阶段：P2 + P7  
> 状态：实现完成，待 Java 人工布局签收  
> Java 基线：当前 `github-develop-ts` 分支 Java 版  
> 最后更新：2026-07-16

## 1. 功能清单

| 功能 | Java 基线 | Next 当前状态 | 状态 |
| --- | --- | --- | --- |
| 格式化/压缩 | 缩进与压缩 | 2/4 空格、自定义格式 | 已实现 |
| 自动换行 | 工具栏切换 | 工具栏切换，读取编辑器设置 | 已实现 |
| 复制/清空 | 工具栏操作 | Tooltip + Toast | 已实现 |
| JSON/字符串转义 | 转义、还原 | JSON 字符串与 Java 字符串路径 | 已实现 |
| 语法校验 | 显示解析状态 | 实时状态与错误 | 已实现 |
| Key 排序/忽略大小写 | 支持 | 递归排序 | 已实现 |
| 重复 Key 检查 | 支持 | 保留路径的重复检测 | 已实现 |
| Key/Value 互换 | 支持 | Object 递归互换 | 已实现 |
| JSON/XML | 双向转换 | `fast-xml-parser` | 已实现 |
| JavaBean | 双向转换 | 字段提取与合法嵌套 Java 类生成 | 已实现 |
| JSON Path | 查询与结果 | `jsonpath-plus` | 已实现 |
| 可视化选择器 | 树 + 预览 | 树、路径、值预览与双击 | 已实现 |
| 文件导入/导出 | 支持 | 受限 typed IPC，20 MB 上限 | 已实现 |
| 查找 | `Ctrl/Cmd + F` | 计数、回车下一个 | 已实现 |
| 历史记录 | SQLite | Java 兼容 `t_func_history` | 已实现 |
| JSON Vault 基础 | 文件树 | 嵌套路径、打开、新建、保存、删除、未保存保护 | 已实现 |
| JSON Vault 高级 | 文件夹、重命名、拖拽、排序、监视 | 文件夹、重命名、复制、移动、拖拽、排序、忽略规则、watcher | 已实现 |
| Git 主流程 | 状态、提交、同步、历史、Diff | 初始化、远程、commit/fetch/pull/push、历史、Diff | 已实现 |
| Git 高级 | 冲突、撤销、中止合并、快捷/自动提交、自动拉取 | 冲突动作、撤销、中止合并、自动提交和自动拉取 | 已实现 |
| Java 旧数据 | SQLite + 文件 Vault | 只读扫描、备份、事务导入、行级幂等、回滚 | 已实现 |
| 大文本 | Java 编辑区 | CodeMirror 6 虚拟视口，1 MB 以上停用完整语法树 | 已实现 |

## 2. 布局与视觉

- [x] 页面只保留主标题，无小标题或面包屑。
- [x] 左侧 Vault、中央编辑器、右侧高级工具保留 Java 版操作心智。
- [x] 1440 宽屏三栏不溢出，工具栏无换行。
- [x] 1080 x 720 下高级工具改为可关闭覆盖层，功能不丢失。
- [x] 浅色、深色、JSONPath 弹窗和 Git 面板已建立截图基线。

截图位置：`doc/screenshots/json/`

## 3. 自动化证据

- `src/features/json/jsonTools.test.ts`：格式化、排序、重复 Key、XML、JSONPath、Key/Value 和 JavaBean。
- `src/shared/historyRepository.integration.test.ts`：SQLite 历史增删查、搜索、分类和 200 条上限。
- `src/shared/jsonVaultRepository.integration.test.ts`：Vault 嵌套路径、读写删除和路径穿越防护。
- `src/shared/vaultGitService.integration.test.ts`：Git 初始化、变更、提交、历史、Diff 和输入边界。
- `src/shared/legacyMigrationService.integration.test.ts`：Java 数据扫描、映射、幂等、路径边界和回滚。
- `tests/electron/app.spec.ts`：JSON 格式化、历史、Vault CRUD、高级文件操作和 Git 提交端到端闭环。
- `tests/electron/performance-p7.mjs`：打包 App 的 3 MB JSON、5 MB Quick Note、启动与内存门槛。
- `npm run typecheck`、114 项 Vitest、16 项 Electron E2E 和 `npm run build` 已通过。
- React Doctor：`100 / 100`。

## 4. 剩余人工出口条件

- [x] 补齐 Vault 文件夹、重命名、拖拽、排序、`.gitignore` 和 watcher。
- [x] 补齐 Git 冲突/撤销/合并中止与自动化。
- [x] 完成 Java 旧数据样本读取与迁移测试。
- [x] 验证打包 App 的大 JSON 性能与稳定内存。
- [ ] 对照 Java 版逐个签收 JSON/Vault/Git 快捷键。
- [ ] 补充 Java 版同尺寸对比截图并完成人工 parity sign-off。
