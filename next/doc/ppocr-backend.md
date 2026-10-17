# PP-OCRv6 small 离线后端

Windows/Linux x64/arm64 默认选择 PP-OCRv6 small，复用图片助手的识别/截图入口、编辑预览、复制、保存和取消。macOS 继续使用 Vision，不在正式 macOS 包里分发 ONNX 运行时或模型。开发验收可在 macOS arm64 的测试环境强制运行 PP 后端；ONNX Runtime 1.30.0 没有 Intel Mac 二进制，不能据此承诺 Intel Mac 的 PP 后端。

## 识别流程

每次识别创建 Electron utility process。进程校验本地资源后，用 PNG 解码与 OpenCV.js 将透明像素合成到白底、转换 BGR；检测图按长边最多 1536、32 的倍数缩放，生成 CHW float32 张量。ONNX CPU 检测后进行 DB 阈值、膨胀、轮廓、矩形打分和 unclip；透视文字裁剪取原图像素，再按 48 px 高度归一化并进行 CTC 解码。按近似行顺序拼接文字，保留原图坐标的包围框和识别置信度。

使用固定 small 多语言模型覆盖简体、繁体、英文、日文，界面显示统一自动模式；选择简体并不能让该模型只输出简体，所以没有提供该选项。尚无方向分类器、表格结构恢复、拍照透视质量保证或竖排/多栏阅读顺序保证。极宽整屏图的 1536 检测上限可能损失小字，建议框选文字区域。

CPU 内部线程 4、跨算子线程 1；单次识别最长 120 秒。取消会杀死独立进程，并等待其退出后清理临时图片。模型在每次进程退出时释放，不常驻主进程。一次最多 200 个文字区域、2000 个检测轮廓；单行识别归一化宽度最多 4096 px，裁剪最多 1600 万像素。超出计算范围时显示错误，不静默截断结果。

## 固定资源和构建

模型固定 RapidOCR v3.10.0 发布的 `PP-OCRv6_det_small.onnx`/`PP-OCRv6_rec_small.onnx`，合计 **29.72 MiB**。下载校验 SHA-256，字典直接从已验证识别模型的 ONNX metadata 提取。运行时依赖独立固定在 `scripts/ocr-runtime/package.json`/`package-lock.json`：ONNX Runtime Node 1.30.0、OpenCV.js 4.12.0、pngjs 7.0.0、clipper-lib 6.4.2。

准备脚本使用 `npm ci --ignore-scripts`，不执行 ONNX 的 CUDA 自动安装脚本。只复制目标平台/架构的原生库和必要 JS，保留许可证和第三方声明。Windows 保留发布包提供的全部 DLL，不能在未验证 DLL 装载关系时仅按 CPU 模式删除 DirectML 相关文件。资源目录使用 `runtime/node_modules`，避免 Electron Builder 忽略 `extraResources` 根目录的 `node_modules`。

```sh
# 本机 Windows/Linux 开发：先准备资源，再运行 dev
npm run ocr:prepare
npm run dev

# 在支持的构建主机准备指定架构的资源
npm run ocr:prepare -- --platform win32 --arch x64
npm run ocr:prepare -- --platform linux --arch arm64

# 打包自动调用 beforePack，OCR 独立资源放入 dist/optional-components
npm run package:win
npm run package:linux

# macOS arm64 开发验收（正式 macOS 包仍用 Vision）
npm run ocr:prepare -- --platform darwin --arch arm64
MOOTOOL_OCR_BACKEND=pp-ocrv6-small npx playwright test tests/electron/image-ocr.spec.ts tests/electron/ppocr-lifecycle.spec.ts
```

构建时首次下载 npm 运行时和模型，封装为独立组件；Windows/Linux 安装包首次使用 OCR 时需确认下载，校验完成后重启生效。后续识别不下载，不启动 Python/HTTP 服务。缓存和准备产物位于 `out/ocr-cache`/`out/ocr`，不进入 Git，也不复制进 app.asar。`MOOTOOL_OCR_MODEL_SOURCE` 可显式指定本地模型目录，仍必须通过固定校验。进入图片助手后检查本地组件，资源缺失或校验失败时提示用户下载修复；运行中的资源变化也会被识别前的再次校验拒绝。只有点击下载按钮才联网，完整流程见 [按需组件说明](optional-components.md)。工作进程禁用 JS HTTP、fetch 和 socket 连接；ONNX Node 绑定未公开 Python/C API 的 telemetry 禁用接口，Windows 原生运行时与系统遥测行为仍需在目标机器断网验收，不能用 JS 拦截声称已禁用所有原生遥测。

资源 manifest 记录每个文件的 SHA-256/字节数和目标架构，工作进程每次加载前验证。正式 Windows/Linux 组件中的库不会由安装包重新签名；若自定义签名流程会改变 DLL 内容，须在签名后、最终发布前重新封存 manifest，仍保留模型的固定校验值。macOS 强制 PP 的临时诊断包包含重新签名的 dylib/Node binding，必须签完原生库后更新资源 manifest，再对外层 app 重新签名；正式 macOS 包没有这些 PP 文件。

资源体积为未压缩文件字节总量，包含模型、选定原生库、JS/WASM、字典及随资源的声明；不是安装包增量，也不包含 manifest、包内公共许可证副本及额外签名开销。

| 目标 | 离线资源体积 |
| --- | ---: |
| Windows x64 | 104.85 MiB |
| Windows arm64 | 110.25 MiB |
| Linux x64 | 84.96 MiB |
| Linux arm64 | 65.23 MiB |
| macOS arm64（仅开发诊断） | 83.66 MiB |

## 实测与验收边界

本机 macOS arm64、Electron 43.2.0 / Node 24.18.0 使用现有 8 张固定截图，通过产品 IPC、私有快照和真实独立进程完成两轮识别。首轮 **汉字 0/400 错误，全字符 2/684 错误（0.29%）**，与此前 Python small 参考结果一致；所有 16 次识别耗时中位数 **501.5 ms**，范围 475–713 ms。耗时包含校验、进程/运行时/模型加载、识别、清理和退出，不是常驻模型的纯推理耗时。原始输出见 [electron-mac-arm64.json](ocr-ppocr-evaluation/electron-mac-arm64.json)。该小型合成语料不能代表任意真实截图的准确率，也没有完成日语专项精度验收。

`npm run check` 通过 **82 个测试文件、404 项测试和构建**。PP 后端的 6 项桌面测试及临时 macOS arm64 诊断包内相同 6 项测试通过，覆盖真实识别、图片预览、编辑后复制保存、截图、空白图片、过期结果、独立窗口，以及真实进程取消/清理/重试。新增检测张量与 CTC 校验、平台资源完整性、进程超时/终止测试。临时诊断包严格签名验证通过，manifest 在原生库签名后封存，再重新签名外层 app；未发布该包。

2000×480 Retina 固定图的一次打包进程工作集采样峰值约 **309.64 MiB**，每 20 ms 取样，包含库、模型和中间数据；不是精确峰值或最大图片的上界。识别结束和取消后均确认没有残留 OCR utility process，私有快照已清理。[采样记录](ocr-ppocr-evaluation/memory-sample-mac-arm64.json)

另在 macOS `sandbox-exec` 禁止出站网络下，以独立 Node 传输适配器执行同一生产 worker 的真实资源校验与推理，固定中文图片识别 3 行成功。这是 macOS/Node 原生推理的断网探针，不能替代 Windows/Linux 的 Electron 实机断网验收。[原始探针结果](ocr-ppocr-evaluation/offline-mac-arm64.json)

Windows/Linux 的目标资源已经分别准备并检查；目录包在 macOS 构建主机交叉打包。**跨平台目录打包不能代替目标系统启动、截图权限、DLL/so 装载及断网识别验收。** 当前没有 Windows/Linux 实机结果；Electron 安装包工作流新增 `test-packaged-ocr.mjs`，会在目标 runner 对打包程序执行 OCR 桌面测试，Windows/Linux 还验证真实工作进程取消和清理，Linux 使用 Xvfb。arm64 同样没有目标系统实机结果，现有工作流矩阵只覆盖 Windows/Linux x64。

源码算法来源和运行时版权见 `resources/ocr/NOTICE` 与随附许可证；OpenCV.js/ONNX 使用本地资源，不涉及发送截图到外部 OCR 服务。
