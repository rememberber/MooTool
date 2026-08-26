import { Check, Copy, Download, Eye, FileInput, Plus, Save, Search, Trash2 } from 'lucide-react'
import { useCallback, useEffect, useEffectEvent, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { Dialog } from '@/shared/components/Dialog'
import { FindReplaceBar } from '@/shared/components/FindReplaceBar'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader, WorkspaceDragZone } from '@/shared/components/ToolPage'
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
import { useDesktopDialog } from '@/shared/feedback/DesktopDialogProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'

export function HostTool() {
  const toolActive = useToolActivity()
  const { t } = useI18n()
  const actions = useToolActions('host')
  const desktopDialog = useDesktopDialog()
  const [profiles, setProfiles] = useState<HostProfile[]>([])
  const [query, setQuery] = useState('')
  const [includeContent, setIncludeContent] = useState(true)
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
  const [contextMenu, setContextMenu] = useState<{ profile: HostProfile; left: number; top: number } | null>(null)
  const [renameTarget, setRenameTarget] = useState<HostProfile | null>(null)
  const [renameValue, setRenameValue] = useState('')
  const editorRef = useRef<TextCodeEditorHandle>(null)
  const contextMenuRef = useRef<HTMLDivElement>(null)
  const dirty = useMemo(() => selected ? selected.name !== name || selected.content !== content : Boolean(name || content), [content, name, selected])
  const findMatches = useMemo(() => findAllMatches(content, find, findOptions), [content, find, findOptions])
  const latestEditorRef = useRef({ selected, name, content })
  const saveQueueRef = useRef<Promise<void>>(Promise.resolve())
  latestEditorRef.current = { selected, name, content }

  const load = useCallback(async () => {
    const items = await window.mootool.listHostProfiles({ keyword: query, includeContent })
    setProfiles(items)
    const current = latestEditorRef.current
    if (!current.selected && !current.name && !current.content && items[0]) {
      setSelected(items[0]); setName(items[0].name); setContent(items[0].content)
    }
  }, [includeContent, query])
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

  useEffect(() => {
    if (!contextMenu) return
    if (!toolActive) {
      setContextMenu(null)
      return
    }
    const focusFrame = window.requestAnimationFrame(() => contextMenuRef.current?.querySelector('button')?.focus())
    const close = () => setContextMenu(null)
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', closeOnEscape)
    window.addEventListener('blur', close)
    return () => {
      window.cancelAnimationFrame(focusFrame)
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', closeOnEscape)
      window.removeEventListener('blur', close)
    }
  }, [contextMenu, toolActive])

  async function openProfile(profile: HostProfile): Promise<boolean> {
    if (dirty && selected && !await persistProfile({ id: selected.id, name: name.trim() || selected.name, content }, false)) return false
    setSelected(profile); setName(profile.name); setContent(profile.content)
    return true
  }

  async function createProfile(): Promise<void> {
    if (dirty && selected && !await persistProfile({ id: selected.id, name: name.trim() || selected.name, content }, false)) return
    setSelected(null); setName(t('host.untitled')); setContent(defaultHostsTemplate())
  }

  async function save(): Promise<void> {
    const nextName = name.trim() || (await desktopDialog.prompt(t('host.namePrompt'), { defaultValue: t('host.untitled'), confirmLabel: t('common.save') }))?.trim()
    if (!nextName) return
    try {
      await persistProfile({ id: selected?.id, name: nextName, content }, true)
    } catch (error) { actions.reportError(error) }
  }

  async function persistProfile(snapshot: { id?: number; name: string; content: string }, showToast: boolean, syncEditorName = false): Promise<boolean> {
    if (!snapshot.name) return false
    let saved = false
    const operation = saveQueueRef.current.catch(() => undefined).then(async () => {
      const profile = await window.mootool.saveHostProfile(snapshot)
      saved = true
      const current = latestEditorRef.current
      if ((snapshot.id && current.selected?.id === snapshot.id) || (!snapshot.id && current.selected === null)) {
        setSelected(profile)
        if (syncEditorName || current.name === snapshot.name) setName(profile.name)
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

  async function remove(profile = selected): Promise<void> {
    if (!profile || !await desktopDialog.confirm(t('host.confirmDelete'), { confirmLabel: t('common.action.delete'), danger: true })) return
    try {
      await window.mootool.deleteHostProfile(profile.id)
      if (selected?.id === profile.id) {
        setSelected(null)
        setName('')
        setContent('')
      }
      setContextMenu(null)
      await load()
    } catch (error) { actions.reportError(error) }
  }

  async function showSystemHosts(): Promise<void> {
    try { setSystemHosts(await window.mootool.readSystemHosts()); setSystemDialogOpen(true) } catch (error) { actions.reportError(error) }
  }

  async function apply(): Promise<void> {
    if (!content.trim() || !await desktopDialog.confirm(t('host.confirmApply'), { confirmLabel: t('common.action.apply') })) return
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

  async function exportFile(profile?: HostProfile): Promise<void> {
    const current = latestEditorRef.current
    const useCurrentEditor = !profile || current.selected?.id === profile.id
    const exportName = useCurrentEditor ? current.name : profile.name
    const exportContent = useCurrentEditor ? current.content : profile.content
    try { await window.mootool.saveTextFile({ kind: 'text', defaultName: `${exportName || 'hosts'}.txt`, content: exportContent }) } catch (error) { actions.reportError(error) }
  }

  function beginRenameProfile(profile: HostProfile): void {
    const current = latestEditorRef.current
    const useCurrentEditor = current.selected?.id === profile.id
    const target = useCurrentEditor ? { ...profile, name: current.name, content: current.content } : profile
    setRenameTarget(target)
    setRenameValue(target.name)
  }

  async function submitRenameProfile(): Promise<void> {
    if (!renameTarget) return
    const nextName = renameValue.trim()
    if (!nextName) return
    if (nextName === renameTarget.name) {
      setRenameTarget(null)
      return
    }
    const current = latestEditorRef.current
    const useCurrentEditor = current.selected?.id === renameTarget.id
    const profileContent = useCurrentEditor ? current.content : renameTarget.content
    if (!await persistProfile({ id: renameTarget.id, name: nextName, content: profileContent }, false, true)) return
    setRenameTarget(null)
  }

  async function openProfileContextMenu(profile: HostProfile, left: number, top: number): Promise<void> {
    if (selected?.id !== profile.id && !await openProfile(profile)) return
    setContextMenu({ profile, left, top })
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
        <aside className="host-profiles"><header><div className="compact-search"><Search size={13} /><input value={query} placeholder={t('common.search')} aria-label={t('common.search')} onChange={(event) => setQuery(event.target.value)} /></div><button className="icon-button" type="button" aria-label={t('common.new')} onClick={() => { void createProfile() }}><Plus size={14} /></button><label className="list-search-content host-search-content"><input type="checkbox" checked={includeContent} onChange={(event) => setIncludeContent(event.target.checked)} />{t('common.searchContent')}</label></header><div>{profiles.length === 0 ? <div className="history-empty">{t('host.empty')}</div> : profiles.map((profile) => <button className={profile.id === selected?.id ? 'host-profile host-profile--active' : 'host-profile'} type="button" key={profile.id} onClick={() => { void openProfile(profile) }} onContextMenu={(event) => { event.preventDefault(); void openProfileContextMenu(profile, Math.min(event.clientX, window.innerWidth - 176), Math.min(event.clientY, window.innerHeight - 126)) }}><strong>{profile.name}</strong><span>{profile.modifiedTime}</span></button>)}</div><footer><button className="icon-button" type="button" aria-label={t('host.import')} onClick={() => { void importFile() }}><FileInput size={14} /></button><button className="icon-button" type="button" disabled={!content} aria-label={t('host.export')} onClick={() => { void exportFile() }}><Download size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!selected} aria-label={t('common.action.delete')} onClick={() => { void remove() }}><Trash2 size={14} /></button></footer></aside>
        <main className="host-editor"><div className="host-toolbar"><input className="host-name" value={name} aria-label={t('host.profileName')} placeholder={t('host.profileName')} onChange={(event) => setName(event.target.value)} /><WorkspaceDragZone className="p4-toolbar__spacer" /><button className="toolbar-button" type="button" onClick={() => { void showSystemHosts() }}><Eye size={14} />{t('host.current')}</button><button className="toolbar-button" type="button" onClick={() => { if (findVisible) closeFindReplace(); else openFindReplace() }}><Search size={14} />{t('host.find')}</button><button className="toolbar-button" type="button" disabled={!dirty} onClick={() => { void save() }}><Save size={14} />{t('common.save')}</button><button className="toolbar-button toolbar-button--primary" data-testid="host-apply" type="button" disabled={!content || applying} onClick={() => { void apply() }}><Check size={14} />{applying ? t('host.applying') : t('host.apply')}</button></div>
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
      {contextMenu && toolActive && createPortal(
        <div
          ref={contextMenuRef}
          className="host-profile-menu"
          role="menu"
          aria-label={t('host.contextMenu')}
          style={{ left: contextMenu.left, top: contextMenu.top }}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <button type="button" role="menuitem" onClick={() => { const profile = contextMenu.profile; setContextMenu(null); beginRenameProfile(profile) }}>{t('common.rename')}</button>
          <button type="button" role="menuitem" onClick={() => { const profile = contextMenu.profile; setContextMenu(null); void exportFile(profile) }}>{t('common.export')}</button>
          <button type="button" role="menuitem" onClick={() => { const profile = contextMenu.profile; setContextMenu(null); void remove(profile) }}>{t('common.action.delete')}</button>
        </div>,
        document.body
      )}
      <Dialog
        title={t('common.rename')}
        open={renameTarget !== null}
        width={420}
        onClose={() => setRenameTarget(null)}
        footer={(
          <>
            <button className="dialog-button" type="button" onClick={() => setRenameTarget(null)}>{t('common.cancel')}</button>
            <button className="dialog-button dialog-button--primary" type="button" disabled={!renameValue.trim()} onClick={() => { void submitRenameProfile() }}>{t('common.save')}</button>
          </>
        )}
      >
        <label className="vault-new-field">
          <span>{t('host.profileName')}</span>
          <input autoFocus value={renameValue} onChange={(event) => setRenameValue(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && renameValue.trim()) void submitRenameProfile() }} />
        </label>
      </Dialog>
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
