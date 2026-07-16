# P4 文件、媒体与密码学工具 Parity Checklist

> 阶段：P4
> 状态：实现完成，待 Java 对照验收
> Java 基线：当前 `github-develop-ts` 分支 Java 版
> 最后更新：2026-07-16

## 1. 工具清单

| 工具 | Java 布局/能力基线 | Next 实现 | 状态 |
| --- | --- | --- | --- |
| 格式化 | 文本/文件 Tab，Nginx、Java、XML、HTML，缩进 2-6，历史与快捷键 | Prettier 插件与 Nginx 格式器、源文件导入、导出、历史、`Cmd/Ctrl + Shift + F` | 待对齐验收 |
| 加解密/随机 | 对称、非对称、摘要、Base64/Base32、随机五个 Tab | AES/DES/SM4，RSA/SM2，MD5/SHA/SM3，文件摘要，Base64/Base32，四类随机值与历史 | 待对齐验收 |
| 二维码 | 生成、识别、历史，尺寸、纠错级别、Logo、文件/剪贴板 | PNG 生成与保存、Logo、ZXing 普通/纯码识别、剪贴板和历史 | 待对齐验收 |
| 调色板 | 主题色、标准色、取色、格式转换、颜色运算、收藏、历史 | Chromium 屏幕取色、HTML/html/RGB、主/对比色、六类运算、SQLite 收藏与历史 | 待对齐验收 |
| 图片助手 | 本地图片库、截图、剪贴板、导入导出、缩放、压缩、水印、Base64 | 固定数据目录图片库、截图与裁剪、批量压缩/水印、复制、重命名、导出和 Base64 | 待对齐验收 |
| PDF | 拆分/合并 Tab，最多 20 个任务，页码范围和拆分规则 | `pdf-lib` 检查、页码选择、奇偶/自定义拆分、按范围合并和输出队列 | 待对齐验收 |
| Protobuf | JSON/Binary、Wire、Hex/Base64 三个 Tab，Proto 格式化与历史 | `protobufjs` 定义解析、JSON 双向转换、Wire 字段解析、Hex/Base64 与历史 | 待对齐验收 |

## 2. 共享基础设施

- 七个页面全部由 `toolRegistry` 懒加载，并使用统一 Tool Page、Tabs、Toolbar、Dialog、History、Favorite、Tooltip 和 Toast。
- Renderer 只调用 typed preload API；文件摘要、原生文件对话框、系统剪贴板、截图、图片仓库和 PDF 服务均位于 Electron 主进程。
- 图片仓库固定在设置的数据目录下，文件名、扩展名、数量和 data URL 均由主进程校验。
- PDF 只能处理用户通过原生对话框授权的路径；页码支持分号、中英文逗号和范围表达式。
- QR 尺寸、纠错级别与随机字符串长度接入统一设置；历史和颜色收藏使用 SQLite。
- 所有新增页面提供简体中文、英文、日文文案，并支持系统、浅色和深色主题。

## 3. 布局与等价说明

- [x] 所有页面只保留主标题，无小标题和面包屑。
- [x] 格式化保留文本/文件双 Tab；Crypto 保留五 Tab；QR、PDF、Protobuf 保留 Java 版 Tab 顺序。
- [x] 图片助手保留“工具栏 + 图片列表 + 画布 + 缩放栏”的桌面布局。
- [x] PDF 保留任务表、页码范围、规则、状态和最近输出区域。
- [x] 调色板保留当前色/对比色、颜色运算、主题色、标准色和收藏/历史入口。
- [x] 1440 x 920 浅色与 1080 x 720 深色共 14 张页面截图已建立。
- [x] 两组视口下七页均通过页面边界、标题截断、标题/工作区相交和窗口级横向溢出检查。

截图位置：`doc/screenshots/p4-<tool>-<viewport-theme>.png`

## 4. 自动化证据

- Reformat、Crypto、Color、QR、Protobuf、Image 和页码解析均有纯逻辑单元测试。
- `src/shared/pdfService.integration.test.ts` 使用真实 PDF 文件覆盖检查、选页合并、奇数页拆分、输出命名和错误边界。
- `tests/electron/app.spec.ts` 覆盖格式化、AES 往返、Protobuf 往返、QR 生成后经系统剪贴板识别、颜色收藏、图片持久化/复制和 PDF 工作区。
- macOS QR 剪贴板回读黑图问题已通过主进程高质量图像表示修复，并由 Electron E2E 回归。
- 全量 Vitest：23 个测试文件、72 项测试通过。
- Electron Playwright：8 项 E2E 通过。
- TypeScript 类型检查、Electron/Vite 生产构建和 `git diff --check` 通过。
- React Doctor：`100 / 100`，无诊断问题。

## 5. 已批准差异

| 差异 | 原因 | 状态 |
| --- | --- | --- |
| 图片助手不提供 OCR | 用户明确决定本轮移除 OCR，不建设 Provider、模型和下载链路 | 不阻塞 |
| Protobuf 使用 `protobufjs`，不下载 `protoc` | 避免运行时下载和跨平台二进制管理，保留相同的定义解析与转换工作流 | 不阻塞 |
| macOS 复制图片使用高质量 JPEG 剪贴板表示 | Electron/macOS 会把部分 PNG 剪贴板表示回读为黑图；该分支保证 QR 与普通图片可用 | 不阻塞 |

## 6. 共享验收跟踪

- [ ] 使用 Java 版同尺寸截图完成七个工具的逐页人工 parity sign-off。
- [ ] 在 Windows 和 Linux 安装包中复测截图、屏幕取色、剪贴板和原生文件对话框。
- [ ] 用旧版真实历史/收藏样本验证数据迁移；数据迁移统一放在 P7，不在单页重复实现。
- [ ] 对大图片和大 PDF 补充性能、取消与压力测试；不影响 P4 功能实现阶段收口。

以上项目属于发布前共享 parity 与跨平台验收，不影响 P4 实现范围已完成的结论。
