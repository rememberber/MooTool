# 对照

已对齐：截图捕获后可裁剪或保存全图，取消不写库；取色失败不改色值；六种界面风格有独立 token。

已知差异：

- macOS 截图用 `CGDisplayCreateImage`（主屏），不是 ScreenCaptureKit 多屏选区叠加层。
- 区域裁剪是捕获后的像素矩形，不是拖拽选区 overlay。
- Swift 未在本机用 Xcode 编译运行。
- 仍无第二 Flutter engine。
