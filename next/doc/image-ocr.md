# 图片与截图文字识别

Electron 版图片助手提供两个入口：选择当前图片后点「识别文字」，或点「截图识字」框选屏幕区域。预览窗口显示图片快照和可编辑的识别结果；选择语言后点「开始识别」，校对后再复制或保存为 UTF-8 `.txt`。截图识字只保留当前窗口内的快照，不新增图片库条目。

本轮实现 **macOS 13 及以上的 Apple Vision 后端**，不随应用分发 OCR 模型，不增加 Python 或第三方 OCR 运行时。可选语言根据当前系统实际能力显示，包含简体、繁体、英文和日文；「自动（中英）」尝试简体、繁体和英文。窗口展示本次识别的引擎、语言、行数和耗时。数字、标点及复杂排版仍需人工校对，结果是按 Vision 返回的文字行拼接，不提供表格还原或手写识别保证。

Windows/Linux 当前显示不可用提示并禁用入口。`OcrBackend` 和共享请求/结果协议已经独立，后续可接入 [评估推荐的 PP-OCRv6 small](ocr-evaluation.md)。本轮尚未实现该模型的 JavaScript 检测、裁剪及识别流程。

## 处理边界

- 使用打开预览时的图片快照，支持图片库现有导入格式；主进程收到的快照统一为 PNG。原图不被改写。
- 图片单边最多 16384 px、总像素最多 3200 万，归一化 PNG 最多 32 MiB；结果最多 2000 行和 100000 个 UTF-16 字符。
- 一次运行一个识别任务，原生识别进程最长运行 120 秒。系统首次初始化推理缓存可能较慢，本机新编译程序曾耗时约 70 秒，缓存就绪后的识别通常远快于首次启动。取消、关闭预览或销毁所属工具视图会停止识别；晚到的结果不能覆盖后续预览。
- 识别阶段不会修改剪贴板或自动保存结果。文本只保留在当前预览；复制和保存使用用户编辑后的内容。无文字时显示提示并禁用复制/保存。
- 主进程把快照写入系统临时目录的私有任务文件夹，图片权限为 `0600`，通过无 shell 的子进程传给系统 Vision。结束、失败和取消均删除该任务文件夹。应用/系统异常中断可能留下系统临时文件，当前没有跨启动清扫机制。
- 应用识别路径不发起网络请求。系统 Vision 的模型由 macOS 管理；截图仍使用现有系统屏幕捕捉权限和框选流程。

## 构建与分发

`npm run dev` 和 `npm run build` 会调用 `scripts/build-native-ocr.mjs`。macOS 开发环境需安装 Xcode Command Line Tools，脚本编译 macOS 13 部署目标的 arm64/x86_64 通用可执行文件，并在源码或脚本更新后重新构建。其他系统跳过原生编译。

构建结果位于 `out/native/mootool-ocr`；安装包通过 `extraResources` 将 helper 放到 `Contents/Resources/native/mootool-ocr`，主进程从该位置加载。`mac.binaries` 将其纳入 Electron Builder 的应用签名流程。用户运行安装包不需要 Swift 编译器，也无需安装 Python 或自行下载模型。

## 验证

`electron/main/ocrService.test.ts` 验证请求/输出边界、私有快照、完成/失败/取消清理、全局并发和请求归属；`ocrBackend.test.ts` 验证子进程协议、超时/终止和平台可用性。

`tests/electron/image-ocr.spec.ts` 在 macOS 使用评估语料中的固定中文图片调用真实 Vision，覆盖识别预览、编辑后复制/保存、原图保留、空白图片、取消/关闭后的过期结果、截图框选和窄屏独立工具窗口。截图测试只替换屏幕源为固定图片，仍走应用的框选与识别路径；过期结果测试替换 IPC 返回时序，其余识别使用真实后端。

运行 `npm run check` 和 `npx playwright test tests/electron/image-ocr.spec.ts`。打包后可用 `MOOTOOL_OCR_EXECUTABLE="绝对路径/MooTool Next Electron.app/Contents/MacOS/MooTool Next Electron" npx playwright test tests/electron/image-ocr.spec.ts` 验证资源定位和安装包内的真实识别。当前验收机器为 macOS arm64；Intel slice 可编译，不等同于 Intel 实机验收。

2026-10-12 本机验证结果：`npm run check` 通过 79 个测试文件、394 项测试和构建；OCR、图片缩放、SVG/压缩桌面回归共 12 项通过；临时 macOS arm64 应用包内 5 项 OCR 桌面测试通过。使用本地 ad-hoc 签名完成打包，应用与 helper 均通过 `codesign --verify --deep --strict`/单独严格签名验证，helper 保留 arm64/x86_64 两个 slice，签名后约 269 KiB。未发布或公证该临时包。

另用 `sandbox-exec` 禁止出站网络执行真实 helper，对固定中文图片完成 3 行识别。浅色与深色、620 px 宽的独立窗口均做了截图检查。
