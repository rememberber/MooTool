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
import { credentialApi } from '../../platform/api/credentialApi'
import { localDataApi } from '../../platform/api/localDataApi'
import { settingsApi } from '../../platform/api/settingsApi'
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
  providerLabel,
  supportsTranslationLanguage,
  translationLanguages
} from './translationTools'
import { formatTranslationName, namingFormats, splitIdentifiers, type NamingFormat } from './translationNaming'
import { translationMessages } from './translationMessages'

type TranslationTab = 'translate' | 'words' | 'history'
type TranslationMessageKey = LocalizedMessageKey<typeof translationMessages>
type TranslationNotice = { key: TranslationMessageKey; values?: MessageValues } | { raw: string }

export function TranslationSurface() {
  const { settings, ready: settingsReady, save: saveSettings } = useSettings()
  const { t, locale } = useLocalizedMessages(translationMessages)
  const [tab, setTab] = useState<TranslationTab>('translate')
  const [source, setSource] = useState('')
  const [target, setTarget] = useState('')
  const [sourceLang, setSourceLang] = useState('auto')
  const [targetLang, setTargetLang] = useState('zh-CN')
  const [provider, setProvider] = useState<TranslationProvider>('google')
  const [namingFormat, setNamingFormat] = useState<NamingFormat>('camelCase')
  const [providerUsed, setProviderUsed] = useState<TranslationProvider>()
  const [fallbackUsed, setFallbackUsed] = useState(false)
  const [translating, setTranslating] = useState(false)
  const [notice, setNotice] = useState<TranslationNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const activeRequest = useRef('')
  const requestSequence = useRef(0)
  const suppressNextAutomaticTranslation = useRef(false)
  const hydratedDefaults = useRef(false)
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
  useEffect(() => {
    if (!settingsReady || hydratedDefaults.current) return
    hydratedDefaults.current = true
    if (translationLanguages.some((code) => code === settings.tools.translationSourceLang)) setSourceLang(settings.tools.translationSourceLang)
    if (translationLanguages.some((code) => code === settings.tools.translationTargetLang) && settings.tools.translationTargetLang !== 'auto') setTargetLang(settings.tools.translationTargetLang)
    setProvider(settings.tools.translationProvider)
  }, [settings.tools.translationProvider, settings.tools.translationSourceLang, settings.tools.translationTargetLang, settingsReady])
  useOperationRestore('translation', (entry) => {
    const metadata = parseOperationMetadata(entry)
    suppressNextAutomaticTranslation.current = true
    setSource(entry.inputText)
    setTarget(entry.outputText)
    if (typeof metadata.sourceLang === 'string') setSourceLang(metadata.sourceLang)
    if (typeof metadata.targetLang === 'string') setTargetLang(metadata.targetLang)
    if (metadata.provider === 'google' || metadata.provider === 'bing' || metadata.provider === 'deepl') setProvider(metadata.provider)
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
      if (provider === 'deepl' && !(await credentialApi.getDeepLStatus()).stored) {
        throw new Error('DEEPL_KEY_REQUIRED')
      }
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
      setNotice({ key: result.fallbackUsed ? 'notice.completeFallback' : 'notice.complete', values: { provider: providerLabel(result.provider) } })
      recordOperation(t('operation.translate'), `${languageName(sourceLang)} → ${languageName(targetLang)} · ${value.length} · ${result.provider}`, 'success', {
        inputText: value, outputText: result.text, metadata: { sourceLang, targetLang, provider, providerUsed: result.provider }
      })
    } catch (cause) {
      if (sequence !== requestSequence.current) return
      setFailed(true)
      setNotice({ raw: translationErrorMessage(cause, t) })
      recordOperation(t('operation.translate'), `${languageName(sourceLang)} → ${languageName(targetLang)} · ${translationErrorMessage(cause, t)}`, 'error', {
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
    if (provider === 'deepl') return
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
  }, [cancelActive, provider, source, translate])

  function exchange() {
    cancelActive()
    const nextSourceLang = targetLang
    const nextTargetLang = sourceLang === 'auto' ? 'en' : sourceLang
    setSourceLang(nextSourceLang)
    setTargetLang(nextTargetLang)
    setSource(target)
    setTarget(source)
    persistDefaults({ translationSourceLang: nextSourceLang, translationTargetLang: nextTargetLang })
  }

  function updateProvider(nextProvider: TranslationProvider) {
    let nextSource = sourceLang
    let nextTarget = targetLang
    if (!supportsTranslationLanguage(nextProvider, nextSource, true)) nextSource = 'auto'
    if (!supportsTranslationLanguage(nextProvider, nextTarget, false)) nextTarget = nextSource === 'en' ? 'zh-CN' : 'en'
    if (nextSource !== 'auto' && nextSource === nextTarget) nextTarget = alternateTargetLanguage(nextSource)
    setProvider(nextProvider)
    setSourceLang(nextSource)
    setTargetLang(nextTarget)
    persistDefaults({ translationProvider: nextProvider, translationSourceLang: nextSource, translationTargetLang: nextTarget })
  }

  function updateSourceLanguage(value: string) {
    setSourceLang(value)
    const nextTarget = value !== 'auto' && value === targetLang ? alternateTargetLanguage(value) : targetLang
    if (nextTarget !== targetLang) setTargetLang(nextTarget)
    persistDefaults({ translationSourceLang: value, translationTargetLang: nextTarget })
  }

  function updateTargetLanguage(value: string) {
    const nextTarget = value === sourceLang ? alternateTargetLanguage(value) : value
    setTargetLang(nextTarget)
    persistDefaults({ translationTargetLang: nextTarget })
  }

  function persistDefaults(patch: Partial<typeof settings.tools>) {
    if (!settingsReady) return
    void saveSettings({ ...settings, tools: { ...settings.tools, ...patch } }).catch((cause: unknown) => {
      setFailed(true)
      setNotice({ raw: errorMessage(cause) })
    })
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

  const formattedName = targetLang === 'en' && !translating ? formatTranslationName(target, namingFormat) : ''

  return (
    <main className="utility-workbench translation-workbench">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
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
            <LanguageSelect provider={provider} value={sourceLang} includeAuto onChange={updateSourceLanguage} />
            <button type="button" title={t('action.exchange')} onClick={exchange}><ArrowLeftRight /></button>
            <LanguageSelect provider={provider} value={targetLang} onChange={updateTargetLanguage} />
            <span />
            <label>{t('option.provider')}<select value={provider} onChange={(event) => updateProvider(event.target.value as TranslationProvider)}><option value="google">Google</option><option value="bing">Bing</option><option value="deepl">DeepL</option></select></label>
            <button type="button" disabled={!target} onClick={() => void copyTarget()}><Copy />{t('action.copy')}</button>
            <button type="button" disabled={!source.trim()} onClick={() => void saveCurrentWord()}><Star />{t('action.favorite')}</button>
            <button type="button" onClick={() => { cancelActive(); setSource(''); setTarget('') }}><X />{t('action.clear')}</button>
          </div>
          <div className="translation-assist-toolbar">
            <button className="secondary-button" type="button" disabled={!source.trim() || splitIdentifiers(source) === source} onClick={() => setSource(splitIdentifiers(source))}>{t('action.splitIdentifiers')}</button>
            {provider === 'deepl' && <button className="secondary-button" type="button" disabled={!source.trim() || translating} onClick={() => void translate(source)}>{t('action.translateNow')}</button>}
            <label>{t('action.namingFormat')}<select aria-label={t('action.namingFormat')} value={namingFormat} onChange={(event) => setNamingFormat(event.target.value as NamingFormat)}>{namingFormats.map((format) => <option key={format}>{format}</option>)}</select></label>
            <button className="secondary-button" type="button" disabled={!formattedName} title={formattedName || t('action.namingHint')} onClick={() => { void clipboardApi.writeText(formattedName).then(() => { setFailed(false); setNotice({ key: 'notice.copied' }) }).catch(() => { setFailed(true); setNotice({ key: 'notice.copyFailed' }) }) }}><Copy />{t('action.copyName')}</button>
            {formattedName && <code className="translation-name-preview" title={formattedName}>{formattedName}</code>}
          </div>
          {provider === 'deepl' && <div className="translation-engine-hint"><span>{t('deepl.hint')}</span><button className="secondary-button" type="button" onClick={() => { window.localStorage.setItem('mootool-next-tauri:settings-category', 'tools'); void settingsApi.openWindow() }}>{t('deepl.configure')}</button></div>}
          <div className="translation-editor-grid">
            <section><header><strong>{languageName(sourceLang)}</strong><span>{source.length} / 50000</span></header><CodeEditor ariaLabel={t('aria.source')} value={source} onChange={(value) => setSource(value.slice(0, 50_000))} className="utility-code-editor" lineWrapping /></section>
            <section><header><strong>{languageName(targetLang)}</strong><span>{translating ? t('status.translating') : providerUsed ? `${providerLabel(providerUsed)}${fallbackUsed ? ` · ${t('status.fallback')}` : ''}` : t('status.result')}</span></header><CodeEditor ariaLabel={t('aria.result')} value={translating && !target ? t('status.translating') : target} readOnly className="utility-code-editor" lineWrapping /></section>
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

function LanguageSelect({ provider, value, includeAuto = false, onChange }: { provider: TranslationProvider; value: string; includeAuto?: boolean; onChange: (value: string) => void }) {
  const { t, locale } = useLocalizedMessages(translationMessages)
  return (
    <select aria-label={includeAuto ? t('aria.sourceLanguage') : t('aria.targetLanguage')} value={value} onChange={(event) => onChange(event.target.value)}>
      {translationLanguages.map((code) => includeAuto || code !== 'auto' ? <option key={code} value={code} disabled={!supportsTranslationLanguage(provider, code, includeAuto)}>{languageLabel(code, locale, t('language.auto'))}</option> : null)}
    </select>
  )
}

const deepLErrorCodes = ['DEEPL_KEY_REQUIRED', 'DEEPL_INVALID_KEY', 'DEEPL_QUOTA_EXCEEDED', 'DEEPL_RATE_LIMITED', 'DEEPL_UNSUPPORTED_LANGUAGE', 'DEEPL_EMPTY_RESPONSE'] as const

function translationErrorMessage(cause: unknown, t: (key: TranslationMessageKey) => string): string {
  const message = errorMessage(cause)
  const code = deepLErrorCodes.find((value) => message.includes(value))
  return code ? t(`error.${code}`) : message
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
