# 图片与截图文字识别

Electron 版图片助手提供两个入口：选择当前图片后点「识别文字」，或点「截图识字」框选屏幕区域。预览窗口显示图片快照和可编辑的识别结果；选择语言后点「开始识别」，校对后再复制或保存为 UTF-8 `.txt`。截图识字只保留当前窗口内的快照，不新增图片库条目。

**macOS 13 及以上默认使用 Apple Vision**，不随应用分发 OCR 模型。可选语言根据当前系统实际能力显示，包含简体、繁体、英文和日文；「自动（中英）」尝试简体、繁体和英文。

**Windows/Linux x64/arm64 已接入 PP-OCRv6 small 的完整 JavaScript CPU 流程**，模型和推理运行时改为首次使用时经用户确认下载，校验完成后重启生效，无需 Python。使用同一个多语言模型识别简体、繁体、英文和日文，不提供实际无效的单语言切换。组件缺失或损坏时保留入口并提示下载；不支持的架构会禁用入口并提示。Windows/Linux 实机验收状态见 [PP-OCR 后端说明](ppocr-backend.md)。

窗口展示识别引擎、语言模式、行数和耗时。数字、标点及复杂排版仍需人工校对，结果是文字行拼接，不提供表格还原或手写识别保证。

## 处理边界

- 使用打开预览时的图片快照，支持图片库现有导入格式；主进程收到的快照统一为 PNG。原图不被改写。
- 图片单边最多 16384 px、总像素最多 3200 万，归一化 PNG 最多 32 MiB；结果最多 2000 行和 100000 个 UTF-16 字符。PP-OCR 另有 200 个文字区域及单行归一化宽度 4096 px 的计算上限，超限要求缩小范围。
- 一次运行一个识别任务，原生识别进程最长运行 120 秒。系统首次初始化推理缓存可能较慢，本机新编译程序曾耗时约 70 秒，缓存就绪后的识别通常远快于首次启动。取消、关闭预览或销毁所属工具视图会停止识别；晚到的结果不能覆盖后续预览。
- 识别阶段不会修改剪贴板或自动保存结果。文本只保留在当前预览；复制和保存使用用户编辑后的内容。无文字时显示提示并禁用复制/保存。
- 主进程把快照写入系统临时目录的私有任务文件夹，图片权限为 `0600`，交给无 shell 的 Vision helper 或独立 PP-OCR utility process。结束、失败和取消均删除该任务文件夹；PP-OCR 每次结束/取消都等待进程退出，释放模型内存。应用/系统异常中断可能留下系统临时文件，当前没有跨启动清扫机制。
- 应用识别路径不发起网络请求。系统 Vision 的模型由 macOS 管理；截图仍使用现有系统屏幕捕捉权限和框选流程。

## 构建与分发

`npm run dev` 和 `npm run build` 会调用 `scripts/build-native-ocr.mjs`。macOS 开发环境需安装 Xcode Command Line Tools，脚本编译 macOS 13 部署目标的 arm64/x86_64 通用可执行文件，并在源码或脚本更新后重新构建。其他系统跳过原生编译。

构建结果位于 `out/native/mootool-ocr`；macOS 安装包通过 `extraResources` 将 helper 放到 `Contents/Resources/native/mootool-ocr`，主进程从该位置加载。`mac.binaries` 将其纳入 Electron Builder 的应用签名流程。Windows/Linux 的打包 hook 生成按架构筛选的独立组件下载包，安装包只包含固定下载描述文件。用户首次使用时在应用内确认下载并重启，之后离线使用；无需 Swift 编译器、Python 或手动配置模型。下载进度、校验、重试和发布约定见 [按需组件说明](optional-components.md)。详见 [PP-OCR 构建命令和体积](ppocr-backend.md)。

## 验证

`electron/main/ocrService.test.ts` 验证请求/输出边界、私有快照、完成/失败/取消清理、全局并发和请求归属；`ocrBackend.test.ts` 验证子进程协议、超时/终止和平台可用性。

`tests/electron/image-ocr.spec.ts` 调用当前系统的真实后端，覆盖识别预览、编辑后复制/保存、原图保留、空白图片、取消/关闭后的过期结果、截图框选和窄屏独立工具窗口。截图测试只替换屏幕源为固定图片，仍走应用的框选与识别路径；过期结果测试替换 IPC 返回时序，其余识别使用真实后端。`ppocr-lifecycle.spec.ts` 另验证真实 PP-OCR 子进程终止、临时文件清理、再次识别和结束后的进程释放，并记录工作集采样。

运行 `npm run check` 和 `npx playwright test tests/electron/image-ocr.spec.ts`。打包后可用 `MOOTOOL_OCR_EXECUTABLE="绝对路径/MooTool Next Electron.app/Contents/MacOS/MooTool Next Electron" npx playwright test tests/electron/image-ocr.spec.ts` 验证资源定位和安装包内的真实识别。当前验收机器为 macOS arm64；Intel slice 可编译，不等同于 Intel 实机验收。

2026-10-12 首批 Vision 验收：394 项单元测试、12 项桌面回归及临时 macOS arm64 包内 5 项 OCR 测试通过；通用 helper、应用/helper 严格签名验证通过。后续 PP-OCR 验证、真实 JavaScript 精度和资源体积记录见 [后端说明](ppocr-backend.md)。临时包均未发布或公证。

另用 `sandbox-exec` 禁止出站网络执行真实 helper，对固定中文图片完成 3 行识别。浅色与深色、620 px 宽的独立窗口均做了截图检查。
