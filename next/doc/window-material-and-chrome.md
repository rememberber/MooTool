# 窗口材质与顶部布局

## 结构

- `electron/main/windowChrome.ts`：共用系统窗口配置。macOS 使用 hiddenInset、原生红绿灯与透明原生背景；其他平台保留默认系统窗口框架。跟踪全屏状态，包括工具视图换宿主时的初始状态。
- `electron/main/windowMaterial.ts`：统一材质能力判断、扩展加载和失败回退，支持 BrowserWindow 和 BaseWindow。子视图通过回调同步实际材质与原生背景，避免把玻璃覆盖成实色。
- `src/shared/contracts/windowMaterial.ts`：实际材质、降级原因、当前窗口启动时的选择以及是否待重启。
- `src/shared/styles/sidebar.css`：唯一的菜单状态规则，主题只定义颜色、圆角、字重变量。选中项 hover 保持不变，键盘焦点独立于选中状态。
- `src/shared/styles/windowMaterial.css`：主侧栏原生材质的透明背景链和浅色 0.4 / 深色 0.5 色层。
- `src/shared/styles/windowChrome.css`：沉浸式工具栏的原生红绿灯横向安全区和材质状态提示。工具页面从窗口顶部开始，不增加统一标题栏；全屏时释放安全区。

## 生命周期

窗口材质选择在下一次启动生效。原生扩展没有公开的移除 API，不因设置变化重建工具 renderer。独立窗口延用本次主窗口的材质选择，保证同一会话一致。

拆出工具时保留同一个 WebContentsView；创建 BaseWindow、连接全屏事件、应用材质后显示。关闭独立窗口仍收回工具。收回或宿主意外销毁时重置子视图材质与全屏状态；原生窗口销毁不会销毁要复用的工具 webContents。应用退出时由管理器关闭这些 webContents。

减少透明度、高对比度与强制颜色偏好优先于材质选择。已挂载玻璃时，通过实色原生背景与网页背景遮蔽，不叠加另一层 vibrancy。扩展加载错误、挂载返回 -1 或原生材质失败都产生可解释的降级状态。

## 验证

```sh
npm run check
npx playwright test tests/electron/tool-windows.spec.ts tests/electron/window-material.spec.ts
```

覆盖工具拆出/收回状态保持、顶部安全区、可点击操作区、四种材质、辅助功能切换、独立窗口全屏恢复、实际材质与待重启提示。移除了原红绿灯轮询与随显隐变化的工具专用偏移。

测试支持直接启动打包后的二进制，使用独立临时数据目录：

```sh
MOOTOOL_E2E_EXECUTABLE='/path/to/MooTool Next Electron.app/Contents/MacOS/MooTool Next Electron' \
  npx playwright test tests/electron/tool-windows.spec.ts tests/electron/window-material.spec.ts
```

此前统一标题栏版本已验证本地未签名打包产物；恢复沉浸式布局后需重新打包验证。原生扩展的 arm64 二进制位于 app.asar.unpacked。不同平台与旧 macOS 的策略回退有单元测试，不能代替对应系统实机验证。发布签名、公证、跨显示器缩放和不同壁纸下的原生观感仍应纳入正式发布的人工检查。


恢复沉浸式布局后的验证：333 项单元测试、生产构建和 12 项 Electron 回归通过。布局断言确认 JSON 与随手记工具顶部为 0，无额外标题栏，原有边缘按钮行为保留。

## 浮动控件玻璃

`glassControls.css` 统一管理边缘拆出/收回按钮、随手记视图切换组和留言板沉浸展示退出按钮的 CSS 玻璃。分段按钮只有一层整体玻璃，选中状态保留实底色层。使用背景 alpha、模糊和内侧高光；文字与图标保持不透明。工具内容仍贴顶。

这些控件是网页 CSS 效果，与原生 Liquid Glass 的窗口材质不同，也不需要给每个按钮挂载原生视图。控件样式随保存的材质设置即时切换：实色关闭，其他模式启用；停靠后的不透明 WebContentsView 仍可以模糊其内部内容。减少透明度、高对比度、强制颜色直接回退实色，旧浏览器不支持 backdrop-filter 时保留半透明色层。
