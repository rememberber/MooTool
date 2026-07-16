import { Check, Copy, Download, Eye, FileInput, Plus, Save, Search, Trash2, X } from 'lucide-react'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import type { HostProfile, SystemHostsFile } from '@/shared/contracts/system'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function HostTool() {
  const { t } = useI18n()
  const actions = useToolActions('host')
  const [profiles, setProfiles] = useState<HostProfile[]>([])
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<HostProfile | null>(null)
  const [name, setName] = useState('')
  const [content, setContent] = useState('')
  const [findVisible, setFindVisible] = useState(false)
  const [find, setFind] = useState('')
  const [replace, setReplace] = useState('')
  const [systemHosts, setSystemHosts] = useState<SystemHostsFile | null>(null)
  const [systemDialogOpen, setSystemDialogOpen] = useState(false)
  const [applying, setApplying] = useState(false)
  const dirty = useMemo(() => selected ? selected.name !== name || selected.content !== content : Boolean(name || content), [content, name, selected])

  const load = useCallback(async () => {
    const items = await window.mootool.listHostProfiles(query)
    setProfiles(items)
    if (!selected && items[0]) openProfile(items[0])
  }, [query, selected])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 100); return () => clearTimeout(timer) }, [load])

  function openProfile(profile: HostProfile): void {
    if (dirty && selected && !window.confirm(t('host.discardChanges'))) return
    setSelected(profile); setName(profile.name); setContent(profile.content)
  }

  function createProfile(): void {
    if (dirty && !window.confirm(t('host.discardChanges'))) return
    setSelected(null); setName(t('host.untitled')); setContent(defaultHostsTemplate())
  }

  async function save(): Promise<void> {
    const nextName = name.trim() || window.prompt(t('host.namePrompt'), t('host.untitled'))?.trim()
    if (!nextName) return
    try {
      const profile = await window.mootool.saveHostProfile({ id: selected?.id, name: nextName, content })
      setSelected(profile); setName(profile.name); await load(); actions.toast.success(t('common.saved'))
    } catch (error) { actions.reportError(error) }
  }

  async function remove(): Promise<void> {
    if (!selected || !window.confirm(t('host.confirmDelete'))) return
    try { await window.mootool.deleteHostProfile(selected.id); setSelected(null); setName(''); setContent(''); await load() } catch (error) { actions.reportError(error) }
  }

  async function showSystemHosts(): Promise<void> {
    try { setSystemHosts(await window.mootool.readSystemHosts()); setSystemDialogOpen(true) } catch (error) { actions.reportError(error) }
  }

  async function apply(): Promise<void> {
    if (!content.trim() || !window.confirm(t('host.confirmApply'))) return
    setApplying(true)
    try {
      const result = await window.mootool.writeSystemHosts(content)
      setSystemHosts(result)
      actions.toast.success(t('host.applied'))
    } catch (error) { actions.reportError(error) } finally { setApplying(false) }
  }

  async function importFile(): Promise<void> {
    try {
      const file = await window.mootool.openTextFile('text')
      if (!file) return
      setSelected(null); setName(file.name.replace(/\.[^.]+$/, '')); setContent(file.content)
    } catch (error) { actions.reportError(error) }
  }

  async function exportFile(): Promise<void> {
    try { await window.mootool.saveTextFile({ kind: 'text', defaultName: `${name || 'hosts'}.txt`, content }) } catch (error) { actions.reportError(error) }
  }

  function replaceNext(): void {
    if (!find) return
    const index = content.indexOf(find)
    if (index >= 0) setContent(`${content.slice(0, index)}${replace}${content.slice(index + find.length)}`)
  }

  function replaceAll(): void {
    if (find) setContent(content.split(find).join(replace))
  }

  return (
    <section className="tool-page p5-tool host-tool-page">
      <ToolPageHeader title={t('host.title')} />
      <ResizableColumns className="local-tool-shell host-workspace" columns={2} defaultSizes={[220, 780]} minPaneWidths={[170, 360]} storageKey="host-workspace">
        <aside className="host-profiles"><header><div className="compact-search"><Search size={13} /><input value={query} placeholder={t('common.search')} aria-label={t('common.search')} onChange={(event) => setQuery(event.target.value)} /></div><button className="icon-button" type="button" aria-label={t('common.new')} onClick={createProfile}><Plus size={14} /></button></header><div>{profiles.length === 0 ? <div className="history-empty">{t('host.empty')}</div> : profiles.map((profile) => <button className={profile.id === selected?.id ? 'host-profile host-profile--active' : 'host-profile'} type="button" key={profile.id} onClick={() => openProfile(profile)}><strong>{profile.name}</strong><span>{profile.modifiedTime}</span></button>)}</div><footer><button className="icon-button" type="button" aria-label={t('host.import')} onClick={() => { void importFile() }}><FileInput size={14} /></button><button className="icon-button" type="button" disabled={!content} aria-label={t('host.export')} onClick={() => { void exportFile() }}><Download size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!selected} aria-label={t('common.action.delete')} onClick={() => { void remove() }}><Trash2 size={14} /></button></footer></aside>
        <main className="host-editor"><div className="host-toolbar"><input className="host-name" value={name} aria-label={t('host.profileName')} placeholder={t('host.profileName')} onChange={(event) => setName(event.target.value)} /><span className="p4-toolbar__spacer" /><button className="toolbar-button" type="button" onClick={() => { void showSystemHosts() }}><Eye size={14} />{t('host.current')}</button><button className="toolbar-button" type="button" onClick={() => setFindVisible((value) => !value)}><Search size={14} />{t('host.find')}</button><button className="toolbar-button" type="button" disabled={!dirty} onClick={() => { void save() }}><Save size={14} />{t('common.save')}</button><button className="toolbar-button toolbar-button--primary" data-testid="host-apply" type="button" disabled={!content || applying} onClick={() => { void apply() }}><Check size={14} />{applying ? t('host.applying') : t('host.apply')}</button></div>
          {findVisible && <div className="host-find-bar"><input value={find} placeholder={t('host.findPlaceholder')} onChange={(event) => setFind(event.target.value)} /><input value={replace} placeholder={t('host.replacePlaceholder')} onChange={(event) => setReplace(event.target.value)} /><button className="dialog-button" type="button" onClick={replaceNext}>{t('host.replace')}</button><button className="dialog-button" type="button" onClick={replaceAll}>{t('host.replaceAll')}</button><button className="icon-button" type="button" aria-label={t('common.close')} onClick={() => setFindVisible(false)}><X size={13} /></button></div>}
          <textarea data-testid="host-content" value={content} aria-label={t('host.content')} placeholder={t('host.placeholder')} spellCheck={false} onChange={(event) => setContent(event.target.value)} />
        </main>
      </ResizableColumns>
      <Dialog title={t('host.current')} open={systemDialogOpen} width={820} onClose={() => setSystemDialogOpen(false)} footer={<><button className="dialog-button" type="button" disabled={!systemHosts} onClick={() => { if (systemHosts) void actions.copy(systemHosts.content) }}><CopyIcon />{t('common.action.copy')}</button><button className="dialog-button" type="button" onClick={() => setSystemDialogOpen(false)}>{t('common.close')}</button></>}><div className="system-hosts-meta"><code>{systemHosts?.path}</code><span>{systemHosts?.writable ? t('host.writable') : t('host.requiresPrivilege')}</span></div><textarea className="system-hosts-view" readOnly value={systemHosts?.content ?? ''} /></Dialog>
    </section>
  )
}

function CopyIcon() {
  return <Copy size={14} />
}

function defaultHostsTemplate(): string {
  return '# MooTool hosts profile\n127.0.0.1 localhost\n::1 localhost\n'
}
