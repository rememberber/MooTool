# MooTool Next Tauri

MooTool Next Tauri 是基于 Tauri 2、Rust、React 和 TypeScript 的独立桌面产品线。它与 MooTool Java、MooTool Next Electron 独立安装、独立存储、独立发布和独立演进。

## 本地开发

前置条件：

- Node.js 20+
- Rust stable
- Tauri 对应平台的系统依赖

```bash
npm install
npm run dev
```

常用检查：

```bash
npm run typecheck
npm test
npm run check
npm run build:desktop
```

当前原生 P0 能力可在“系统工具 → WebView 实验台”中验证：创建一个 Rust 管理的子 WebView，在主窗口与独立原生窗口之间分离、收回，并执行 100 次状态保持压力测试。

实现边界和分阶段计划见 [`doc/independent-product-implementation-plan.md`](doc/independent-product-implementation-plan.md)，实测进度见 [`doc/p0-validation.md`](doc/p0-validation.md)。
