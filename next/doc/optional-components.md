# 安装包体积与按需组件

Electron 版将只用于 renderer 的 React、CodeMirror、Prettier、二维码、前端密码算法等依赖放在 `devDependencies`。它们仍被 Vite 编译进页面，但不再作为完整 npm 包重复分发。主进程、MCP 和原生模块使用的依赖保留在 `dependencies`，保证打包后的独立运行。

2026-10-17 本机 macOS arm64 目录包对比：`app.asar` 约 144 → 65 MiB，应用目录约 421 → 342 MiB。这是未压缩目录体积，不是 DMG 下载体积。

## OCR 首次使用

macOS 继续使用系统 Vision。Windows/Linux 的安装包只携带 OCR 下载描述文件与许可证，首次点击「识别文字」或「截图识字」时弹出组件下载提示，显示大小。用户点击「下载组件」才联网；关闭弹窗后下载继续。下载或校验失败可重试。下载完成后选择立即重启或稍后重启；重启前 OCR 保持不可用，其他工具可以继续使用。

组件存放在 `<userData>/components/ocr/<内容 SHA-256>/<平台-架构>`，无需修改安装目录或管理员权限。同一内容版本在应用升级后继续复用；更换组件内容时使用新的缓存目录。安装状态与完整性在进入图片助手后检查，不在应用启动时扫描模型。异常中断的下载临时目录在下次检查时清理，损坏的安装可重新下载。

Windows x64 拆出约 104.85 MiB、Linux x64 约 84.96 MiB 的未压缩资源。当前独立 gzip 下载包分别约 56.32 MiB、45.93 MiB；具体以打包时生成的描述文件为准。

下载地址、压缩包 SHA-512、大小及各文件 SHA-256 固定在应用自带的描述文件中。组件是按清单顺序拼接文件的 gzip 流；下载内容不提供文件名，不能增加条目或指定解压路径。下载流和解压流均限制大小，校验成功并验证 OCR 模型/运行时清单后才原子安装并写入完成标志。重启时再次验证资源。图片不参与组件下载，也不会上传；识别阶段继续离线运行。

## 打包与发布

`beforePack` 在 Windows/Linux 目标上准备资源，然后调用 `scripts/package-ocr-component.mjs`：

- `out/optional-components/<平台-架构>.json`：随对应安装包分发的固定下载描述。
- `dist/optional-components/MooTool-OCR-<内容 SHA-256>-<平台-架构>.bin.gz`：独立下载资源。
- `dist/optional-components/<平台-架构>.json`：发布验证用的描述副本。

资源地址指向安装包版本对应的 `next-electron-v<版本>` GitHub Release。手动发布时，必须同时上传目标平台的 `.bin.gz` 和 `.json`。现有安装包工作流自动暂存对应资源，并在发布前检查 Windows/Linux x64 资源的大小和校验值；旧缓存中的其他版本不会被误上传。仅构建本地包而不发布对应资源时，首次下载会失败并允许重试。

`out/ocr` 与 `out/ocr-cache` 仍服务于源码开发和组件构建，不进入安装包。源码开发继续使用 `npm run ocr:prepare`。

## 验证

`optionalOcrComponent.test.ts` 覆盖按需下载、并发合并、校验失败、截断/超量数据、非法路径、原子安装、异常临时文件清理和重启复用。`ocr-component.spec.ts` 覆盖提示、重试和重启入口；Windows/Linux 打包测试还使用真实发布组件完成首次下载、重启和断网识别。原有识别与进程生命周期测试预置已安装组件，继续测试真实后端。

本机可验证 macOS 打包程序和 Windows/Linux 资源字节完整性；Windows/Linux 原生库加载及桌面流程仍需相应系统的 CI runner 验证。
