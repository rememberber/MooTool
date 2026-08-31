import {
  ArrowLeftRight,
  Binary,
  CheckCircle2,
  Clipboard,
  Copy,
  FileCode2,
  Play,
  ScanSearch,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { json } from '@codemirror/lang-json'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  convertProtobufBinary,
  decodeProtobuf,
  encodeProtobuf,
  inspectProtobufSchema,
  inspectWire,
  ProtobufToolError,
  type ProtobufBinaryFormat
} from './protobufTools'
import { protobufMessages } from './protobufMessages'

type ProtobufMessageKey = LocalizedMessageKey<typeof protobufMessages>
type ProtobufNotice = { key: ProtobufMessageKey; values?: MessageValues } | { raw: string }

const defaultSchema = `syntax = "proto3";
package mootool.demo;

message Person {
  uint64 id = 1;
  string name = 2;
  repeated string tags = 3;
  Role role = 4;
}

enum Role {
  ROLE_UNSPECIFIED = 0;
  ADMIN = 1;
  MEMBER = 2;
}`

const defaultJson = `{
  "id": "9007199254740993",
  "name": "MooTool",
  "tags": ["tauri", "desktop"],
  "role": "ADMIN"
}`

export function ProtobufSurface() {
  const { t } = useLocalizedMessages(protobufMessages)
  const [schema, setSchema] = useState(defaultSchema)
  const [messageName, setMessageName] = useState('mootool.demo.Person')
  const [jsonInput, setJsonInput] = useState(defaultJson)
  const [binaryInput, setBinaryInput] = useState('')
  const [format, setFormat] = useState<ProtobufBinaryFormat>('base64')
  const [wire, setWire] = useState('')
  const [wireInspected, setWireInspected] = useState(false)
  const [notice, setNotice] = useState<ProtobufNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const schemaInfo = useMemo(() => {
    try {
      return inspectProtobufSchema(schema)
    } catch {
      return { messageNames: [], packageName: '' }
    }
  }, [schema])
  const session = useMemo(() => ({
    digest: JSON.stringify({
      schemaHash: contentFingerprint(schema),
      messageName,
      format,
      jsonHash: contentFingerprint(jsonInput),
      binaryHash: contentFingerprint(binaryInput)
    }),
    summary: t('session.summary', { message: messageName || t('session.noMessage'), format: format.toUpperCase(), count: binaryInput.replace(/\s/g, '').length })
  }), [binaryInput, format, jsonInput, messageName, schema, t])
  const { sessionId, reportError } = useToolSessionReport('protobuf', session.digest, session.summary)
  const recordOperation = useOperationHistory('protobuf')

  useOperationRestore('protobuf', (entry) => {
    const metadata = parseOperationMetadata(entry)
    try {
      const input = JSON.parse(entry.inputText) as { schema?: string; jsonInput?: string; binaryInput?: string }
      setSchema(input.schema ?? defaultSchema)
      setJsonInput(input.jsonInput ?? '')
      setBinaryInput(input.binaryInput ?? '')
    } catch {
      setJsonInput(entry.inputText)
      setBinaryInput(entry.outputText)
    }
    if (typeof metadata.messageName === 'string') setMessageName(metadata.messageName)
    if (metadata.format === 'base64' || metadata.format === 'hex') setFormat(metadata.format)
    setWire('')
    setWireInspected(false)
    setFailed(false)
  })

  function encode(): void {
    try {
      const output = encodeProtobuf(schema, messageName, jsonInput, format)
      setBinaryInput(output)
      setWire(inspectWire(output, format))
      setWireInspected(true)
      succeed('notice.encoded')
      recordOperation(t('action.encode'), `${messageName} · ${format.toUpperCase()} · ${output.length}`, 'success', {
        inputText: JSON.stringify({ schema, jsonInput, binaryInput: '' }), outputText: output,
        metadata: { direction: 'encode', messageName, format }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  function decode(): void {
    try {
      const output = decodeProtobuf(schema, messageName, binaryInput, format)
      setJsonInput(output)
      setWire(inspectWire(binaryInput, format))
      setWireInspected(true)
      succeed('notice.decoded')
      recordOperation(t('action.decode'), `${messageName} · ${format.toUpperCase()} · ${output.length}`, 'success', {
        inputText: JSON.stringify({ schema, jsonInput: '', binaryInput }), outputText: output,
        metadata: { direction: 'decode', messageName, format }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  function switchFormat(next: ProtobufBinaryFormat): void {
    try {
      setBinaryInput(binaryInput ? convertProtobufBinary(binaryInput, format, next) : '')
      setFormat(next)
      succeed('notice.format', { format: next.toUpperCase() })
    } catch (cause) {
      fail(cause)
    }
  }

  function inspect(): void {
    try {
      setWire(inspectWire(binaryInput, format))
      setWireInspected(true)
      succeed('notice.inspected')
    } catch (cause) {
      fail(cause)
    }
  }

  async function copyBinary(): Promise<void> {
    try {
      await clipboardApi.writeText(binaryInput)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'error.clipboard' })
      setFailed(true)
    }
  }

  function succeed(key: ProtobufMessageKey, values?: MessageValues): void {
    setNotice({ key, values })
    setFailed(false)
  }

  function fail(cause: unknown): void {
    setNotice(cause instanceof ProtobufToolError
      ? { key: `error.${cause.code}`, values: cause.values }
      : { raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
  }

  return (
    <main className="utility-workbench protobuf-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI PROTOBUF WORKBENCH</span>
          <h1>Protobuf</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar protobuf-toolbar">
        <label className="utility-select">
          {t('field.message')}
          <select value={messageName} onChange={(event) => setMessageName(event.target.value)}>
            {schemaInfo.messageNames.length
              ? schemaInfo.messageNames.map((name) => <option key={name}>{name}</option>)
              : <option value="">{t('field.noMessages')}</option>}
          </select>
        </label>
        <div className="utility-segments">
          {(['base64', 'hex'] as const).map((item) => (
            <button
              className={format === item ? 'utility-segment utility-segment--active' : 'utility-segment'}
              type="button"
              key={item}
              onClick={() => switchFormat(item)}
            >
              {item.toUpperCase()}
            </button>
          ))}
        </div>
        <span className="protobuf-package"><FileCode2 />{schemaInfo.packageName || t('field.noPackage')}</span>
      </section>

      <section className="protobuf-grid">
        <section className="utility-editor-card protobuf-schema">
          <header><span>{t('pane.schema')}</span><code>{t('pane.messages', { count: schemaInfo.messageNames.length })}</code></header>
          <CodeEditor
            ariaLabel="Protobuf schema"
            value={schema}
            onChange={setSchema}
            className="utility-code-editor"
            lineWrapping={false}
          />
        </section>

        <section className="protobuf-convert">
          <section className="utility-editor-card">
            <header><span>{t('pane.json')}</span><button className="utility-copy" type="button" onClick={encode}><Play />{t('action.encode')}</button></header>
            <CodeEditor
              ariaLabel={t('aria.json')}
              value={jsonInput}
              onChange={setJsonInput}
              extensions={[json()]}
              className="utility-code-editor"
              lineWrapping={false}
            />
          </section>
          <div className="protobuf-actions">
            <button type="button" onClick={encode}><Binary />{t('action.encode')}</button>
            <ArrowLeftRight />
            <button type="button" onClick={decode}><FileCode2 />{t('action.decode')}</button>
          </div>
          <section className="utility-editor-card">
            <header>
              <span>{t('pane.binary', { format: format.toUpperCase() })}</span>
              <button className="utility-copy" type="button" disabled={!binaryInput} onClick={() => void copyBinary()}>
                {copied ? <Clipboard /> : <Copy />}{t(copied ? 'action.copied' : 'action.copy')}
              </button>
            </header>
            <CodeEditor
              ariaLabel={t('aria.binary')}
              value={binaryInput}
              onChange={setBinaryInput}
              className="utility-code-editor"
              lineWrapping
            />
          </section>
        </section>

        <section className="protobuf-wire">
          <header><ScanSearch /><strong>Wire Inspector</strong><button type="button" onClick={inspect}>{t('action.inspect')}</button></header>
          <pre>{wireInspected ? wire || t('wire.empty') : t('wire.hint')}</pre>
        </section>
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
