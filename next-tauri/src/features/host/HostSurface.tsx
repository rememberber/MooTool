import { CheckCircle2, Download, Eye, FileInput, Globe2, Plus, RefreshCw, Save, Search, ServerCog, Trash2, TriangleAlert, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { hostApi } from '../../platform/api/hostApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import type { HostProfile } from '../../platform/contracts/localData'
import type { SystemHostsFile } from '../../platform/contracts/host'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { hostMessages } from './hostMessages'

const template = '# MooTool Next Tauri hosts profile\n127.0.0.1 localhost\n::1 localhost\n'
type HostMessageKey = LocalizedMessageKey<typeof hostMessages>
type Notice = { key: HostMessageKey; values?: MessageValues } | { raw: string }

export function HostSurface() {
  const dialog = useDesktopDialog()
  const { t, locale } = useLocalizedMessages(hostMessages)
  const [profiles, setProfiles] = useState<HostProfile[]>([])
  const [activeId, setActiveId] = useState('')
  const [name, setName] = useState(() => t('profile.new'))
  const [content, setContent] = useState(template)
  const [pristineNew, setPristineNew] = useState(true)
  const [system, setSystem] = useState<SystemHostsFile>()
  const [showSystem, setShowSystem] = useState(false)
  const [dnsHost, setDnsHost] = useState('localhost')
  const [addresses, setAddresses] = useState<string[]>([])
  const [query, setQuery] = useState('')
  const [findVisible, setFindVisible] = useState(false)
  const [find, setFind] = useState('')
  const [replace, setReplace] = useState('')
  const [notice, setNotice] = useState<Notice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)
  const revisionRef = useRef(0)
  const active = profiles.find((item) => item.id === activeId)
  const dirty = !active || active.name !== name || active.content !== content
  const shouldPersistDraft = dirty && Boolean(active || !pristineNew)
  const visibleProfiles = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle ? profiles.filter((item) => `${item.name}\n${item.content}`.toLowerCase().includes(needle)) : profiles
  }, [profiles, query])
  const matchCount = useMemo(() => countTextMatches(content, find), [content, find])
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ activeId, name, hash: contentFingerprint(content), profiles: profiles.length, showSystem }),
    summary: t('session.summary', { count: profiles.length, state: t(dirty ? 'session.unsaved' : 'session.saved') })
  }), [activeId, content, dirty, name, profiles.length, showSystem, t])
  const { sessionId, reportError } = useToolSessionReport('host', session.digest, session.summary)
  const recordOperation = useOperationHistory('host')

  useEffect(() => { void loadProfiles(); void refreshSystem() }, [])

  useEffect(() => {
    if (pristineNew && !active) setName(t('profile.new'))
  }, [active, locale, pristineNew, t])

  useEffect(() => {
    if (!shouldPersistDraft || !name.trim() || showSystem || busy) return
    const timer = window.setTimeout(() => {
      void persistCurrent(false)
    }, 500)
    return () => window.clearTimeout(timer)
  }, [busy, content, name, shouldPersistDraft, showSystem])

  async function loadProfiles(): Promise<void> {
    try {
      const loaded = await localDataApi.listHostProfiles()
      setProfiles(loaded)
      if (loaded[0]) void select(loaded[0])
    } catch (cause) { fail(cause) }
  }

  async function refreshSystem(): Promise<void> {
    try { setSystem(await hostApi.readSystem()) } catch (cause) { fail(cause) }
  }

  async function select(profile: HostProfile): Promise<void> {
    if (shouldPersistDraft && !await persistCurrent(false)) return
    setActiveId(profile.id); applyDraft(profile.name, profile.content, false); setShowSystem(false)
  }

  async function create(): Promise<void> {
    if (shouldPersistDraft && !await persistCurrent(false)) return
    setActiveId(''); applyDraft(t('profile.new'), template, true); setShowSystem(false)
  }

  async function save(): Promise<void> {
    await persistCurrent(true)
  }

  async function persistCurrent(announce: boolean): Promise<boolean> {
    if (busy) return false
    if (!name.trim()) {
      failMessage('notice.nameRequired')
      return false
    }
    setBusy(true)
    try {
      const revision = revisionRef.current
      const snapshot = { name, content }
      const now = Date.now()
      const saved = await localDataApi.saveHostProfile({ id: active?.id ?? crypto.randomUUID(), name: snapshot.name.trim(), content: snapshot.content, createdAt: active?.createdAt ?? now, updatedAt: now })
      setProfiles((items) => [saved, ...items.filter((item) => item.id !== saved.id)])
      setActiveId(saved.id)
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setName(saved.name)
        setContent(saved.content)
      }
      if (announce) {
        succeed(unchanged ? 'notice.saved' : 'notice.savedPending')
        recordOperation(t('operation.save'), saved.name, 'success')
      } else {
        succeed(unchanged ? 'notice.autoSaved' : 'notice.autoSavedPending')
      }
      return true
    } catch (cause) {
      fail(cause)
      return false
    } finally { setBusy(false) }
  }

  async function remove(): Promise<void> {
    if (!active || busy || !await dialog.confirm(t('confirm.delete', { name: active.name }), { dangerous: true })) return
    try {
      await localDataApi.deleteHostProfile(active.id)
      const next = profiles.filter((item) => item.id !== active.id)
      setProfiles(next)
      if (next[0]) {
        setActiveId(next[0].id); applyDraft(next[0].name, next[0].content, false); setShowSystem(false)
      } else {
        setActiveId(''); applyDraft(t('profile.new'), template, true); setShowSystem(false)
      }
      succeed('notice.deleted')
    } catch (cause) { fail(cause) }
  }

  async function applySystem(): Promise<void> {
    if (!system || !await dialog.confirm(t('confirm.apply', { path: system.path }))) return
    setBusy(true)
    try { const updated = await hostApi.writeSystem(content, system.content); setSystem(updated); succeed('notice.systemWritten'); recordOperation(t('operation.writeSystem'), updated.path, 'success') } catch (cause) { fail(cause); recordOperation(t('operation.writeSystem'), cause instanceof Error ? cause.message : String(cause), 'error') } finally { setBusy(false) }
  }

  async function resolve(): Promise<void> {
    try { const result = await hostApi.resolve(dnsHost); setAddresses(result); succeed(result.length ? 'notice.resolved' : 'notice.noAddress', result.length ? { count: result.length } : undefined) } catch (cause) { fail(cause) }
  }

  async function importFile(): Promise<void> {
    if (shouldPersistDraft && !await persistCurrent(false)) return
    try {
      const file = await userFilesApi.pickText()
      if (!file) return
      setActiveId('')
      applyDraft(file.name.replace(/\.[^.]+$/, '') || t('profile.imported'), file.content, false)
      setShowSystem(false)
      succeed('notice.imported', { file: file.name })
    } catch (cause) { fail(cause) }
  }

  async function exportFile(): Promise<void> {
    try {
      const target = await userFilesApi.exportText(`${safeFileName(name || 'hosts')}.txt`, content)
      if (target) succeed('notice.exported', { path: target })
    } catch (cause) { fail(cause) }
  }

  function replaceText(all: boolean): void {
    if (!find) return
    if (all) {
      const count = countTextMatches(content, find)
      if (!count) return failMessage('notice.noMatches')
      updateContent(content.split(find).join(replace))
      succeed('notice.replaced', { count })
      return
    }
    const index = content.indexOf(find)
    if (index < 0) return failMessage('notice.noMatches')
    updateContent(`${content.slice(0, index)}${replace}${content.slice(index + find.length)}`)
    succeed('notice.replaced', { count: 1 })
  }

  function succeed(key: HostMessageKey, values?: MessageValues) { setNotice({ key, values }); setFailed(false) }
  function failMessage(key: HostMessageKey, values?: MessageValues) { setNotice({ key, values }); setFailed(true) }
  function fail(cause: unknown) { setNotice({ raw: cause instanceof Error ? cause.message : String(cause) }); setFailed(true) }
  function applyDraft(nextName: string, nextContent: string, pristine: boolean) { revisionRef.current += 1; setName(nextName); setContent(nextContent); setPristineNew(pristine) }
  function updateName(nextName: string) { revisionRef.current += 1; setName(nextName); setPristineNew(false) }
  function updateContent(nextContent: string) { revisionRef.current += 1; setContent(nextContent); setPristineNew(false) }

  return (
    <main className="utility-workbench host-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <section className="host-toolbar">
        <label>
          <Globe2 />
          <input value={dnsHost} placeholder={t('dns.placeholder')} onChange={(event) => setDnsHost(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') void resolve() }} />
          <button type="button" onClick={() => void resolve()}><Search />{t('action.resolve')}</button>
        </label>
        <div>{addresses.map((address) => <code key={address}>{address}</code>)}</div>
        <button className="secondary-button" type="button" onClick={() => void importFile()}><FileInput />{t('action.import')}</button>
        <button className="secondary-button" type="button" disabled={!content} onClick={() => void exportFile()}><Download />{t('action.export')}</button>
        <button className="secondary-button" type="button" onClick={() => { setShowSystem(!showSystem); if (!system) void refreshSystem() }}><Eye />{t(showSystem ? 'action.editProfile' : 'action.viewSystem')}</button>
        <button className="secondary-button" type="button" onClick={() => void refreshSystem()}><RefreshCw />{t('action.refresh')}</button>
      </section>
      <section className="host-layout">
        <aside>
          <header>
            <label className="host-profile-search"><Search /><input value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} /></label>
            <button type="button" aria-label={t('action.newProfile')} onClick={() => void create()}><Plus /></button>
          </header>
          <div>{visibleProfiles.map((profile) => (
            <button className={profile.id === activeId ? 'host-profile host-profile--active' : 'host-profile'} type="button" key={profile.id} onClick={() => void select(profile)}>
              <strong>{profile.name}</strong>
              <small>{new Date(profile.updatedAt).toLocaleString(locale)}</small>
            </button>
          ))}</div>
        </aside>
        <section className="utility-editor-card host-editor">
          <header>
            <input value={showSystem ? system?.path ?? t('system.title') : name} readOnly={showSystem} onChange={(event) => updateName(event.target.value)} />
            <div>{!showSystem && <>
              <button className="utility-copy" type="button" onClick={() => setFindVisible((value) => !value)}><Search />{t('action.findReplace')}</button>
              <button className="utility-copy" type="button" disabled={!active || busy} onClick={() => void remove()}><Trash2 />{t('action.delete')}</button>
              <button className="utility-copy" type="button" disabled={!dirty || busy} onClick={() => void save()}><Save />{t('action.save')}</button>
              <button className="primary-button" type="button" disabled={!system || busy} onClick={() => void applySystem()}><ServerCog />{t('action.applySystem')}</button>
            </>}</div>
          </header>
          {findVisible && !showSystem && (
            <div className="host-find-replace">
              <input autoFocus value={find} placeholder={t('find.placeholder')} onChange={(event) => setFind(event.target.value)} />
              <input value={replace} placeholder={t('replace.placeholder')} onChange={(event) => setReplace(event.target.value)} />
              <span>{find ? t('find.matches', { count: matchCount }) : ''}</span>
              <button type="button" disabled={!find} onClick={() => replaceText(false)}>{t('action.replace')}</button>
              <button type="button" disabled={!find} onClick={() => replaceText(true)}>{t('action.replaceAll')}</button>
              <button type="button" aria-label={t('action.closeFind')} onClick={() => setFindVisible(false)}><X /></button>
            </div>
          )}
          <CodeEditor ariaLabel={showSystem ? t('system.title') : t('editor.profile')} value={showSystem ? system?.content ?? '' : content} onChange={showSystem ? undefined : updateContent} readOnly={showSystem} className="utility-code-editor" lineWrapping={false} />
        </section>
      </section>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t(system?.writable ? 'footer.writable' : 'footer.mayRequireAdmin')} · {t('footer.storage')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function countTextMatches(value: string, query: string): number {
  if (!query) return 0
  let count = 0
  let from = 0
  while ((from = value.indexOf(query, from)) >= 0) {
    count += 1
    from += Math.max(query.length, 1)
  }
  return count
}

function safeFileName(value: string): string {
  return value.trim().replace(/[\\/:*?"<>|\u0000-\u001f]/g, '-').replace(/^\.+/, '').slice(0, 120) || 'hosts'
}
