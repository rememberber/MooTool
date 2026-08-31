import {
  ArrowLeftRight,
  CheckCircle2,
  Copy,
  Languages,
  Plus,
  Save,
  Search,
  Star,
  Trash2,
  TriangleAlert,
  X
} from 'lucide-react'
import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { localDataApi } from '../../platform/api/localDataApi'
import { translationApi } from '../../platform/api/translationApi'
import type { TranslationHistory, TranslationWord } from '../../platform/contracts/localData'
import type { TranslationProvider } from '../../platform/contracts/translation'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { errorMessage } from '../../shared/errors'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import {
  alternateTargetLanguage,
  includesTranslationQuery,
  languageLabel,
  translationLanguages
} from './translationTools'
import { translationMessages } from './translationMessages'

type TranslationTab = 'translate' | 'words' | 'history'
type TranslationMessageKey = LocalizedMessageKey<typeof translationMessages>
type TranslationNotice = { key: TranslationMessageKey; values?: MessageValues } | { raw: string }

export function TranslationSurface() {
  const { settings } = useSettings()
  const { t, locale } = useLocalizedMessages(translationMessages)
  const [tab, setTab] = useState<TranslationTab>('translate')
  const [source, setSource] = useState('')
  const [target, setTarget] = useState('')
  const [sourceLang, setSourceLang] = useState('auto')
  const [targetLang, setTargetLang] = useState('zh-CN')
  const [provider, setProvider] = useState<TranslationProvider>('google')
  const [providerUsed, setProviderUsed] = useState<TranslationProvider>()
  const [fallbackUsed, setFallbackUsed] = useState(false)
  const [translating, setTranslating] = useState(false)
  const [notice, setNotice] = useState<TranslationNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const activeRequest = useRef('')
  const requestSequence = useRef(0)
  const suppressNextAutomaticTranslation = useRef(false)
  const languageName = useCallback(
    (code: string) => languageLabel(code, locale, t('language.auto')),
    [locale, t]
  )
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)

  const session = useMemo(() => ({
    digest: JSON.stringify({
      source: contentFingerprint(source),
      target: contentFingerprint(target),
      sourceLang,
      targetLang,
      provider,
      tab
    }),
    summary: t('session.summary', { source: languageName(sourceLang), target: languageName(targetLang), count: source.length })
  }), [languageName, provider, source, sourceLang, t, tab, target, targetLang])
  const { sessionId, reportError } = useToolSessionReport('translation', session.digest, session.summary)
  const recordOperation = useOperationHistory('translation')
  useOperationRestore('translation', (entry) => {
    const metadata = parseOperationMetadata(entry)
    suppressNextAutomaticTranslation.current = true
    setSource(entry.inputText)
    setTarget(entry.outputText)
    if (typeof metadata.sourceLang === 'string') setSourceLang(metadata.sourceLang)
    if (typeof metadata.targetLang === 'string') setTargetLang(metadata.targetLang)
    if (metadata.provider === 'google' || metadata.provider === 'bing') setProvider(metadata.provider)
    setTab('translate')
    setFailed(false)
  })

  const cancelActive = useCallback(() => {
    requestSequence.current += 1
    const requestId = activeRequest.current
    activeRequest.current = ''
    if (requestId) void translationApi.cancel(requestId)
  }, [])

  const translate = useCallback(async (value: string) => {
    const trimmed = value.trim()
    if (!trimmed) {
      cancelActive()
      setTarget('')
      setTranslating(false)
      setProviderUsed(undefined)
      return
    }
    cancelActive()
    const sequence = ++requestSequence.current
    const requestId = `translation-${Date.now()}-${crypto.randomUUID()}`
    activeRequest.current = requestId
    setTranslating(true)
    setFailed(false)
    setNotice({ key: 'notice.translating' })
    try {
      const result = await translationApi.translate({
        requestId,
        text: value,
        sourceLang,
        targetLang,
        preferredProvider: provider,
        timeoutMs: settings.network.translationTimeoutSeconds * 1_000
      })
      if (sequence !== requestSequence.current || activeRequest.current !== requestId) return
      setTarget(result.text)
      setProviderUsed(result.provider)
      setFallbackUsed(result.fallbackUsed)
      setNotice({ key: result.fallbackUsed ? 'notice.completeFallback' : 'notice.complete', values: { provider: result.provider === 'google' ? 'Google' : 'Bing' } })
      recordOperation(t('operation.translate'), `${languageName(sourceLang)} → ${languageName(targetLang)} · ${value.length} · ${result.provider}`, 'success', {
        inputText: value, outputText: result.text, metadata: { sourceLang, targetLang, provider, providerUsed: result.provider }
      })
    } catch (cause) {
      if (sequence !== requestSequence.current) return
      setFailed(true)
      setNotice({ raw: errorMessage(cause) })
      recordOperation(t('operation.translate'), `${languageName(sourceLang)} → ${languageName(targetLang)} · ${errorMessage(cause)}`, 'error', {
        inputText: value, metadata: { sourceLang, targetLang, provider }
      })
    } finally {
      if (sequence === requestSequence.current) {
        activeRequest.current = ''
        setTranslating(false)
      }
    }
  }, [cancelActive, languageName, provider, recordOperation, settings.network.translationTimeoutSeconds, sourceLang, t, targetLang])

  useEffect(() => {
    if (suppressNextAutomaticTranslation.current) {
      suppressNextAutomaticTranslation.current = false
      return
    }
    if (!source.trim()) {
      setTarget('')
      return
    }
    const timer = window.setTimeout(() => void translate(source), 650)
    return () => {
      window.clearTimeout(timer)
      cancelActive()
    }
  }, [cancelActive, source, translate])

  function exchange() {
    cancelActive()
    const nextSourceLang = targetLang
    const nextTargetLang = sourceLang === 'auto' ? 'en' : sourceLang
    setSourceLang(nextSourceLang)
    setTargetLang(nextTargetLang)
    setSource(target)
    setTarget(source)
  }

  function updateSourceLanguage(value: string) {
    setSourceLang(value)
    if (value !== 'auto' && value === targetLang) setTargetLang(alternateTargetLanguage(value))
  }

  function updateTargetLanguage(value: string) {
    setTargetLang(value === sourceLang ? alternateTargetLanguage(value) : value)
  }

  async function copyTarget() {
    try {
      await clipboardApi.writeText(target)
      setFailed(false)
      setNotice({ key: 'notice.copied' })
    } catch {
      setFailed(true)
      setNotice({ key: 'notice.copyFailed' })
    }
  }

  async function saveCurrentWord() {
    if (!source.trim()) return
    const now = Date.now()
    try {
      await localDataApi.saveTranslationWord({
        id: crypto.randomUUID(),
        sourceText: source,
        targetText: target,
        sourceLang,
        targetLang,
        remark: '',
        createdAt: now,
        updatedAt: now
      })
      setFailed(false)
      setNotice({ key: 'notice.savedWord' })
    } catch (cause) {
      setFailed(true)
      setNotice({ raw: errorMessage(cause) })
    }
  }

  function applyRecord(item: TranslationWord | TranslationHistory) {
    cancelActive()
    suppressNextAutomaticTranslation.current = true
    setSource(item.sourceText)
    setTarget(item.targetText)
    setSourceLang(item.sourceLang)
    setTargetLang(item.targetLang)
    setTab('translate')
  }

  return (
    <main className="utility-workbench translation-workbench">
      <header className="utility-header">
        <div><span className="eyebrow">TAURI RUST TRANSLATION</span><h1>{t('title')}</h1></div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <nav className="utility-segments translation-tabs">
        {([['translate', 'tab.translate'], ['words', 'tab.words'], ['history', 'tab.history']] as const).map(([id, label]) => (
          <button key={id} className={tab === id ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setTab(id)}>{t(label)}</button>
        ))}
      </nav>
      {tab === 'translate' && (
        <section className="translation-main">
          <div className="translation-toolbar">
            <LanguageSelect value={sourceLang} includeAuto onChange={updateSourceLanguage} />
            <button type="button" title={t('action.exchange')} onClick={exchange}><ArrowLeftRight /></button>
            <LanguageSelect value={targetLang} onChange={updateTargetLanguage} />
            <span />
            <label>{t('option.provider')}<select value={provider} onChange={(event) => setProvider(event.target.value as TranslationProvider)}><option value="google">Google</option><option value="bing">Bing</option></select></label>
            <button type="button" disabled={!target} onClick={() => void copyTarget()}><Copy />{t('action.copy')}</button>
            <button type="button" disabled={!source.trim()} onClick={() => void saveCurrentWord()}><Star />{t('action.favorite')}</button>
            <button type="button" onClick={() => { cancelActive(); setSource(''); setTarget('') }}><X />{t('action.clear')}</button>
          </div>
          <div className="translation-editor-grid">
            <section><header><strong>{languageName(sourceLang)}</strong><span>{source.length} / 50000</span></header><CodeEditor ariaLabel={t('aria.source')} value={source} onChange={(value) => setSource(value.slice(0, 50_000))} className="utility-code-editor" lineWrapping /></section>
            <section><header><strong>{languageName(targetLang)}</strong><span>{translating ? t('status.translating') : providerUsed ? `${providerUsed === 'google' ? 'Google' : 'Bing'}${fallbackUsed ? ` · ${t('status.fallback')}` : ''}` : t('status.result')}</span></header><CodeEditor ariaLabel={t('aria.result')} value={translating && !target ? t('status.translating') : target} readOnly className="utility-code-editor" lineWrapping /></section>
          </div>
        </section>
      )}
      {tab === 'words' && <WordBook onApply={applyRecord} />}
      {tab === 'history' && <HistoryPanel onApply={applyRecord} />}
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function LanguageSelect({ value, includeAuto = false, onChange }: { value: string; includeAuto?: boolean; onChange: (value: string) => void }) {
  const { t, locale } = useLocalizedMessages(translationMessages)
  return (
    <select aria-label={includeAuto ? t('aria.sourceLanguage') : t('aria.targetLanguage')} value={value} onChange={(event) => onChange(event.target.value)}>
      {translationLanguages.map((code) => includeAuto || code !== 'auto' ? <option key={code} value={code}>{languageLabel(code, locale, t('language.auto'))}</option> : null)}
    </select>
  )
}

function WordBook({ onApply }: { onApply: (word: TranslationWord) => void }) {
  const { t, locale } = useLocalizedMessages(translationMessages)
  const dialog = useDesktopDialog()
  const [items, setItems] = useState<TranslationWord[]>([])
  const [query, setQuery] = useState('')
  const [selected, setSelected] = useState<TranslationWord>()
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    try {
      const next = await localDataApi.listTranslationWords()
      setItems(next)
      setSelected((current) => current ? next.find((item) => item.id === current.id) ?? current : next[0])
    } catch (cause) {
      setError(errorMessage(cause))
    }
  }, [])
  useEffect(() => { void load() }, [load])
  const visible = items.filter((item) => includesTranslationQuery(item, query))

  function add() {
    const now = Date.now()
    setSelected({ id: crypto.randomUUID(), sourceText: '', targetText: '', sourceLang: 'auto', targetLang: 'zh-CN', remark: '', createdAt: now, updatedAt: now })
  }

  async function save() {
    if (!selected?.sourceText.trim()) return
    try {
      const saved = await localDataApi.saveTranslationWord({ ...selected, updatedAt: Date.now() })
      setSelected(saved)
      await load()
    } catch (cause) { setError(errorMessage(cause)) }
  }

  async function remove() {
    if (!selected || !await dialog.confirm(t('confirm.deleteWord'), { dangerous: true })) return
    await localDataApi.deleteTranslationWord(selected.id)
    setSelected(undefined)
    await load()
  }

  return (
    <section className="translation-record-layout">
      <aside><label className="translation-search"><Search /><input value={query} placeholder={t('search.words')} onChange={(event) => setQuery(event.target.value)} /></label><div>{visible.map((item) => <button className={selected?.id === item.id ? 'translation-record translation-record--active' : 'translation-record'} type="button" key={item.id} onClick={() => setSelected(item)}><strong>{item.sourceText}</strong><span>{item.targetText}</span></button>)}</div><footer><button type="button" onClick={add}><Plus />{t('action.new')}</button><button type="button" disabled={!selected} onClick={() => void remove()}><Trash2 />{t('action.delete')}</button></footer></aside>
      <section>{selected ? <><header><strong>{languageLabel(selected.sourceLang, locale, t('language.auto'))} → {languageLabel(selected.targetLang, locale, t('language.auto'))}</strong><button type="button" onClick={() => onApply(selected)}><Languages />{t('action.apply')}</button></header><textarea value={selected.sourceText} placeholder={t('field.source')} onChange={(event) => setSelected({ ...selected, sourceText: event.target.value })} /><textarea value={selected.targetText} placeholder={t('field.target')} onChange={(event) => setSelected({ ...selected, targetText: event.target.value })} /><input value={selected.remark} placeholder={t('field.remark')} onChange={(event) => setSelected({ ...selected, remark: event.target.value })} /><button className="primary-button" type="button" onClick={() => void save()}><Save />{t('action.save')}</button></> : <div className="translation-empty">{t('words.empty')}</div>}{error && <p className="translation-error">{error}</p>}</section>
    </section>
  )
}

function HistoryPanel({ onApply }: { onApply: (item: TranslationHistory) => void }) {
  const { t, locale } = useLocalizedMessages(translationMessages)
  const dialog = useDesktopDialog()
  const [items, setItems] = useState<TranslationHistory[]>([])
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')
  const load = useCallback(async () => {
    try { setItems(await localDataApi.listTranslationHistory()) } catch (cause) { setError(errorMessage(cause)) }
  }, [])
  useEffect(() => { void load() }, [load])
  const visible = items.filter((item) => includesTranslationQuery(item, query))
  async function clearHistory() {
    if (await dialog.confirm(t('confirm.clearHistory'), { dangerous: true })) await localDataApi.clearTranslationHistory().then(load)
  }
  return (
    <section className="translation-history-panel">
      <header><label className="translation-search"><Search /><input value={query} placeholder={t('search.history')} onChange={(event) => setQuery(event.target.value)} /></label><button type="button" disabled={!items.length} onClick={() => void clearHistory()}><Trash2 />{t('action.clearAll')}</button></header>
      <div>{visible.length ? visible.map((item) => <article key={item.id}><button type="button" onClick={() => onApply(item)}><header><strong>{languageLabel(item.sourceLang, locale, t('language.auto'))} → {languageLabel(item.targetLang, locale, t('language.auto'))}</strong><span>{item.provider === 'google' ? 'Google' : 'Bing'} · {new Date(item.createdAt).toLocaleString(locale)}</span></header><p>{item.sourceText}</p><p>{item.targetText}</p></button><button type="button" title={t('action.delete')} onClick={() => void localDataApi.deleteTranslationHistory(item.id).then(load)}><Trash2 /></button></article>) : <div className="translation-empty">{t('history.empty')}</div>}</div>
      {error && <p className="translation-error">{error}</p>}
    </section>
  )
}
