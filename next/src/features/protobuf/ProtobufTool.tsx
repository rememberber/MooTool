import { ArrowLeft, ArrowRight, Braces, Copy, History, Play } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { ResizableColumns } from '@/shared/components/ResizableColumns'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import { TextCodeEditor } from '@/shared/components/TextCodeEditor'
import type { FuncHistoryRecord } from '@/shared/contracts/history'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { convertBinary, decodeWire, formatProtoDefinition, jsonToProtobuf, protobufToJson, type BinaryFormat } from './protobufTools'

type ProtobufTab = 'json' | 'wire' | 'convert'

const defaultProto = 'syntax = "proto3";\n\nmessage Person {\n  string name = 1;\n  int32 age = 2;\n}'

export function ProtobufTool() {
  const { t } = useI18n()
  const actions = useToolActions('protobuf')
  const [tab, setTab] = useState<ProtobufTab>('json')
  const [proto, setProto] = useState(defaultProto)
  const [messageName, setMessageName] = useState('Person')
  const [format, setFormat] = useState<BinaryFormat>('Hex')
  const [json, setJson] = useState('{\n  "name": "MooTool",\n  "age": 25\n}')
  const [binary, setBinary] = useState('')
  const [wireInput, setWireInput] = useState('')
  const [wireFormat, setWireFormat] = useState<BinaryFormat>('Hex')
  const [wireOutput, setWireOutput] = useState('')
  const [hex, setHex] = useState('')
  const [base64, setBase64] = useState('')
  const [historyOpen, setHistoryOpen] = useState(false)

  function run(operation: () => void): void {
    try { operation() } catch (error) { actions.reportError(error) }
  }

  function jsonToBinary(): void {
    run(() => {
      const output = jsonToProtobuf(proto, messageName, json, format)
      setBinary(output)
      void actions.saveHistory(t('protobuf.history.jsonToBinary'), json, output, `json|jsonToBinary|${messageName}|${format}`)
    })
  }

  function binaryToJson(): void {
    run(() => {
      const output = protobufToJson(proto, messageName, binary, format)
      setJson(output)
      void actions.saveHistory(t('protobuf.history.binaryToJson'), binary, output, `json|binaryToJson|${messageName}|${format}`)
    })
  }

  function formatProto(): void {
    const before = proto
    const output = formatProtoDefinition(proto)
    setProto(output)
    void actions.saveHistory(t('protobuf.history.format'), before, output, 'json|format')
  }

  function decode(): void {
    run(() => {
      const output = decodeWire(wireInput, wireFormat)
      setWireOutput(output)
      void actions.saveHistory(t('protobuf.history.wire'), wireInput, output, `wire|decode|${wireFormat}`)
    })
  }

  function hexToBase64(): void {
    run(() => { const output = convertBinary(hex, 'Hex', 'Base64'); setBase64(output); void actions.saveHistory(t('protobuf.history.hexToBase64'), hex, output, 'convert|hexToBase64') })
  }

  function base64ToHex(): void {
    run(() => { const output = convertBinary(base64, 'Base64', 'Hex'); setHex(output); void actions.saveHistory(t('protobuf.history.base64ToHex'), base64, output, 'convert|base64ToHex') })
  }

  function applyHistory(record: FuncHistoryRecord): void {
    const [historyTab, operation, historyMessage, historyFormat] = (record.extraData ?? '').split('|')
    if (historyTab === 'json' || historyTab === 'wire' || historyTab === 'convert') setTab(historyTab)
    if (historyMessage) setMessageName(historyMessage)
    if (historyFormat === 'Hex' || historyFormat === 'Base64') setFormat(historyFormat)
    if (operation === 'jsonToBinary') { setJson(record.inputText); setBinary(record.outputText) }
    else if (operation === 'binaryToJson') { setBinary(record.inputText); setJson(record.outputText) }
    else if (operation === 'format') setProto(record.outputText)
    else if (operation === 'decode') { setWireInput(record.inputText); setWireOutput(record.outputText); if (historyFormat === 'Hex' || historyFormat === 'Base64') setWireFormat(historyFormat) }
    else if (operation === 'hexToBase64') { setHex(record.inputText); setBase64(record.outputText) }
    else if (operation === 'base64ToHex') { setBase64(record.inputText); setHex(record.outputText) }
  }

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('protobuf.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <div className="local-tool-shell protobuf-workspace">
        <ToolTabs tabs={[{ id: 'json', label: t('protobuf.tab.json') }, { id: 'wire', label: t('protobuf.tab.wire') }, { id: 'convert', label: t('protobuf.tab.convert') }]} active={tab} onChange={setTab} />
        {tab === 'json' && <ResizableColumns className="protobuf-json-layout" columns={2} defaultSizes={[0.34, 0.66]} minPaneWidths={[240, 420]} minimumWidth={760} storageKey="protobuf-json"><section className="proto-definition"><header><span>{t('protobuf.definition')}</span><button className="dialog-button" type="button" onClick={formatProto}><Braces size={14} />{t('common.action.format')}</button></header><TextCodeEditor ariaLabel={t('protobuf.definition')} value={proto} onChange={setProto} /><div className="proto-definition-options"><label><span>{t('protobuf.message')}</span><input value={messageName} spellCheck={false} onChange={(event) => setMessageName(event.target.value)} /></label><label><span>{t('protobuf.binaryFormat')}</span><select value={format} onChange={(event) => setFormat(event.target.value as BinaryFormat)}><option>Hex</option><option>Base64</option></select></label></div></section><div className="protobuf-convert-grid"><div className="text-pane"><span>JSON</span><TextCodeEditor ariaLabel="JSON" language="json" value={json} onChange={setJson} /></div><div className="vertical-actions"><button className="primary-command" type="button" onClick={jsonToBinary}><ArrowRight size={14} />{t('protobuf.toBinary')}</button><button className="dialog-button" type="button" onClick={binaryToJson}><ArrowLeft size={14} />{t('protobuf.toJson')}</button></div><div className="text-pane"><span>{t('protobuf.binary')}</span><TextCodeEditor ariaLabel={t('protobuf.binary')} value={binary} onChange={setBinary} /></div></div></ResizableColumns>}
        {tab === 'wire' && <ResizableColumns className="protobuf-wire-layout" columns={3} defaultSizes={[1, 0.28, 1]} minPaneWidths={[240, 110, 240]} storageKey="protobuf-wire"><div className="text-pane"><span>{t('protobuf.wireInput')}</span><TextCodeEditor ariaLabel={t('protobuf.wireInput')} value={wireInput} onChange={setWireInput} /></div><div className="vertical-actions"><select aria-label={t('protobuf.binaryFormat')} value={wireFormat} onChange={(event) => setWireFormat(event.target.value as BinaryFormat)}><option>Hex</option><option>Base64</option></select><button className="primary-command" type="button" onClick={decode}><Play size={14} />{t('protobuf.decode')}</button></div><div className="text-pane"><span>{t('protobuf.wireOutput')}</span><TextCodeEditor ariaLabel={t('protobuf.wireOutput')} value={wireOutput} readOnly /></div></ResizableColumns>}
        {tab === 'convert' && <ResizableColumns className="protobuf-wire-layout" columns={3} defaultSizes={[1, 0.28, 1]} minPaneWidths={[240, 110, 240]} storageKey="protobuf-convert"><div className="text-pane"><span>Hex</span><TextCodeEditor ariaLabel="Hex" value={hex} onChange={setHex} /></div><div className="vertical-actions"><button className="primary-command" type="button" onClick={hexToBase64}><ArrowRight size={14} />{t('protobuf.hexToBase64')}</button><button className="dialog-button" type="button" onClick={base64ToHex}><ArrowLeft size={14} />{t('protobuf.base64ToHex')}</button></div><div className="text-pane"><span>Base64</span><TextCodeEditor ariaLabel="Base64" value={base64} onChange={setBase64} /></div></ResizableColumns>}
        <button className="protobuf-copy" type="button" aria-label={t('common.action.copy')} onClick={() => { void actions.copy(tab === 'wire' ? wireOutput : tab === 'convert' ? base64 : binary) }}><Copy size={14} />{t('common.action.copy')}</button>
      </div>
      <HistoryDialog funcType="protobuf" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={() => undefined} onApplyRecord={applyHistory} />
    </section>
  )
}
