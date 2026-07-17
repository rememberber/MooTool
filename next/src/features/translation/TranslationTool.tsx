import { ArrowLeftRight, Copy, History, Languages, Plus, Save, Search, Star, Trash2, X } from 'lucide-react'
import { useCallback, useEffect, useRef, useState } from 'react'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { translationLanguageCodes, type TranslationHistory, type TranslationProvider, type TranslationWord } from '@/shared/contracts/network'
import { useSettings } from '@/features/settings/SettingsProvider'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'

type TranslationTab = 'translate' | 'words' | 'history'

export function TranslationTool() {
  const { t } = useI18n()
  const { settings, updateSettings } = useSettings()
  const actions = useToolActions('translation')
  const [tab, setTab] = useState<TranslationTab>('translate')
  const [source, setSource] = useState('')
  const [target, setTarget] = useState('')
  const [providerUsed, setProviderUsed] = useState<TranslationProvider | null>(null)
  const [fallbackUsed, setFallbackUsed] = useState(false)
  const [translating, setTranslating] = useState(false)
  const requestId = useRef('')
  const requestSequence = useRef(0)
  const restoredSource = useRef<string | null>(null)
  const reportError = useRef(actions.reportError)
  reportError.current = actions.reportError

  const cancelActiveTranslation = useCallback(() => {
    requestSequence.current += 1
    const activeRequestId = requestId.current
    requestId.current = ''
    if (activeRequestId) void window.mootool.cancelNetworkRequest(activeRequestId)
  }, [])

  const translate = useCallback(async (text: string) => {
    const sequence = ++requestSequence.current
    const previousRequestId = requestId.current
    requestId.current = ''
    if (!text.trim()) {
      if (previousRequestId) void window.mootool.cancelNetworkRequest(previousRequestId)
      setTarget('')
      setProviderUsed(null)
      setFallbackUsed(false)
      setTranslating(false)
      return
    }
    if (previousRequestId) await window.mootool.cancelNetworkRequest(previousRequestId)
    if (sequence !== requestSequence.current) return
    const currentRequestId = `translation-${Date.now()}-${Math.random().toString(36).slice(2)}`
    requestId.current = currentRequestId
    setTranslating(true)
    try {
      const result = await window.mootool.translate({
        requestId: currentRequestId,
        text,
        sourceLang: settings.tools.translationSourceLang,
        targetLang: settings.tools.translationTargetLang,
        preferredProvider: settings.tools.translationProvider,
        timeoutMs: settings.network.translationTimeoutMs
      })
      if (sequence === requestSequence.current && requestId.current === currentRequestId) {
        setTarget(result.text)
        setProviderUsed(result.provider)
        setFallbackUsed(result.fallbackUsed)
      }
    } catch (error) {
      if (sequence === requestSequence.current && !String(error).includes('ABORTED')) reportError.current(error)
    } finally {
      if (sequence === requestSequence.current && requestId.current === currentRequestId) {
        requestId.current = ''
        setTranslating(false)
      }
    }
  }, [settings.network.translationTimeoutMs, settings.tools.translationProvider, settings.tools.translationSourceLang, settings.tools.translationTargetLang])

  useEffect(() => {
    if (restoredSource.current === source) return
    const timer = window.setTimeout(() => { void translate(source) }, 500)
    return () => {
      window.clearTimeout(timer)
      cancelActiveTranslation()
    }
  }, [cancelActiveTranslation, source, translate])

  async function saveWord(): Promise<void> {
    if (!source.trim()) return
    try {
      await window.mootool.saveTranslationWord({ sourceText: source, targetText: target, sourceLang: settings.tools.translationSourceLang, targetLang: settings.tools.translationTargetLang, remark: '' })
      actions.toast.success(t('translation.savedWord'))
    } catch (error) { actions.reportError(error) }
  }

  function prepareForRetranslation(): void {
    restoredSource.current = null
    cancelActiveTranslation()
    setTranslating(false)
  }

  function changeSourceLanguage(value: string): void {
    prepareForRetranslation()
    const targetLang = value !== 'auto' && value === settings.tools.translationTargetLang
      ? alternateTargetLanguage(value)
      : settings.tools.translationTargetLang
    void updateSettings({ tools: { translationSourceLang: value, translationTargetLang: targetLang } })
  }

  function changeTargetLanguage(value: string): void {
    prepareForRetranslation()
    const targetLang = value === settings.tools.translationSourceLang ? alternateTargetLanguage(value) : value
    void updateSettings({ tools: { translationTargetLang: targetLang } })
  }

  function changeProvider(value: TranslationProvider): void {
    prepareForRetranslation()
    void updateSettings({ tools: { translationProvider: value } })
  }

  function changeSource(value: string): void {
    prepareForRetranslation()
    setSource(value)
  }

  function clear(): void {
    prepareForRetranslation()
    setSource('')
    setTarget('')
    setProviderUsed(null)
    setFallbackUsed(false)
  }

  function exchange(): void {
    const sourceCode = settings.tools.translationSourceLang
    const targetCode = settings.tools.translationTargetLang
    cancelActiveTranslation()
    setTranslating(false)
    void updateSettings({ tools: { translationSourceLang: targetCode, translationTargetLang: sourceCode === 'auto' ? 'en' : sourceCode } })
    restoredSource.current = null
    setSource(target)
    setTarget(source)
  }

  function restore(sourceText: string, targetText: string): void {
    cancelActiveTranslation()
    setTranslating(false)
    restoredSource.current = sourceText
    setSource(sourceText)
    setTarget(targetText)
  }

  return (
    <section className="tool-page p5-tool translation-tool-page">
      <ToolPageHeader title={t('translation.title')} />
      <div className="local-tool-shell translation-workspace">
        <ToolTabs tabs={(['translate', 'words', 'history'] as TranslationTab[]).map((id) => ({ id, label: t(`translation.tab.${id}` as 'translation.tab.translate') }))} active={tab} onChange={setTab} />
        {tab === 'translate' && <div className="translation-main">
          <div className="translation-toolbar"><LanguageSelect value={settings.tools.translationSourceLang} includeAuto onChange={changeSourceLanguage} /><button className="icon-button" type="button" aria-label={t('translation.exchange')} onClick={exchange}><ArrowLeftRight size={14} /></button><LanguageSelect value={settings.tools.translationTargetLang} onChange={changeTargetLanguage} /><span className="p4-toolbar__spacer" /><label>{t('translation.provider')}<select value={settings.tools.translationProvider} onChange={(event) => changeProvider(event.target.value as TranslationProvider)}><option value="google">Google</option><option value="bing">Bing</option></select></label><button className="icon-button" type="button" aria-label={t('translation.copy')} disabled={!target} onClick={() => { void actions.copy(target) }}><Copy size={14} /></button><button className="icon-button" type="button" aria-label={t('translation.saveWord')} disabled={!source} onClick={() => { void saveWord() }}><Star size={14} /></button><button className="icon-button" type="button" aria-label={t('common.action.clear')} onClick={clear}><X size={14} /></button></div>
          <ResizableColumns className="translation-editor-grid" columns={2} defaultSizes={[1, 1]} minPaneWidths={[280, 280]} storageKey="translation-editor"><TextCodeEditor className="translation-source-editor" testId="translation-source" ariaLabel={t('translation.sourcePlaceholder')} value={source} placeholder={t('translation.sourcePlaceholder')} onChange={changeSource} /><div className="translation-result"><TextCodeEditor className="translation-target-editor" testId="translation-result" ariaLabel={t('translation.targetPlaceholder')} value={translating ? t('translation.translating') : target} readOnly /><footer>{providerUsed && <span><Languages size={13} />{providerUsed === 'google' ? 'Google' : 'Bing'}{fallbackUsed ? ` · ${t('translation.fallback')}` : ''}</span>}<span>{source.length} / 50000</span></footer></div></ResizableColumns>
        </div>}
        {tab === 'words' && <WordBook onApply={(word) => { restore(word.sourceText, word.targetText); void updateSettings({ tools: { translationSourceLang: word.sourceLang, translationTargetLang: word.targetLang } }); setTab('translate') }} onRetranslate={(word) => translateWord(word, settings, actions.reportError)} />}
        {tab === 'history' && <TranslationHistoryPanel onApply={(item) => { restore(item.sourceText, item.targetText); void updateSettings({ tools: { translationSourceLang: item.sourceLang, translationTargetLang: item.targetLang } }); setTab('translate') }} />}
      </div>
    </section>
  )
}

function LanguageSelect({ value, includeAuto = false, onChange }: { value: string; includeAuto?: boolean; onChange: (value: string) => void }) {
  const { t } = useI18n()
  return <select aria-label={includeAuto ? t('translation.sourceLanguage') : t('translation.targetLanguage')} value={value} onChange={(event) => onChange(event.target.value)}>{translationLanguageCodes.map((code) => includeAuto || code !== 'auto' ? <option value={code} key={code}>{t(`translation.lang.${code}` as MessageKey)}</option> : null)}</select>
}

function WordBook({ onApply, onRetranslate }: { onApply: (word: TranslationWord) => void; onRetranslate: (word: TranslationWord) => Promise<TranslationWord> }) {
  const { t } = useI18n()
  const actions = useToolActions('translation')
  const [query, setQuery] = useState('')
  const [items, setItems] = useState<TranslationWord[]>([])
  const [selected, setSelected] = useState<TranslationWord | null>(null)
  const load = useCallback(async () => { const next = await window.mootool.listTranslationWords(query); setItems(next); setSelected((current) => current ? next.find((item) => item.id === current.id) ?? null : next[0] ?? null) }, [query])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 100); return () => clearTimeout(timer) }, [load])
  async function save(): Promise<void> { if (!selected) return; try { setSelected(await window.mootool.saveTranslationWord(selected)); await load(); actions.toast.success(t('common.saved')) } catch (error) { actions.reportError(error) } }
  async function remove(): Promise<void> { if (!selected || !window.confirm(t('translation.confirmDeleteWord'))) return; await window.mootool.deleteTranslationWord(selected.id); setSelected(null); await load() }
  return <ResizableColumns className="translation-record-layout" columns={2} defaultSizes={[220, 780]} minPaneWidths={[170, 360]} storageKey="translation-words"><aside><div className="compact-search"><Search size={13} /><input aria-label={t('translation.searchWords')} value={query} placeholder={t('translation.searchWords')} onChange={(event) => setQuery(event.target.value)} /></div><div className="translation-record-list">{items.map((item) => <button className={item.id === selected?.id ? 'translation-record translation-record--active' : 'translation-record'} type="button" key={item.id} onClick={() => setSelected(item)}><strong>{item.sourceText}</strong><span>{item.targetText}</span></button>)}</div><footer><button className="icon-button" type="button" aria-label={t('common.add')} onClick={() => setSelected({ id: 0, sourceText: '', targetText: '', sourceLang: 'auto', targetLang: 'zh-CN', remark: '', createTime: '', modifiedTime: '' })}><Plus size={14} /></button><button className="icon-button icon-button--danger" type="button" disabled={!selected?.id} aria-label={t('common.action.delete')} onClick={() => { void remove() }}><Trash2 size={14} /></button></footer></aside><main>{selected ? <><header><span>{languageLabel(selected.sourceLang, t)} → {languageLabel(selected.targetLang, t)}</span><div><button className="toolbar-button" type="button" onClick={() => onApply(selected)}><Languages size={14} />{t('translation.apply')}</button><button className="toolbar-button" type="button" onClick={() => { void onRetranslate(selected).then((word) => { setSelected(word); void load() }) }}><History size={14} />{t('translation.retranslate')}</button></div></header><TextCodeEditor className="translation-record-editor" ariaLabel={t('translation.sourcePlaceholder')} value={selected.sourceText} placeholder={t('translation.sourcePlaceholder')} onChange={(sourceText) => setSelected({ ...selected, sourceText })} /><TextCodeEditor className="translation-record-editor" ariaLabel={t('translation.targetPlaceholder')} value={selected.targetText} placeholder={t('translation.targetPlaceholder')} onChange={(targetText) => setSelected({ ...selected, targetText })} /><input aria-label={t('translation.remark')} value={selected.remark} placeholder={t('translation.remark')} onChange={(event) => setSelected({ ...selected, remark: event.target.value })} /><button className="dialog-button" type="button" onClick={() => { void save() }}><Save size={14} />{t('common.save')}</button></> : <div className="history-empty">{t('translation.wordEmpty')}</div>}</main></ResizableColumns>
}

function TranslationHistoryPanel({ onApply }: { onApply: (item: TranslationHistory) => void }) {
  const { t } = useI18n()
  const [query, setQuery] = useState('')
  const [items, setItems] = useState<TranslationHistory[]>([])
  const load = useCallback(async () => setItems(await window.mootool.listTranslationHistory(query)), [query])
  useEffect(() => { const timer = window.setTimeout(() => { void load() }, 100); return () => clearTimeout(timer) }, [load])
  return <div className="translation-history"><div className="compact-search"><Search size={13} /><input aria-label={t('translation.searchHistory')} value={query} placeholder={t('translation.searchHistory')} onChange={(event) => setQuery(event.target.value)} /></div><div className="translation-history-list">{items.length === 0 ? <div className="history-empty">{t('history.empty')}</div> : items.map((item) => <article key={item.id}><button type="button" onClick={() => onApply(item)}><header><strong>{languageLabel(item.sourceLang, t)} → {languageLabel(item.targetLang, t)}</strong><span>{item.translatorType} · {item.createTime}</span></header><p>{item.sourceText}</p><p>{item.targetText}</p></button><button className="icon-button" type="button" aria-label={t('history.delete')} onClick={() => { void window.mootool.deleteTranslationHistory(item.id).then(load) }}><Trash2 size={13} /></button></article>)}</div><footer><button className="dialog-button dialog-button--danger" type="button" disabled={!items.length} onClick={() => { if (window.confirm(t('history.confirmClear'))) void window.mootool.clearTranslationHistory().then(load) }}><Trash2 size={14} />{t('history.clearAll')}</button></footer></div>
}

async function translateWord(word: TranslationWord, settings: ReturnType<typeof useSettings>['settings'], reportError: (error: unknown) => void): Promise<TranslationWord> {
  try {
    const result = await window.mootool.translate({ requestId: `word-${Date.now()}`, text: word.sourceText, sourceLang: word.sourceLang, targetLang: word.targetLang, preferredProvider: settings.tools.translationProvider, timeoutMs: settings.network.translationTimeoutMs })
    return window.mootool.saveTranslationWord({ ...word, targetText: result.text })
  } catch (error) { reportError(error); return word }
}

function languageLabel(code: string, t: (key: MessageKey) => string): string {
  return t(`translation.lang.${code}` as MessageKey)
}

function alternateTargetLanguage(code: string): string {
  return code === 'zh-CN' ? 'en' : 'zh-CN'
}
