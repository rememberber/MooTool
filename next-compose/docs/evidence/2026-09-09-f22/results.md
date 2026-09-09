# 本轮验收记录

- 阶段/条目：F22 调色板（P4）
- 本产品工作树：仅 `next-compose/`
- 依赖：JDK AWT `Robot` / `JColorChooser`；无新 Maven 库
- 参考：Electron `colorTools.ts`、`ColorBoardTool.tsx`、`screenColorPicker.ts`
- 已执行：`JAVA_HOME` Zulu 21，`./gradlew :composeApp:desktopTest`，**80/80** 通过（ColorEngine 4、ScreenColorSampler 1；此前 F17 为 75/75）
- 语义：HEX/RGB 往返、7 主题 + 10 标准色顺序与 SHA-256、五运算、主色/对比色、Shift 选对比色、屏幕取色冻结截图、自由取色、文件夹收藏、历史与分离窗口
- 差异：DIFF-010（Robot 冻结截图；全黑视为权限失败；收藏 UUID JSON）
- 未测：窗口截图、真实多屏负坐标手工取色、macOS 录屏权限对话框、HiDPI 偏移、安装镜像
- 下一轮：P4 其余（F19 留言板、F23 图片、F24 PDF）
