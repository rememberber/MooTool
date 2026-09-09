# DIFF-013：图片转 SVG 使用 ImageTracer.java

- 编号：DIFF-013
- 影响：F23 图片助手
- 日期：2026-09-09

## 原行为（Electron）

`@visioncortex/vtracer` `convertBuffer`。预设 poster/photo/bw 映射到 clustering/hierarchical/simplify。输出必须含 `<svg xmlns>` 与 `<path>`，禁止 `<image>` 或 `data:image/`。

## 本产品行为

将 Unlicense 的 **ImageTracer.java 1.1.2** 复制进 `composeApp/src/desktopMain/java/jankovicsandras/imagetracer/`。`ImageSvg` 把同一组 UI 参数映射到 `ltres/qtres/pathomit/colorquantcycles/blurradius`，并用 median-cut 调色板（bw 为黑白）。压缩/水印/命名/页面对齐 Electron `imageTools.ts`。截图使用与调色板相同的 Robot 冻结虚拟桌面，再拖选区域。

## 理由

架构要求本产品自带 tracer，且不得依赖 Electron 的 npm 包。ImageTracer.java 为公有领域，可独立随包。Java2D 完成压缩与水印，避免浏览器 Canvas。

## 证据

`ImageEngineTest`：缩放/命名/Base64/锚点与 Electron fixture 一致；JPEG 压缩改变尺寸；水印改像素；poster/bw 输出含 path 且无嵌入位图；图片库导入/重命名/删除。`desktopTest` **90/90**。

## 受影响范围

- 同一张图的 SVG path 不会与 vtracer 视觉一致，参数语义对齐但算法不同。
- WebP 无额外 ImageIO 插件时导入失败，会报无法读取。
- 区域截图是拖选确认，没有 Electron 的四角缩放手柄。
- 未跑安装镜像、多屏截图权限对话框与超 16MP 真图。
