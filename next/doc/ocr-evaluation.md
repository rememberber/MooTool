# Electron 中文 OCR 方案评估

评估日期：2026-10-12。结论：**macOS 优先考虑系统 Vision；需要统一跨平台中文模型时优先 PP-OCRv6 small。tiny 可用于简体轻量模式，但不宜默认承担繁体识别。Tesseract.js 接入最简单，适合作为可选兼容后端，需明确选择简体或繁体。**

评估阶段未增加产品 OCR 功能、生产依赖或模型文件。模型及实验运行时均安装在临时目录。后续首批开发见 [图片与截图文字识别](image-ocr.md)。

## 本机实测

测试环境：macOS 27.0.1、arm64，系统报告 Apple M6；Electron 43.2.0 / 内置 Node 24.18.0。参考 OCR 使用 Python 3.12.14、RapidOCR 3.10.0、ONNX Runtime 1.31.0 CPU；Tesseract.js 7.0.0 使用 Node 24.21.0。Apple Vision 使用 accurate 模式、关闭语言纠正，语言为 zh-Hans、zh-Hant、en-US。

语料为 [8 张固定合成截图](ocr-evaluation/samples.json)：浅色 20px、深色 16px、小字 12px、12px 的 2x Retina、繁体 20px、中英技术文本 18px、低对比模糊文本、宋体 22px。共 **400 个汉字、684 个非空白字符**。文字均为本项目工具场景的自编示例，没有使用用户截图。

汉字 CER 为汉字序列的编辑距离除以参考汉字数；全字符 CER 只去除空白，仍区分简繁、大小写、标点和货币符号。数值越低越好。识别输出来自首轮；耗时是后续三轮、每轮八图的中位数。各引擎依次运行，避免相互争用 CPU。

| 方案 / 配置 | 汉字 CER | 全字符 CER | 热识别中位耗时 | 检测+识别模型 |
| --- | ---: | ---: | ---: | --- |
| PP-OCRv6 small，截图配置 | **0 / 400，0.00%** | 0.29% | 132 ms | 29.72 MiB |
| PP-OCRv6 tiny，截图配置 | 7 / 400，1.75% | 1.61% | **40 ms** | 6.03 MiB |
| Apple Vision | 2 / 400，0.50% | 0.73% | 65 ms | 不随应用分发模型 |
| Tesseract，简体+繁体+英文 | 24 / 400，6.00% | 7.02% | 318 ms | gzip 6.03 MiB，解压 9.57 MiB |
| Tesseract，简体+英文 | 35 / 400，8.75% | 6.43% | 113 ms | gzip 4.46 MiB，解压 7.31 MiB |

**0 / 400 不代表真实截图能达到 100% 准确率。** 样本量小、均为横排印刷文本；未覆盖真实拍照、手写、竖排、透视、多栏、表格阅读顺序、罕见字及高压缩截图。表中的 RapidOCR 延迟来自参考 Python 流程，不是已完成的 Electron OCR 性能。

Vision 原始输出在开发验证时用同一语料复测，汉字和全字符 CER 未变；表中及 `vision.json`/`summary.json` 使用最后一次复测的热耗时。新编译的程序首次识别曾触发约 70 秒的系统推理缓存编译，已有缓存的进程首次识别约 0.2 秒，不能用热耗时估计首次使用等待时间。产品需要保留取消并为系统初始化留出更长超时。

### 简繁差异与错误示例

- small 在这八张图里没有汉字错误，但仍有两处非汉字差异，说明数字/标点等也需要预览校对。
- tiny 的七个汉字错误全部来自繁体图，繁体图 CER 为 **13.46%**。例如「檔案名稱」中的「稱」被识别为「释」，部分繁体被改成简体。其余七张图的汉字 CER 为 0%。tiny 因而适合简体轻量路线，不能由总平均值掩盖繁体问题。
- Vision 漏掉「視窗設定」里的「窗」，将「邮箱」识别为「邮租」；技术文本中的 `v1` 也被读成 `V1`。
- Tesseract 三语同时启用时，部分简体被读成繁体，例如「识别」「图片压缩」「格式转换」。七张简体/混合图的汉字 CER 为 **6.90%**；繁体图为 0%。
- Tesseract 只启用简体+英文后，七张简体/混合图的汉字 CER 降到 **1.72%**，但繁体图升到 **55.77%**。产品若采用它，需要明确的简体/繁体选项，不能用「同时加载更多语言」代替语言选择。
- 12px 小字：Tesseract 简体模式为 5.66%，相同文字保留 2x 像素后降为 0%；其他方案在这两张图里没有汉字错误。截图应保留原始像素，不先缩成 CSS 尺寸。

原始输出、逐图 CER 与三轮耗时见 [summary.json](ocr-evaluation/summary.json)。

### 截图配置和内存

RapidOCR 原配置按短边 736 缩放，窄截图会被大幅放大。评估了长边最多 1536 的截图配置，同样使用四个 CPU 内部线程、一个跨算子线程，不使用 GPU和方向分类器。

| 方案 | 默认短边 736 | 截图长边最多 1536 | 截图配置 Python 进程峰值 RSS |
| --- | ---: | ---: | ---: |
| tiny | 150 ms | 40 ms | 365 MiB |
| small | 331 ms | 132 ms | 718 MiB |

两种配置的汉字结果相同。峰值 RSS 包含 Python、NumPy、OpenCV、模型和中间张量，不能当作 Electron 内存预算；Node 完整流程仍需独立验证。对于宽图/整屏图，1536 的上限可能损失小字，需要追加测试，不能直接把它视为通用最优参数。文字识别裁剪应取原图，检测图可以独立缩放。

## 离线体积

模型大小按下载后文件的实际字节数统计，单位 MiB（1024²），不是论文参数量或安装包压缩大小。PP-OCR 的四个文件已验证 RapidAI 发布的 SHA-256，固定在 v3.10.0。[模型地址、校验值与运行时文件清单](ocr-evaluation/sizes.json)可复查。

| 模型组合 | 实测文件体积 |
| --- | ---: |
| v6 tiny 检测 | 1.74 MiB |
| v6 tiny 识别 | 4.28 MiB |
| v6 small 检测 | 9.47 MiB |
| v6 small 识别 | 20.25 MiB |
| Tesseract 简体 traineddata | gzip 1.64 MiB / 解压 2.36 MiB |
| Tesseract 繁体 traineddata | gzip 1.58 MiB / 解压 2.26 MiB |
| Tesseract 英文 traineddata | gzip 2.82 MiB / 解压 4.96 MiB |

运行时必须另算：

- **ONNX Node，Mac arm64**：当前 npm 平台子目录 85.30 MiB，包含两个内容相同的 dylib。保留绑定和它实际链接的 `libonnxruntime.1.dylib` 后是 **42.78 MiB**；删除重复文件后，Electron 原生加载和一次识别模型推理已通过。tiny 加此运行时约 **48.81 MiB**，small 约 **72.50 MiB**，尚未计入前后处理、公共 JS 依赖及签名开销。这不是安装包增量测量。
- **Tesseract.js**：完整 core 包为 43.17 MiB，API 包 1.35 MiB。Node 路径保留 `.js` + `.wasm`、移除浏览器内联 core 后，core 为 **18.58 MiB**；在本机通过了一张图的裁剪包验证。三语 gzip 模型 + 此 core/API 约 **25.96 MiB**，首次缓存解压数据另需约 9.57 MiB。裁剪包尚未做各平台安装包验收。
- **Vision**：应用无需额外分发 OCR 模型，依赖系统框架及系统提供的语言能力。「应用新增模型为零」不等于系统模型自身为零。
- 本轮 Python 环境仅用于评估，不建议把 Python/NumPy/OpenCV 参考环境直接捆进 Electron；其体积和依赖都不能仅按模型文件估算。

PP-OCR small 使用同一模型覆盖简体、繁体、英文、日文等，tiny 不包含日语；这是官方模型范围，并不是本轮逐语种验收结论。[PaddleOCR 官方说明](https://github.com/PaddlePaddle/PaddleOCR/blob/main/docs/version3.x/algorithm/PP-OCRv6/PP-OCRv6.en.md)

本机在 `sandbox-exec` 禁止进程网络出口时，small 和 Tesseract 各完成了一张图的识别。模型与语言文件全部指定本地路径；默认自动下载行为必须在产品接入时显式关闭。Tesseract 未配置 `langPath` 会下载语言文件。[Tesseract 本地部署文档](https://github.com/naptha/tesseract.js/blob/master/docs/local-installation.md)

## 跨平台与接入成本

| 方案 | Windows | macOS arm64 | macOS Intel | Linux | 接入成本 |
| --- | --- | --- | --- | --- | --- |
| PP-OCR + ONNX Node 1.30 | 有 x64/arm64 二进制，待实机验收 | 原生加载/单模型推理已通过 | **npm 包没有 darwin/x64** | 有 x64/arm64 二进制，待实机验收 | 需移植检测前后处理、裁剪、识别解码、阅读顺序，并处理原生包签名/asar |
| Tesseract.js | JS/WASM 路线，待实机验收 | 参考识别已通过 | 不依赖本平台原生 OCR 二进制，待实机验收 | JS/WASM 路线，待实机验收 | 最低；仍需处理 worker 路径、本地模型与 CPU 占用 |
| Apple Vision | 不适用 | Swift 原型已通过 | 系统 API 路线可行，helper/语言需验收 | 不适用 | macOS helper、签名和统一接口 |
| Windows.Media.Ocr | 需要 package identity/MSIX 和可用语言 | 不适用 | 不适用 | 不适用 | 当前 NSIS/portable 包不能直接沿用该部署前提 |

ONNX 官方支持表仍列 Mac x64，但本轮检查 **1.30.0 的实际 npm 文件**没有该目录，上游 issue 也记录了这一缺口。因此不能只看支持表就承诺 Intel Mac 可用。可考虑 Vision、自编译原生 runtime 或单独验证 WASM 路线，不应直接假定解决。[官方 Node 支持表](https://github.com/microsoft/onnxruntime/blob/main/js/node/README.md)、[Intel Mac 缺少二进制的上游 issue](https://github.com/microsoft/onnxruntime/issues/27961)

Electron 探针只加载 ONNX 原生绑定并运行一张空白张量，结果见 [electron-probe.json](ocr-evaluation/electron-probe.json)。这没有验证完整 JavaScript OCR pipeline。RapidOCR 的 Python 项目可作为算法参考；RapidOCRWeb 是 Python HTTP 服务，不是可直接引入的纯浏览器 OCR SDK。[RapidOCR](https://github.com/RapidAI/RapidOCR)、[RapidOCRWeb](https://github.com/RapidAI/RapidOCRWeb)

Windows 官方说明该 OCR API 对桌面应用要求 package identity。目前 Electron 构建是 NSIS/portable，不是 MSIX，系统原生 OCR 不宜作为首批 Windows 默认实现。[Windows.Media.Ocr 官方部署要求](https://learn.microsoft.com/en-us/uwp/api/windows.media.ocr?view=winrt-26100)

## 建议的实施边界

1. 建立一个 OCR 后端接口，先接已有截图选区与图片库，返回文字、行位置、引擎名称和错误；显示可编辑预览，再由用户复制。
2. macOS 可先用 Vision，避免新增模型下载和运行时体积。后续明确验证 Intel Mac helper、系统版本和中英/简繁支持。
3. Windows/Linux 中文方案优先 small，tiny 作为简体轻量选项。先完成一张真实截图的 Node 前后处理原型，再决定是否统一启用 PP-OCR；不要把 Python 结果直接视作 Electron 接入完成。
4. 固定运行时和模型版本，按目标架构收集二进制、保留许可证及校验值；本地资源加载、工作进程隔离、闲置释放模型和不默认下载均作为接入要求。使用 ONNX 时显式关闭遥测，追加进程网络出口检查。
5. 接入前扩充真实语料：不同 DPI 的 Windows/Linux 截图、多栏、表格、竖排、拍照和罕见字；分别统计汉字、数字、标点及阅读顺序。Windows/Linux/Intel Mac 的包启动和断网识别是独立验收项。

建议当前决定采用 **Vision + 可插拔 PP-OCRv6 small** 的方向，保留 tiny 的简体轻量模式；暂不把 Tesseract 三语自动模式设为默认。若目标是三平台输出完全一致，则选择统一 small 后端，但应先解决 Intel Mac runtime 路线。

## 复现与产物

固定 PNG、参考文字、所有原始输出、统计与体积清单在 [ocr-evaluation/](ocr-evaluation/)，脚本及依赖版本在 [评估脚本说明](../scripts/ocr-evaluation/README.md)。重复实验应使用固定 PNG，重新生成字体截图会改变语料。

评估阶段没有修改产品行为或生产依赖；评估产物后续随首批 Vision 实现提交。PP-OCR 的完整 Electron 流程、最新体积和验收状态见 [后端说明](ppocr-backend.md)。
