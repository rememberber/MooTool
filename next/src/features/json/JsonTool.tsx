import { CheckCircle2, FileJson, XCircle } from 'lucide-react'
import { useEffect, useMemo, useReducer, useRef } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { JsonCodeEditor, type JsonCodeEditorHandle } from './JsonCodeEditor'
import { JsonInspector } from './JsonInspector'
import { JsonToolbar } from './JsonToolbar'
import { JsonToolDialogs, type InputConversion, type OutputDialogState } from './JsonToolDialogs'
import { JsonVaultPanel } from './JsonVaultPanel'
import {
  compressJson,
  escapeJavaString,
  escapeJsonString,
  formatJson,
  formatJsonAdvanced,
  javaBeanToJson,
  jsonToJavaBean,
  jsonToXml,
  queryJsonPath,
  swapJsonKeysAndValues,
  unescapeJsonString,
  unescapeJsonText,
  validateJson,
  xmlToJson,
  type JsonFormatOptions
} from './jsonTools'

const sampleJson = `{
  "name": "MooTool Next",
  "stack": ["Electron", "Vite", "React", "TypeScript"],
  "desktop": {
    "style": "quiet macOS workspace",
    "theme": "light"
  }
}`

type CopyState = 'idle' | 'copied' | 'failed'

type JsonUiState = {
  content: string
  wrap: boolean
  copyState: CopyState
  notice: string
  inspectorOpen: boolean
  findOpen: boolean
  findQuery: string
  historyOpen: boolean
  pathPickerOpen: boolean
  jsonPath: string
  inputConversion: InputConversion | null
  conversionInput: string
  outputDialog: OutputDialogState | null
  className: string
  formatOptions: JsonFormatOptions
}

function updateJsonState(state: JsonUiState, patch: Partial<JsonUiState>): JsonUiState {
  return { ...state, ...patch }
}

async function persistHistory(summary: string, input: string, output: string): Promise<void> {
  try {
    await window.mootool.saveHistory({ funcType: 'json', summary, inputText: input, outputText: output })
  } catch {
    // History persistence should never interrupt the active tool operation.
  }
}

export function JsonTool() {
  const { t } = useI18n()
  const { settings } = useSettings()
  const toast = useToast()
  const editorRef = useRef<JsonCodeEditorHandle>(null)
  const findIndexRef = useRef(0)
  const [state, update] = useReducer(updateJsonState, settings.editor.softWrap, (wrap): JsonUiState => ({
    content: sampleJson,
    wrap,
    copyState: 'idle',
    notice: '',
    inspectorOpen: true,
    findOpen: false,
    findQuery: '',
    historyOpen: false,
    pathPickerOpen: false,
    jsonPath: '$',
    inputConversion: null,
    conversionInput: '',
    outputDialog: null,
    className: 'Root',
    formatOptions: { spaces: 2, sortKeys: false, ignoreCase: false, checkDuplicateKeys: true }
  }))
  const status = useMemo(() => validateJson(state.content, t), [state.content, t])
  const findMatches = useMemo(() => findAll(state.content, state.findQuery), [state.content, state.findQuery])

  useEffect(() => {
    const handleFindShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'f') {
        event.preventDefault()
        update({ findOpen: true })
      }
    }
    window.addEventListener('keydown', handleFindShortcut)
    return () => window.removeEventListener('keydown', handleFindShortcut)
  }, [])

  useEffect(() => {
    const desktopLayout = window.matchMedia('(min-width: 1321px)')
    const syncInspector = () => update({ inspectorOpen: desktopLayout.matches })
    syncInspector()
    desktopLayout.addEventListener('change', syncInspector)
    return () => desktopLayout.removeEventListener('change', syncInspector)
  }, [])

  function showError(error: unknown): void {
    const message = error instanceof Error ? error.message : t('json.notice.failed')
    update({ notice: message })
    toast.error(message)
  }

  async function runTransform(transform: (value: string) => string, success: string, summary = success): Promise<void> {
    try {
      const output = transform(state.content)
      update({ content: output, notice: success })
      toast.success(success)
      await persistHistory(summary, state.content, output)
    } catch (error) {
      showError(error)
    }
  }

  async function showOutput(title: string, transform: (value: string) => string): Promise<void> {
    try {
      const output = transform(state.content)
      update({ outputDialog: { title, value: output }, notice: title })
      await persistHistory(title, state.content, output)
    } catch (error) {
      showError(error)
    }
  }

  async function copyValue(value: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(value)
      update({ copyState: 'copied', notice: t('json.notice.copied') })
      toast.success(t('json.notice.copied'))
    } catch {
      update({ copyState: 'failed', notice: t('json.notice.copyFailed') })
      toast.error(t('json.notice.copyFailed'))
    }
    window.setTimeout(() => update({ copyState: 'idle' }), 1400)
  }

  async function importFile(): Promise<void> {
    try {
      const file = await window.mootool.openTextFile('json')
      if (!file) return
      update({ content: file.content, notice: t('json.notice.imported') })
      toast.success(t('json.notice.imported'))
    } catch (error) {
      showError(error)
    }
  }

  async function exportFile(): Promise<void> {
    try {
      const path = await window.mootool.saveTextFile({ kind: 'json', defaultName: 'mootool.json', content: state.content })
      if (!path) return
      update({ notice: t('json.notice.exported') })
      toast.success(t('json.notice.exported'))
    } catch (error) {
      showError(error)
    }
  }

  async function runInputConversion(): Promise<void> {
    if (!state.inputConversion) return
    try {
      const output = state.inputConversion === 'xmlToJson'
        ? xmlToJson(state.conversionInput, t)
        : javaBeanToJson(state.conversionInput, t)
      const title = state.inputConversion === 'xmlToJson' ? t('json.action.xmlToJson') : t('json.action.beanToJson')
      update({ content: output, inputConversion: null, conversionInput: '', notice: title })
      toast.success(title)
      await persistHistory(title, state.conversionInput, output)
    } catch (error) {
      showError(error)
    }
  }

  function selectNextMatch(): void {
    if (findMatches.length === 0) {
      toast.info(t('json.notice.noMatches'))
      return
    }
    const index = findMatches[findIndexRef.current % findMatches.length]
    findIndexRef.current = (findIndexRef.current + 1) % findMatches.length
    editorRef.current?.selectRange(index, index + state.findQuery.length)
  }

  return (
    <section className="tool-page json-tool">
      <div className="tool-page__header">
        <h1>{t('json.title')}</h1>
        <div className={status.kind === 'valid' ? 'status-pill status-pill--valid' : status.kind === 'error' ? 'status-pill status-pill--error' : 'status-pill'}>
          {status.kind === 'valid' ? <CheckCircle2 size={14} /> : status.kind === 'error' ? <XCircle size={14} /> : <FileJson size={14} />}
          {status.message}
        </div>
      </div>

      <ResizableColumns
        className={state.inspectorOpen ? 'json-layout' : 'json-layout json-layout--editor-only'}
        columns={state.inspectorOpen ? 3 : 2}
        defaultSizes={state.inspectorOpen ? [190, 520, 280] : [190, 800]}
        minPaneWidths={state.inspectorOpen ? [150, 360, 220] : [150, 360]}
        minimumWidth={state.inspectorOpen ? 1100 : 720}
        storageKey={state.inspectorOpen ? 'json-three-pane' : 'json-two-pane'}
      >
        <JsonVaultPanel content={state.content} onOpen={(content) => update({ content, notice: '' })} />
        <div className="editor-shell">
          <JsonToolbar
            wrap={state.wrap}
            copied={state.copyState === 'copied'}
            findOpen={state.findOpen}
            findQuery={state.findQuery}
            findMatchCount={findMatches.length}
            onFormat={() => { void runTransform((value) => formatJson(value, t, 2), t('json.notice.formatted'), t('json.action.format')) }}
            onCompress={() => { void runTransform((value) => compressJson(value, t), t('json.notice.compressed'), t('json.action.compress')) }}
            onToggleWrap={() => update({ wrap: !state.wrap })}
            onCopy={() => { void copyValue(state.content) }}
            onToggleFind={() => update({ findOpen: !state.findOpen })}
            onImport={() => { void importFile() }}
            onExport={() => { void exportFile() }}
            onHistory={() => update({ historyOpen: true })}
            onToggleInspector={() => update({ inspectorOpen: !state.inspectorOpen })}
            onClear={() => update({ content: '' })}
            onFindQueryChange={(findQuery) => { findIndexRef.current = 0; update({ findQuery }) }}
            onNextMatch={selectNextMatch}
            onCloseFind={() => update({ findOpen: false })}
          />
          <JsonCodeEditor
            ref={editorRef}
            value={state.content}
            wrap={state.wrap}
            fontSize={settings.editor.jsonFontSize}
            searchQuery={state.findOpen ? state.findQuery : ''}
            ariaLabel={t('json.editor.label')}
            onChange={(content) => update({ content, notice: '', copyState: 'idle' })}
          />
        </div>

        {state.inspectorOpen && (
          <JsonInspector
            formatOptions={state.formatOptions}
            className={state.className}
            jsonPath={state.jsonPath}
            notice={state.notice}
            status={status}
            onFormatOptionsChange={(formatOptions) => update({ formatOptions })}
            onAdvancedFormat={() => { void runTransform((value) => formatJsonAdvanced(value, t, state.formatOptions), t('json.notice.formatted'), t('json.format.apply')) }}
            onJsonToXml={() => { void showOutput(t('json.action.jsonToXml'), (value) => jsonToXml(value, t)) }}
            onXmlToJson={() => update({ inputConversion: 'xmlToJson', conversionInput: '' })}
            onBeanToJson={() => update({ inputConversion: 'beanToJson', conversionInput: '' })}
            onJsonToBean={() => { void showOutput(t('json.action.jsonToBean'), (value) => jsonToJavaBean(value, t, state.className)) }}
            onSwap={() => { void runTransform((value) => swapJsonKeysAndValues(value, t), t('json.action.swap')) }}
            onEscapeJson={() => { void runTransform(escapeJsonString, t('json.notice.escaped'), t('json.action.escape')) }}
            onUnescapeJson={() => { void runTransform((value) => unescapeJsonString(value, t), t('json.notice.unescaped'), t('json.action.unescape')) }}
            onEscapeText={() => { void runTransform(escapeJavaString, t('json.action.escapeText')) }}
            onUnescapeText={() => { void runTransform(unescapeJsonText, t('json.action.unescapeText')) }}
            onClassNameChange={(className) => update({ className })}
            onJsonPathChange={(jsonPath) => update({ jsonPath })}
            onQueryPath={() => { void showOutput(t('json.panel.jsonPath'), (value) => queryJsonPath(value, state.jsonPath, t)) }}
            onOpenPathPicker={() => update({ pathPickerOpen: true })}
            onClose={() => update({ inspectorOpen: false })}
          />
        )}
      </ResizableColumns>

      <JsonToolDialogs
        content={state.content}
        historyOpen={state.historyOpen}
        pathPickerOpen={state.pathPickerOpen}
        inputConversion={state.inputConversion}
        conversionInput={state.conversionInput}
        outputDialog={state.outputDialog}
        onCloseHistory={() => update({ historyOpen: false })}
        onApplyHistory={(content) => update({ content, historyOpen: false })}
        onClosePathPicker={() => update({ pathPickerOpen: false })}
        onChoosePath={(jsonPath) => update({ jsonPath, pathPickerOpen: false, notice: t('json.notice.pathApplied') })}
        onCloseInput={() => update({ inputConversion: null })}
        onConversionInputChange={(conversionInput) => update({ conversionInput })}
        onRunInputConversion={() => { void runInputConversion() }}
        onCloseOutput={() => update({ outputDialog: null })}
        onCopyOutput={() => { if (state.outputDialog) void copyValue(state.outputDialog.value) }}
        onUseOutput={() => update({ content: state.outputDialog?.value ?? state.content, outputDialog: null })}
      />
    </section>
  )
}

function findAll(content: string, query: string): number[] {
  if (!query) return []
  const escapedQuery = query.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return Array.from(content.matchAll(new RegExp(escapedQuery, 'giu')), (match) => match.index)
}
