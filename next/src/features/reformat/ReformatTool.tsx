import { Copy, FileDown, FolderOpen, History, Paintbrush, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { formatCode, reformatTypes, type ReformatType } from './reformatTools'

type ReformatTab = 'text' | 'file'

const sampleByType: Record<ReformatType, string> = {
  nginx: 'server { listen 80; location / { proxy_pass http://127.0.0.1:3000; } }',
  java: 'class Demo{public static void main(String[] args){System.out.println("MooTool");}}',
  xml: '<root><tool id="mootool"><name>MooTool</name></tool></root>',
  html: '<main><h1>MooTool</h1><p>Desktop toolbox</p></main>'
}

export function ReformatTool() {
  const { t } = useI18n()
  const actions = useToolActions('reformat')
  const [tab, setTab] = useState<ReformatTab>('text')
  const [type, setType] = useState<ReformatType>('nginx')
  const [indent, setIndent] = useState(4)
  const [text, setText] = useState(sampleByType.nginx)
  const [fileName, setFileName] = useState('')
  const [fileSource, setFileSource] = useState('')
  const [fileResult, setFileResult] = useState('')
  const [busy, setBusy] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)

  async function runText(): Promise<void> {
    setBusy(true)
    try {
      const output = await formatCode(text, type, indent)
      setText(output)
      void actions.saveHistory(t('reformat.historySummary', { type: type.toUpperCase() }), text, output, `${type}|text|${indent}`)
      actions.toast.success(t('reformat.formatted'))
    } catch (error) {
      actions.reportError(error)
    } finally {
      setBusy(false)
    }
  }

  async function chooseFile(): Promise<void> {
    try {
      const file = await window.mootool.openTextFile('source')
      if (!file) return
      setFileName(file.name)
      setFileSource(file.content)
      setFileResult('')
    } catch (error) {
      actions.reportError(error)
    }
  }

  async function runFile(): Promise<void> {
    setBusy(true)
    try {
      const output = await formatCode(fileSource, type, indent)
      setFileResult(output)
      void actions.saveHistory(t('reformat.historySummary', { type: type.toUpperCase() }), fileSource, output, `${type}|file|${indent}|${fileName}`)
      actions.toast.success(t('reformat.formatted'))
    } catch (error) {
      actions.reportError(error)
    } finally {
      setBusy(false)
    }
  }

  async function exportResult(): Promise<void> {
    const content = tab === 'text' ? text : fileResult
    if (!content) return
    const extension = type === 'nginx' ? 'conf' : type
    const base = fileName ? fileName.replace(/\.[^.]+$/, '') : 'formatted'
    await window.mootool.saveTextFile({ kind: 'text', defaultName: `${base}.${extension}`, content })
  }

  function changeType(nextType: ReformatType): void {
    setType(nextType)
    if (tab === 'text' && (!text.trim() || Object.values(sampleByType).includes(text))) setText(sampleByType[nextType])
  }

  const toolbar = (
    <>
      <label className="compact-field"><span>{t('reformat.type')}</span><select value={type} onChange={(event) => changeType(event.target.value as ReformatType)}>{reformatTypes.map((item) => <option key={item} value={item}>{item === 'nginx' ? 'Nginx' : item.toUpperCase()}</option>)}</select></label>
      <label className="compact-field"><span>{t('reformat.indent')}</span><select value={indent} onChange={(event) => setIndent(Number(event.target.value))}>{[2, 3, 4, 5, 6].map((size) => <option key={size} value={size}>{size}</option>)}</select></label>
      <button className="primary-command" type="button" disabled={busy || !(tab === 'text' ? text : fileSource).trim()} onClick={() => { void (tab === 'text' ? runText() : runFile()) }}><Paintbrush size={14} />{busy ? t('common.processing') : t('common.action.format')}</button>
    </>
  )

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('reformat.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <div className="local-tool-shell reformat-workspace">
        <ToolTabs tabs={[{ id: 'text', label: t('reformat.tab.text') }, { id: 'file', label: t('reformat.tab.file') }]} active={tab} onChange={setTab} />
        <div className="p4-toolbar">{toolbar}<span className="p4-toolbar__spacer" /><button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.action.copy')} onClick={() => { void actions.copy(tab === 'text' ? text : fileResult) }}><Copy size={14} /></button><button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.export')} onClick={() => { void exportResult() }}><FileDown size={14} /></button><button className="toolbar-button toolbar-button--icon" type="button" aria-label={t('common.action.clear')} onClick={() => { if (tab === 'text') setText(''); else { setFileName(''); setFileSource(''); setFileResult('') } }}><Trash2 size={14} /></button></div>
        {tab === 'text' ? <textarea className="code-editor" aria-label={t('reformat.input')} value={text} spellCheck={false} onChange={(event) => setText(event.target.value)} onKeyDown={(event) => { if ((event.metaKey || event.ctrlKey) && event.shiftKey && event.key.toLowerCase() === 'f') { event.preventDefault(); void runText() } }} /> : (
          <ResizableColumns className="reformat-file-layout" columns={2} defaultSizes={[1, 1]} minPaneWidths={[260, 260]} paneSelector=".text-pane" storageKey="reformat-file">
            <div className="file-drop-row"><button className="dialog-button" type="button" onClick={() => { void chooseFile() }}><FolderOpen size={14} />{t('reformat.chooseFile')}</button><span>{fileName || t('reformat.noFile')}</span></div>
            <label className="text-pane"><span>{t('reformat.original')}</span><textarea value={fileSource} spellCheck={false} onChange={(event) => setFileSource(event.target.value)} /></label>
            <label className="text-pane"><span>{t('reformat.result')}</span><textarea value={fileResult} spellCheck={false} readOnly /></label>
          </ResizableColumns>
        )}
      </div>
      <HistoryDialog funcType="reformat" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={setText} onApplyRecord={(record) => {
        const [historyType, historyTab, historyIndent] = (record.extraData ?? '').split('|')
        if (reformatTypes.includes(historyType as ReformatType)) setType(historyType as ReformatType)
        setIndent(Number(historyIndent) || 4)
        if (historyTab === 'file') { setTab('file'); setFileSource(record.inputText); setFileResult(record.outputText) } else { setTab('text'); setText(record.outputText || record.inputText) }
      }} />
    </section>
  )
}
