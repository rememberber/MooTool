import { Boxes, CheckCircle2, Cpu, Database, GitBranch, PackageCheck } from 'lucide-react'
import type { RuntimeInfo } from '../../platform/contracts/runtime'

interface HomePageProps {
  runtimeInfo?: RuntimeInfo
  onOpenCalculator(): void
}

export function HomePage({ runtimeInfo, onOpenCalculator }: HomePageProps) {
  return (
    <section className="home-page">
      <div className="home-hero">
        <div className="brand-symbol brand-symbol--large"><Boxes aria-hidden="true" /></div>
        <div>
          <span className="eyebrow">INDEPENDENT DESKTOP PRODUCT</span>
          <div className="home-title-row">
            <h1>MooTool Next Tauri</h1>
            <span className="version-pill">v{runtimeInfo?.version ?? '…'}</span>
          </div>
          <p>给开发者准备的桌面小工具，以 Tauri、Rust 和系统 WebView 独立演进。</p>
          <button className="primary-button" type="button" onClick={onOpenCalculator}>
            打开首个可用工具
          </button>
        </div>
      </div>

      <div className="home-grid">
        <article className="home-card home-card--accent">
          <span className="card-icon"><CheckCircle2 /></span>
          <div>
            <h2>P0 工程基线已启动</h2>
            <p>独立包管理、Rust Core、Tauri-owned API 与 Calculator 垂直切片。</p>
          </div>
        </article>
        <article className="home-card">
          <span className="card-icon"><GitBranch /></span>
          <div>
            <h2>独立产品线</h2>
            <p>不依赖 Electron 源码、构建产物、数据目录或发布版本。</p>
          </div>
        </article>
        <article className="home-card">
          <span className="card-icon"><Database /></span>
          <div>
            <h2>数据默认隔离</h2>
            <p>后续通过显式导入与导出实现跨产品数据可携带。</p>
          </div>
        </article>
        <article className="home-card">
          <span className="card-icon"><PackageCheck /></span>
          <div>
            <h2>独立发布</h2>
            <p>应用 ID、版本、安装包、更新节点和 Release Notes 均归 Tauri 产品所有。</p>
          </div>
        </article>
      </div>

      <section className="runtime-panel">
        <div className="runtime-title">
          <Cpu />
          <div>
            <h2>当前运行时</h2>
            <p>由 Rust Command 返回的产品身份与平台信息</p>
          </div>
        </div>
        <dl>
          <div><dt>产品 ID</dt><dd>{runtimeInfo?.productId ?? '读取中'}</dd></div>
          <div><dt>运行时</dt><dd>{runtimeInfo?.runtime ?? '读取中'}</dd></div>
          <div><dt>平台</dt><dd>{runtimeInfo ? `${runtimeInfo.platform} · ${runtimeInfo.architecture}` : '读取中'}</dd></div>
        </dl>
      </section>
    </section>
  )
}
