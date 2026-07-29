import {
  Boxes,
  ChevronDown,
  Command,
  Languages,
  PanelLeftClose,
  Search,
  Settings
} from 'lucide-react'
import { useEffect, useMemo, useState } from 'react'
import { CalculatorPage } from '../features/calculator/CalculatorPage'
import { HomePage } from '../features/home/HomePage'
import { WebviewLab } from '../features/webviewLab/WebviewLab'
import { runtimeApi } from '../platform/api/runtimeApi'
import type { RuntimeInfo } from '../platform/contracts/runtime'
import { homeTool, toolCatalog, toolGroups, type ToolId } from './toolCatalog'

export function App() {
  const [activeTool, setActiveTool] = useState<ToolId>('home')
  const [query, setQuery] = useState('')
  const [sidebarCompact, setSidebarCompact] = useState(false)
  const [runtimeInfo, setRuntimeInfo] = useState<RuntimeInfo>()
  const [notice, setNotice] = useState('')
  const [recent, setRecent] = useState<ToolId[]>([])

  useEffect(() => {
    void runtimeApi.getInfo().then(setRuntimeInfo).catch((error: unknown) => {
      setNotice(error instanceof Error ? error.message : '无法读取 Tauri 运行时信息')
    })
  }, [])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
        event.preventDefault()
        document.getElementById('tool-search')?.focus()
      }
    }
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [])

  const visibleGroups = useMemo(() => toolGroups.map((group) => ({
    group,
    tools: toolCatalog.filter((tool) => tool.group === group && (
      !query.trim()
      || `${tool.title} ${tool.keywords.join(' ')}`.toLowerCase().includes(query.trim().toLowerCase())
    ))
  })).filter(({ tools }) => tools.length > 0), [query])

  function openTool(toolId: ToolId): void {
    const tool = toolCatalog.find((item) => item.id === toolId)
    if (tool && !tool.ready) {
      setNotice(`${tool.title} 已进入独立产品路线图，当前 P0 先验证工作台与 Calculator 垂直切片。`)
      return
    }
    setActiveTool(toolId)
    setNotice('')
    if (toolId !== 'home') {
      setRecent((items) => [toolId, ...items.filter((item) => item !== toolId)].slice(0, 5))
    }
  }

  return (
    <main className={`app-shell ${sidebarCompact ? 'app-shell--compact' : ''}`}>
      <div className="window-drag-region" data-tauri-drag-region />
      <aside className="sidebar">
        <div className="sidebar-toolbar">
          <button
            className="icon-button"
            type="button"
            aria-label={sidebarCompact ? '展开导航' : '收起导航'}
            onClick={() => setSidebarCompact((value) => !value)}
          >
            <PanelLeftClose />
          </button>
          <label className="search-control">
            <Search />
            <input
              id="tool-search"
              value={query}
              placeholder="搜索工具"
              onChange={(event) => setQuery(event.target.value)}
            />
            <kbd><Command />K</kbd>
          </label>
        </div>

        <nav className="tool-nav" aria-label="工具导航">
          <NavButton
            icon={homeTool.icon}
            label={homeTool.title}
            active={activeTool === 'home'}
            compact={sidebarCompact}
            onClick={() => openTool('home')}
          />
          {visibleGroups.map(({ group, tools }) => (
            <section className="nav-group" key={group}>
              <h2>{group}</h2>
              {tools.map((tool) => (
                <NavButton
                  key={tool.id}
                  icon={tool.icon}
                  label={tool.title}
                  active={activeTool === tool.id}
                  compact={sidebarCompact}
                  planned={!tool.ready}
                  onClick={() => openTool(tool.id)}
                />
              ))}
            </section>
          ))}

          {!query && recent.length > 0 && (
            <section className="recent-group">
              <h2>最近 <ChevronDown /></h2>
              {recent.map((toolId) => {
                const tool = toolCatalog.find((item) => item.id === toolId)
                return tool && <button type="button" key={toolId} onClick={() => openTool(toolId)}>{tool.title}</button>
              })}
            </section>
          )}
        </nav>

        <footer className="sidebar-footer">
          <div className="brand-lockup">
            <span className="brand-symbol"><Boxes /></span>
            <span>MooTool <small>Tauri</small></span>
          </div>
          <div className="footer-actions">
            <button className="icon-button" type="button" aria-label="语言"><Languages /></button>
            <button className="icon-button" type="button" aria-label="设置"><Settings /></button>
          </div>
        </footer>
      </aside>

      <section className="workspace">
        <div className={activeTool === 'home' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <HomePage runtimeInfo={runtimeInfo} onOpenCalculator={() => openTool('calculator')} />
        </div>
        <div className={activeTool === 'calculator' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <CalculatorPage />
        </div>
        <div className={activeTool === 'webview-lab' ? 'view-layer' : 'view-layer view-layer--hidden'}>
          <WebviewLab active={activeTool === 'webview-lab'} />
        </div>
        {notice && (
          <button className="notice-toast" type="button" onClick={() => setNotice('')}>
            {notice}
          </button>
        )}
      </section>
    </main>
  )
}

function NavButton({ icon: Icon, label, active, compact, planned = false, onClick }: {
  icon: typeof Boxes
  label: string
  active: boolean
  compact: boolean
  planned?: boolean
  onClick(): void
}) {
  return (
    <button
      className={`nav-button ${active ? 'nav-button--active' : ''}`}
      type="button"
      title={compact ? label : undefined}
      onClick={onClick}
    >
      <Icon />
      <span>{label}</span>
      {planned && <i aria-label="计划中" />}
    </button>
  )
}
