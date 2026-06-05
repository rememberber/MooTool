import {
  Braces,
  CalendarClock,
  ChevronDown,
  Clock3,
  Code2,
  FileText,
  Folder,
  Globe,
  Home,
  Image,
  MessageSquarePlus,
  Palette,
  QrCode,
  Regex,
  Search,
  Settings,
  Shuffle,
  Sparkle,
  Terminal,
  Wand2
} from 'lucide-react'
import { ToolButton } from '@/shared/components/ToolButton'

const primaryTools = [
  { icon: Home, label: '主页', active: true },
  { icon: Braces, label: 'JSON' },
  { icon: Clock3, label: '时间转换' },
  { icon: Shuffle, label: '编码解码' },
  { icon: QrCode, label: '二维码' },
  { icon: Globe, label: 'HTTP 请求' },
  { icon: Code2, label: '文本 Diff' },
  { icon: Regex, label: '正则' },
  { icon: Palette, label: '颜色板' },
  { icon: Image, label: '图片处理' },
  { icon: CalendarClock, label: 'Cron' }
]

const recentItems = ['JSON 格式化片段', '接口调试草稿', 'Base64 转换', '颜色收藏', '正则测试']

export function Workbench() {
  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="window-drag toolbar-spacer" />

        <div className="sidebar-actions">
          <button className="icon-ghost" aria-label="折叠侧栏">
            <FileText size={17} />
          </button>
          <button className="icon-ghost" aria-label="搜索">
            <Search size={17} />
          </button>
        </div>

        <div className="mode-switch" aria-label="工作模式">
          <button className="mode-switch__item mode-switch__item--active">
            <Wand2 size={15} />
            工具
          </button>
          <button className="mode-switch__item">
            <Terminal size={15} />
            脚本
          </button>
        </div>

        <nav className="tool-nav" aria-label="工具导航">
          {primaryTools.map((tool) => (
            <ToolButton key={tool.label} {...tool} />
          ))}
        </nav>

        <section className="recent-section">
          <div className="section-title">
            <span>最近</span>
            <ChevronDown size={15} />
          </div>
          <div className="recent-list">
            {recentItems.map((item, index) => (
              <button className="recent-item" key={item}>
                <span className={index === 1 ? 'recent-dot recent-dot--blue' : 'recent-dot'} />
                <span>{item}</span>
              </button>
            ))}
          </div>
        </section>

        <div className="sidebar-footer">
          <div className="brand-mark">
            <Sparkle size={18} />
            <span>MooTool</span>
          </div>
          <button className="icon-ghost" aria-label="设置">
            <Settings size={17} />
          </button>
        </div>
      </aside>

      <section className="workspace">
        <div className="window-drag workspace-topbar">
          <span>Home</span>
          <ChevronDown size={14} />
          <span className="topbar-muted">Local</span>
        </div>

        <div className="hero-pattern" aria-hidden="true" />

        <div className="home-panel">
          <div className="headline-row">
            <Sparkle className="headline-icon" size={28} />
            <h1>把常用工具收进一个安静的桌面</h1>
          </div>
          <p className="subtle-link">MooTool Next · Electron / React / TypeScript</p>

          <div className="command-box">
            <div className="command-input">想处理什么？</div>
            <div className="command-controls">
              <button className="round-button">+</button>
              <button className="pill-button">
                JSON
                <ChevronDown size={16} />
              </button>
              <button className="send-button">↑</button>
            </div>
          </div>

          <div className="quick-grid">
            <button className="quick-card">
              <Braces size={30} />
              <strong>JSON</strong>
              <span>格式化、压缩、转义</span>
            </button>
            <button className="quick-card">
              <Globe size={30} />
              <strong>HTTP</strong>
              <span>发送请求，保存草稿</span>
            </button>
            <button className="quick-card">
              <Code2 size={30} />
              <strong>Diff</strong>
              <span>并排或统一视图</span>
            </button>
          </div>
        </div>
      </section>

      <aside className="detail-pane">
        <div className="window-drag pane-topbar">
          <button className="icon-ghost" aria-label="新建">
            <MessageSquarePlus size={17} />
          </button>
          <button className="icon-ghost" aria-label="打开项目">
            <Folder size={17} />
          </button>
        </div>

        <div className="state-card">
          <span className="state-kicker">当前状态</span>
          <strong>本地模式</strong>
          <p>后续每个工具通过 preload 暴露安全 API，React 只负责界面和交互。</p>
        </div>
      </aside>
    </main>
  )
}
