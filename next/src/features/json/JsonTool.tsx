import { CheckCircle2, FileJson, FilePlus2, FolderOpen, Sparkles, XCircle } from 'lucide-react'
import { useCallback, useEffect, useMemo, useReducer, useRef, useState } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import type { CodeEditorViewState } from '@/shared/components/codeEditorViewState'
import {
  defaultFindReplaceOptions,
  findAllMatches,
  findNextMatch,
  findNextMatchWithWrap,
  replaceAllMatches,
  replaceCurrentMatch,
  type FindReplaceOptions
} from '@/shared/components/findReplace'
import { useToolActivity } from '@/shared/components/ToolActivity'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useFocusOnWindowActivate } from '@/shared/hooks/useFocusOnWindowActivate'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { isFormatShortcut } from '@/shared/utils/formatShortcut'
import { cssFontFamily } from '@/shared/fonts/systemFonts'
import { JsonCodeEditor, type JsonCodeEditorHandle } from './JsonCodeEditor'
import { JsonInspector } from './JsonInspector'
import { JsonToolbar } from './JsonToolbar'
import { JsonToolDialogs, type InputConversion, type OutputDialogState } from './JsonToolDialogs'
import { JsonVaultPanel, type JsonVaultPanelHandle } from './JsonVaultPanel'
import type { JsonFormatOptions } from './jsonTools'
import { useJsonValidation } from './useJsonValidation'
import { runTextTask, isTaskCancelled } from '@/shared/workers/runTextTask'
import type { JsonOperation } from '@/shared/workers/textTask'

type CopyState = 'idle' | 'copied' | 'failed'

type JsonUiState = {
  content: string
  filePath: string
  wrap: boolean
  copyState: CopyState
  notice: string
  inspectorOpen: boolean
  findOpen: boolean
  findQuery: string
  replaceText: string
  findOptions: FindReplaceOptions
  replacedCount: number
  historyOpen: boolean
  pathPickerOpen: boolean
  jsonPath: string
  inputConversion: InputConversion | null
  conversionInput: string
  outputDialog: OutputDialogState | null
  className: string
  formatOptions: JsonFormatOptions
}

let jsonSessionState: JsonUiState | null = null
let jsonEditorViewState: CodeEditorViewState | undefined
let jsonFindIndex = 0

function createJsonState(wrap: boolean): JsonUiState {
  if (jsonSessionState) {
    return {
      ...jsonSessionState,
      replaceText: jsonSessionState.replaceText ?? '',
      findOptions: jsonSessionState.findOptions ?? defaultFindReplaceOptions,
      replacedCount: jsonSessionState.replacedCount ?? 0,
      copyState: 'idle',
      formatOptions: { ...jsonSessionState.formatOptions }
    }
  }
  return {
    content: '',
    filePath: '',
    wrap,
    copyState: 'idle',
    notice: '',
    inspectorOpen: false,
    findOpen: false,
    findQuery: '',
    replaceText: '',
    findOptions: defaultFindReplaceOptions,
    replacedCount: 0,
    historyOpen: false,
    pathPickerOpen: false,
    jsonPath: '$',
    inputConversion: null,
    conversionInput: '',
    outputDialog: null,
    className: 'Root',
    formatOptions: { spaces: 2, sortKeys: false, ignoreCase: false, checkDuplicateKeys: true }
  }
}

function updateJsonState(state: JsonUiState, patch: Partial<JsonUiState>): JsonUiState {
  const next = { ...state, ...patch }
  jsonSessionState = next
  return next
}

async function persistHistory(summary: string, input: string, output: string): Promise<void> {
  try {
    await window.mootool.saveHistory({ funcType: 'json', summary, inputText: input, outputText: output })
  } catch {
    // History persistence should never interrupt the active tool operation.
  }
}

export function JsonTool() {
  const toolActive = useToolActivity()
  const { t, language } = useI18n()
  const { settings, updateSettings } = useSettings()
  const toast = useToast()
  const editorRef = useRef<JsonCodeEditorHandle>(null)
  const vaultRef = useRef<JsonVaultPanelHandle>(null)
  const findIndexRef = useRef(jsonFindIndex)
  const [state, update] = useReducer(updateJsonState, settings.editor.softWrap, createJsonState)
  const [dirty, setDirty] = useState(false)
  const status = useJsonValidation(state.content, language, t)
  const currentInput = useRef({ state, language })
  currentInput.current = { state, language }
  const taskController = useRef<AbortController | null>(null)
  const [calculating, setCalculating] = useState(false)
  useEffect(() => () => { taskController.current?.abort() }, [])
  useEffect(() => { taskController.current?.abort() }, [state.content, state.filePath, state.conversionInput, state.inputConversion, language])
  const findMatches = useMemo(
    () => findAllMatches(state.content, state.findQuery, state.findOptions),
    [state.content, state.findOptions, state.findQuery]
  )
  const openVaultContent = useCallback((content: string, filePath: string) => update({ content, filePath, notice: '' }), [])

  useFocusOnWindowActivate(
    () => editorRef.current?.focus(),
    toolActive
      && Boolean(state.filePath)
      && !state.findOpen
      && !state.historyOpen
      && !state.pathPickerOpen
      && !state.inputConversion
      && !state.outputDialog
  )

  useEffect(() => {
    if (!toolActive) return
    const handleFindShortcut = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey)) return
      if (!event.shiftKey && !event.altKey && (event.key.toLowerCase() === 'f' || event.key.toLowerCase() === 'r')) {
        event.preventDefault()
        openFindReplace()
      }
    }
    window.addEventListener('keydown', handleFindShortcut)
    return () => window.removeEventListener('keydown', handleFindShortcut)
  })

  useEffect(() => {
    const desktopLayout = window.matchMedia('(min-width: 1321px)')
    const syncInspector = () => {
      if (!desktopLayout.matches) update({ inspectorOpen: false })
    }
    desktopLayout.addEventListener('change', syncInspector)
    return () => desktopLayout.removeEventListener('change', syncInspector)
  }, [])

  function showError(error: unknown): void {
    if (isTaskCancelled(error)) return
    const message = error instanceof Error ? error.message : t('json.notice.failed')
    update({ notice: message })
    toast.error(message)
  }

  async function calculate(operation: JsonOperation, input = state.content): Promise<string> {
    taskController.current?.abort()
    const controller = new AbortController()
    taskController.current = controller
    const snapshot = currentInput.current
    setCalculating(true)
    try {
      const output = await runTextTask({ kind: 'json', operation, input, language,
        options: state.formatOptions, className: state.className, path: state.jsonPath }, controller.signal)
      const current = currentInput.current
      if (controller.signal.aborted || current.language !== snapshot.language
        || current.state.content !== snapshot.state.content || current.state.filePath !== snapshot.state.filePath
        || current.state.conversionInput !== snapshot.state.conversionInput
        || current.state.inputConversion !== snapshot.state.inputConversion) throw new DOMException('Cancelled', 'AbortError')
      return output
    } finally {
      if (taskController.current === controller) { taskController.current = null; setCalculating(false) }
    }
  }

  async function runTransform(operation: JsonOperation, success: string, summary = success): Promise<void> {
    try {
      const output = await calculate(operation)
      update({ content: output, notice: success })
      toast.success(success)
      await persistHistory(summary, state.content, output)
    } catch (error) { showError(error) }
  }

  async function showOutput(title: string, operation: JsonOperation): Promise<void> {
    try {
      const output = await calculate(operation)
      update({ outputDialog: { title, value: output }, notice: title })
      await persistHistory(title, state.content, output)
    } catch (error) { showError(error) }
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
      if (!state.filePath) { vaultRef.current?.createFile(file.content, file.name); return }
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
      const output = await calculate(state.inputConversion === 'xmlToJson' ? 'fromXml' : 'fromBean', state.conversionInput)
      const title = state.inputConversion === 'xmlToJson' ? t('json.action.xmlToJson') : t('json.action.beanToJson')
      update({ content: output, inputConversion: null, conversionInput: '', notice: title })
      toast.success(title)
      await persistHistory(title, state.conversionInput, output)
    } catch (error) {
      showError(error)
    }
  }

  function openFindReplace(): void {
    const selection = editorRef.current?.getSelection()
    const selected = selection && selection.end > selection.start
      ? state.content.slice(selection.start, selection.end)
      : undefined
    findIndexRef.current = 0
    jsonFindIndex = 0
    update({
      findOpen: true,
      replacedCount: 0,
      ...(selected !== undefined ? { findQuery: selected } : {})
    })
  }

  function findAround(forward: boolean): void {
    if (!state.findQuery) return
    const selection = editorRef.current?.getSelection()
    const fromIndex = forward
      ? (selection?.end ?? 0)
      : (selection?.start ?? 0)
    const result = findNextMatchWithWrap(state.content, state.findQuery, state.findOptions, fromIndex, forward)
    if (!result) {
      toast.info(t('findReplace.noMatches'))
      return
    }
    const { match, wrapped } = result
    if (wrapped) toast.info(t(forward ? 'findReplace.wrappedToStart' : 'findReplace.wrappedToEnd'))
    findIndexRef.current = forward ? match.end : match.start
    jsonFindIndex = findIndexRef.current
    editorRef.current?.selectRange(match.start, match.end)
  }

  function replaceCurrent(all: boolean): void {
    if (!state.findQuery) return
    if (all) {
      const result = replaceAllMatches(state.content, state.findQuery, state.replaceText, state.findOptions)
      if (result.count === 0) toast.info(t('findReplace.noMatches'))
      else update({ content: result.content, replacedCount: result.count, notice: '', copyState: 'idle' })
      return
    }
    const result = replaceCurrentMatch(
      state.content,
      state.findQuery,
      state.replaceText,
      state.findOptions,
      editorRef.current?.getSelection() ?? null
    )
    if (!result.replaced) {
      toast.info(t('findReplace.noMatches'))
      return
    }
    update({ content: result.content, replacedCount: state.replacedCount + 1, notice: '', copyState: 'idle' })
    findIndexRef.current = result.nextFrom
    jsonFindIndex = result.nextFrom
    const findQuery = state.findQuery
    const findOptions = state.findOptions
    requestAnimationFrame(() => {
      const match = findNextMatch(result.content, findQuery, findOptions, result.nextFrom, true)
      if (match) {
        findIndexRef.current = match.end
        jsonFindIndex = match.end
        editorRef.current?.selectRange(match.start, match.end)
      }
    })
  }

  const inspectorVisible = state.inspectorOpen && Boolean(state.filePath)
  return (
    <section className="tool-page tool-page--workspace json-tool" aria-label={t('json.title')}>

      <ResizableColumns
        className={inspectorVisible ? 'json-layout' : 'json-layout json-layout--editor-only'}
        columns={inspectorVisible ? 3 : 2}
        defaultSizes={inspectorVisible ? [190, 520, 280] : [190, 800]}
        minPaneWidths={inspectorVisible ? [150, 360, 220] : [150, 360]}
        minimumWidth={inspectorVisible ? 1100 : 720}
        storageKey={inspectorVisible ? 'json-three-pane' : 'json-two-pane'}
      >
        <JsonVaultPanel ref={vaultRef} content={state.content} onOpen={openVaultContent} onDirtyChange={setDirty} />
        <div className="editor-shell">
          {!state.filePath ? <div className="json-empty-state">
            <FileJson size={36} aria-hidden="true" />
            <h2>{t('json.empty.title')}</h2>
            <p>{t('json.empty.description')}</p>
            <div className="json-empty-state__actions">
              <button className="dialog-button dialog-button--primary" type="button" onClick={() => vaultRef.current?.createFile()}><FilePlus2 size={15} />{t('json.vault.new')}</button>
              <button className="dialog-button" type="button" onClick={() => { void importFile() }}><FolderOpen size={15} />{t('json.empty.import')}</button>
              <button className="dialog-button" type="button" onClick={() => vaultRef.current?.createFile(JSON.stringify({ name: 'MooTool', tools: ['JSON', 'PDF'], local: true }, null, 2), 'example.json')}><Sparkles size={15} />{t('json.empty.example')}</button>
            </div>
          </div> : <>
          <JsonToolbar
            wrap={state.wrap}
            copied={state.copyState === 'copied'}
            findOpen={state.findOpen}
            findText={state.findQuery}
            replaceText={state.replaceText}
            findOptions={state.findOptions}
            matchCount={findMatches.length}
            replacedCount={state.replacedCount}
            fontName={settings.editor.jsonFontName}
            onFontNameChange={(jsonFontName) => { void updateSettings({ editor: { jsonFontName } }) }}
            onFormat={() => { void runTransform('format', t('json.notice.formatted'), t('json.action.format')) }}
            onCompress={() => { void runTransform('compress', t('json.notice.compressed'), t('json.action.compress')) }}
            onToggleWrap={() => update({ wrap: !state.wrap })}
            onCopy={() => { void copyValue(state.content) }}
            onToggleFind={() => { if (state.findOpen) update({ findOpen: false }); else openFindReplace() }}
            onImport={() => { void importFile() }}
            onExport={() => { void exportFile() }}
            onHistory={() => update({ historyOpen: true })}
            onToggleInspector={() => update({ inspectorOpen: !state.inspectorOpen })}
            onClear={() => update({ content: '' })}
            onFindTextChange={(findQuery) => { findIndexRef.current = 0; jsonFindIndex = 0; update({ findQuery, replacedCount: 0 }) }}
            onReplaceTextChange={(replaceText) => update({ replaceText })}
            onFindOptionsChange={(findOptions) => { findIndexRef.current = 0; jsonFindIndex = 0; update({ findOptions, replacedCount: 0 }) }}
            onFind={() => findAround(true)}
            onFindPrevious={() => findAround(false)}
            onFindNext={() => findAround(true)}
            onReplace={() => replaceCurrent(false)}
            onReplaceAll={() => replaceCurrent(true)}
            onCloseFind={() => update({ findOpen: false, replacedCount: 0 })}
          />
          <JsonCodeEditor
            ref={editorRef}
            value={state.content}
            readOnly={!state.filePath}
            wrap={state.wrap}
            fontSize={settings.editor.jsonFontSize}
            fontFamily={cssFontFamily(settings.editor.jsonFontName)}
            searchQuery={state.findOpen ? state.findQuery : ''}
            searchOptions={state.findOptions}
            ariaLabel={t('json.editor.label')}
            initialViewState={jsonEditorViewState}
            onChange={(content) => update({ content, notice: '', copyState: 'idle' })}
            onKeyDown={(event) => {
              if (isFormatShortcut(event)) {
                event.preventDefault()
                void runTransform('format', t('json.notice.formatted'), t('json.action.format'))
              }
            }}
            onViewStateChange={(viewState) => { jsonEditorViewState = viewState }}
          />
          <footer className="json-statusbar">
            {calculating && <button type="button" onClick={() => taskController.current?.abort()}>{t('common.cancel')}</button>}
            <span title={state.filePath}>{dirty ? '• ' : ''}{state.filePath}</span>
            <span className={status.kind === 'valid' ? 'json-statusbar__validation json-statusbar--valid' : status.kind === 'error' ? 'json-statusbar__validation json-statusbar--error' : 'json-statusbar__validation'}>
              {status.kind === 'valid' ? <CheckCircle2 size={12} /> : status.kind === 'error' ? <XCircle size={12} /> : <FileJson size={12} />}
              <span>{status.message}</span>
            </span>
          </footer>
          </>}
        </div>

        {inspectorVisible && (
          <JsonInspector
            formatOptions={state.formatOptions}
            className={state.className}
            jsonPath={state.jsonPath}
            notice={state.notice}
            status={status}
            onFormatOptionsChange={(formatOptions) => update({ formatOptions })}
            onAdvancedFormat={() => { void runTransform('advancedFormat', t('json.notice.formatted'), t('json.format.apply')) }}
            onJsonToXml={() => { void showOutput(t('json.action.jsonToXml'), 'toXml') }}
            onXmlToJson={() => update({ inputConversion: 'xmlToJson', conversionInput: '' })}
            onBeanToJson={() => update({ inputConversion: 'beanToJson', conversionInput: '' })}
            onJsonToBean={() => { void showOutput(t('json.action.jsonToBean'), 'toBean') }}
            onSwap={() => { void runTransform('swap', t('json.action.swap')) }}
            onEscapeJson={() => { void runTransform('escape', t('json.notice.escaped'), t('json.action.escape')) }}
            onUnescapeJson={() => { void runTransform('unescape', t('json.notice.unescaped'), t('json.action.unescape')) }}
            onEscapeText={() => { void runTransform('escapeText', t('json.action.escapeText')) }}
            onUnescapeText={() => { void runTransform('unescapeText', t('json.action.unescapeText')) }}
            onClassNameChange={(className) => update({ className })}
            onJsonPathChange={(jsonPath) => update({ jsonPath })}
            onQueryPath={() => { void showOutput(t('json.panel.jsonPath'), 'query') }}
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
