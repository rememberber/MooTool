import { Check, Copy, Download, Eye, FileInput, Plus, Save, Search, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useEffectEvent, useMemo, useRef, useState } from 'react'
import { Dialog } from '@/shared/components/Dialog'
import { FindReplaceBar } from '@/shared/components/FindReplaceBar'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { TextCodeEditor, type TextCodeEditorHandle } from '@/shared/components/TextCodeEditor'
import {
  defaultFindReplaceOptions,
  findAllMatches,
  findNextMatch,
  replaceAllMatches,
  replaceCurrentMatch,
  type FindReplaceOptions
} from '@/shared/components/findReplace'
import { useToolActivity } from '@/shared/components/ToolActivity'
import type { HostProfile, SystemHostsFile } from '@/shared/contracts/system'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function HostTool() {
  const toolActive = useToolActivity()
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
  const [findOptions, setFindOptions] = useState<FindReplaceOptions>(defaultFindReplaceOptions)
  const [replacedCount, setReplacedCount] = useState(0)
  const [systemHosts, setSystemHosts] = useState<SystemHostsFile | null>(null)
  const [systemDialogOpen, setSystemDialogOpen] = useState(false)
  const [applying, setApplying] = useState(false)
  const editorRef = useRef<TextCodeEditorHandle>(null)
  const dirty = useMemo(() => selected ? selected.name !== name || selected.content !== content : Boolean(name || content), [content, name, selected])
  const findMatches = useMemo(() => findAllMatches(content, find, findOptions), [content, find, findOptions])
  const latestEditorRef = useRef({ selected, name, content })
  const saveQueueRef = useRef<Promise<void>>(Promise.resolve())
  latestEditorRef.current = { selected, name, content }

  const load = useCallback(async () => {
    const items = await window.mootool.listHostProfiles(query)
    setProfiles(items)
    const current = latestEditorRef.current
    if (!current.selected && !current.name && !current.content && items[0]) {
      setSelected(items[0]); setName(items[0].name); setContent(items[0].content)
    }
  }, [query])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 100); return () => clearTimeout(timer) }, [load])

  const persistProfileOnIdle = useEffectEvent((snapshot: { id: number; name: string; content: string }) => {
    void persistProfile(snapshot, false)
  })

  useEffect(() => {
    if (!selected || !dirty || !name.trim()) return
    const snapshot = { id: selected.id, name: name.trim(), content }
    const timer = window.setTimeout(() => persistProfileOnIdle(snapshot), 250)
    return () => window.clearTimeout(timer)
  }, [content, dirty, name, selected])

  useEffect(() => {
    if (!toolActive) return
    const handleFindShortcut = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey) || event.shiftKey || event.altKey) return
      if (event.key.toLowerCase() === 'f' || event.key.toLowerCase() === 'r') {
        event.preventDefault()
        openFindReplace()
      }
    }
    window.addEventListener('keydown', handleFindShortcut)
    return () => window.removeEventListener('keydown', handleFindShortcut)
  })

  async function openProfile(profile: HostProfile): Promise<void> {
    if (dirty && selected && !await persistProfile({ id: selected.id, name: name.trim() || selected.name, content }, false)) return
    setSelected(profile); setName(profile.name); setContent(profile.content)
  }

  async function createProfile(): Promise<void> {
    if (dirty && selected && !await persistProfile({ id: selected.id, name: name.trim() || selected.name, content }, false)) return
    setSelected(null); setName(t('host.untitled')); setContent(defaultHostsTemplate())
  }

  async function save(): Promise<void> {
    const nextName = name.trim() || window.prompt(t('host.namePrompt'), t('host.untitled'))?.trim()
    if (!nextName) return
    try {
      await persistProfile({ id: selected?.id, name: nextName, content }, true)
    } catch (error) { actions.reportError(error) }
  }

  async function persistProfile(snapshot: { id?: number; name: string; content: string }, showToast: boolean): Promise<boolean> {
    if (!snapshot.name) return false
    let saved = false
    const operation = saveQueueRef.current.catch(() => undefined).then(async () => {
      const profile = await window.mootool.saveHostProfile(snapshot)
      saved = true
      const current = latestEditorRef.current
      if ((snapshot.id && current.selected?.id === snapshot.id) || (!snapshot.id && current.selected === null)) {
        setSelected(profile)
        if (current.name === snapshot.name) setName(profile.name)
      }
      setProfiles((items) => [profile, ...items.filter((item) => item.id !== profile.id)])
      if (showToast) actions.toast.success(t('common.saved'))
    })
    saveQueueRef.current = operation.then(() => undefined, () => undefined)
    try {
      await operation
      return saved
    } catch (error) {
      actions.reportError(error)
      return false
    }
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
      const result = await window.mootool.writeSystemHosts(content, selected?.id)
      setSystemHosts(result)
      actions.toast.success(t('host.applied'))
    } catch (error) { actions.reportError(error) } finally { setApplying(false) }
  }

  async function importFile(): Promise<void> {
    try {
      if (dirty && selected && !await persistProfile({ id: selected.id, name: name.trim() || selected.name, content }, false)) return
      const file = await window.mootool.openTextFile('text')
      if (!file) return
      setSelected(null); setName(file.name.replace(/\.[^.]+$/, '')); setContent(file.content)
    } catch (error) { actions.reportError(error) }
  }

  async function exportFile(): Promise<void> {
    try { await window.mootool.saveTextFile({ kind: 'text', defaultName: `${name || 'hosts'}.txt`, content }) } catch (error) { actions.reportError(error) }
  }

  function openFindReplace(): void {
    const selection = editorRef.current?.getSelection()
    const selectedText = selection && selection.end > selection.start
      ? content.slice(selection.start, selection.end)
      : undefined
    setFindVisible(true)
    setReplacedCount(0)
    if (selectedText !== undefined) setFind(selectedText)
  }

  function findAround(forward: boolean): void {
    if (!find) return
    const selection = editorRef.current?.getSelection()
    const fromIndex = forward
      ? (selection?.end ?? 0)
      : (selection?.start ?? 0)
    const match = findNextMatch(content, find, findOptions, fromIndex, forward)
    if (!match) {
      actions.toast.info(t('findReplace.noMatches'))
      return
    }
    editorRef.current?.selectRange(match.start, match.end)
  }

  function replaceCurrent(all: boolean): void {
    if (!find) return
    if (all) {
      const result = replaceAllMatches(content, find, replace, findOptions)
      if (result.count === 0) actions.toast.info(t('findReplace.noMatches'))
      else {
        setContent(result.content)
        setReplacedCount(result.count)
      }
      return
    }
    const result = replaceCurrentMatch(
      content,
      find,
      replace,
      findOptions,
      editorRef.current?.getSelection() ?? null
    )
    if (!result.replaced) {
      actions.toast.info(t('findReplace.noMatches'))
      return
    }
    setContent(result.content)
    setReplacedCount((count) => count + 1)
    const findText = find
    const options = findOptions
    requestAnimationFrame(() => {
      const match = findNextMatch(result.content, findText, options, result.nextFrom, true)
      if (match) editorRef.current?.selectRange(match.start, match.end)
    })
  }

  function closeFindReplace(): void {
    setFindVisible(false)
    setReplacedCount(0)
  }

  return (
    <section className="tool-page p5-tool host-tool-page">
      <ToolPageHeader title={t('host.title')} />
      <ResizableColumns className="local-tool-shell host-workspace" columns={2} defaultSizes={[220, 780]} minPaneWidths={[170, 360]} storageKey="host-workspace">
        <aside className="host-profiles"><header><div className="compact-search"><Search size={13} /><input value={query} placeholder={t('common.search')} aria-label={t('common.search')} onChange={(event) => setQuery(event.target.value)} /></div><button className="icon-button" type="button" aria-label={t('common.new')} onClick={() => { void createProfile() }}><Plus size={14} /></button></header><div>{profiles.length === 0 ? <div className="history-empty">{t('host.empty')}</div> : profiles.map((profile) => <button className={profile.id === selected?.id ? 'host-profile host-profile--active' : 'host-profile'} type="button" key={profile.id} onClick={() => { void openProfile(profile) }}><strong>{profile.name}</strong><span>{profile.modifiedTime}</span></button>)}</div><footer><button className="icon-button" type="button" aria-label={t('host.import')} onClick={() => { void importFile() }}><FileInput size={14} /></button><button className="icon-button" type="button" disabled={!content} aria-label={t('host.export')} onClick={() => { void exportFile() }}><Download size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!selected} aria-label={t('common.action.delete')} onClick={() => { void remove() }}><Trash2 size={14} /></button></footer></aside>
        <main className="host-editor"><div className="host-toolbar"><input className="host-name" value={name} aria-label={t('host.profileName')} placeholder={t('host.profileName')} onChange={(event) => setName(event.target.value)} /><span className="p4-toolbar__spacer" /><button className="toolbar-button" type="button" onClick={() => { void showSystemHosts() }}><Eye size={14} />{t('host.current')}</button><button className="toolbar-button" type="button" onClick={() => { if (findVisible) closeFindReplace(); else openFindReplace() }}><Search size={14} />{t('host.find')}</button><button className="toolbar-button" type="button" disabled={!dirty} onClick={() => { void save() }}><Save size={14} />{t('common.save')}</button><button className="toolbar-button toolbar-button--primary" data-testid="host-apply" type="button" disabled={!content || applying} onClick={() => { void apply() }}><Check size={14} />{applying ? t('host.applying') : t('host.apply')}</button></div>
          {findVisible && (
            <FindReplaceBar
              className="host-findbar"
              findText={find}
              replaceText={replace}
              options={findOptions}
              matchCount={findMatches.length}
              replacedCount={replacedCount}
              onFindTextChange={(value) => { setFind(value); setReplacedCount(0) }}
              onReplaceTextChange={setReplace}
              onOptionsChange={(options) => { setFindOptions(options); setReplacedCount(0) }}
              onFind={() => findAround(true)}
              onFindPrevious={() => findAround(false)}
              onFindNext={() => findAround(true)}
              onReplace={() => replaceCurrent(false)}
              onReplaceAll={() => replaceCurrent(true)}
              onClose={closeFindReplace}
            />
          )}
          <TextCodeEditor ref={editorRef} className="host-content-editor" testId="host-content" value={content} ariaLabel={t('host.content')} placeholder={t('host.placeholder')} searchQuery={findVisible ? find : ''} searchOptions={findOptions} onChange={setContent} />
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
