# 本轮验收记录

- 阶段/条目：F21 计算器（P3 的第二个本地算法工具）
- 本产品工作树：仅 `next-compose/`
- 参考：Electron `calculatorTools.ts`、`CalculatorTool.tsx`
- 已执行：`./gradlew :composeApp:desktopTest`，CalculatorEngine 4 项与既有套件一并通过
- 语义：递归下降解析四则/括号/一元符号，长度 500，禁止标识符；进制与 GCD/LCM/排列组合使用 BigInteger；表达式结果为 IEEE Double 再按 14 位有效数字展示（与 Electron `toPrecision(14)` 对齐），未使用任意精度小数
- 未测：窗口截图、分离窗口手工、重启 UI
- 下一轮：P3 其余工具（编码、UA、正则、Cron、Diff 等）；F04 Git 仍待做
