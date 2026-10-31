import {
  ArrowLeft,
  ArrowLeftRight,
  ArrowRight,
  Binary,
  Braces,
  CheckCircle2,
  Clipboard,
  Copy,
  FileCode2,
  Play,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { json } from '@codemirror/lang-json'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { ResizableThreeColumns } from '../../shared/ResizableThreeColumns'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  convertProtobufBinary,
  decodeProtobuf,
  encodeProtobuf,
  formatProtoDefinition,
  inspectProtobufSchema,
  inspectWire,
  ProtobufToolError,
  type ProtobufBinaryFormat
} from './protobufTools'
import { protobufMessages } from './protobufMessages'

type ProtobufMessageKey = LocalizedMessageKey<typeof protobufMessages>
type ProtobufNotice = { key: ProtobufMessageKey; values?: MessageValues } | { raw: string }
type ProtobufTab = 'json' | 'wire' | 'convert'

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
  const [tab, setTab] = useState<ProtobufTab>('json')
  const [wireInput, setWireInput] = useState('')
  const [wireFormat, setWireFormat] = useState<ProtobufBinaryFormat>('hex')
  const [wireOutput, setWireOutput] = useState('')
  const [hex, setHex] = useState('')
  const [base64, setBase64] = useState('')
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
    if (metadata.direction === 'wire') {
      setTab('wire')
      setWireInput(entry.inputText)
      setWireOutput(entry.outputText)
      if (metadata.format === 'base64' || metadata.format === 'hex') setWireFormat(metadata.format)
      setFailed(false)
      return
    }
    if (metadata.direction === 'hexToBase64' || metadata.direction === 'base64ToHex') {
      setTab('convert')
      if (metadata.direction === 'hexToBase64') {
        setHex(entry.inputText)
        setBase64(entry.outputText)
      } else {
        setBase64(entry.inputText)
        setHex(entry.outputText)
      }
      setFailed(false)
      return
    }
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
    setTab('json')
    setFailed(false)
  })

  function encode(): void {
    try {
      const output = encodeProtobuf(schema, messageName, jsonInput, format)
      setBinaryInput(output)
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

  function formatProto(): void {
    const output = formatProtoDefinition(schema)
    setSchema(output)
    succeed('notice.formatted')
  }

  function decodeWireInput(): void {
    try {
      const output = inspectWire(wireInput, wireFormat)
      setWireOutput(output)
      succeed('notice.inspected')
      recordOperation(t('action.decodeWire'), `${wireFormat.toUpperCase()} · ${output.length}`, 'success', {
        inputText: wireInput, outputText: output, metadata: { direction: 'wire', format: wireFormat }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  function convertBinary(direction: 'hexToBase64' | 'base64ToHex'): void {
    try {
      const output = direction === 'hexToBase64'
        ? convertProtobufBinary(hex, 'hex', 'base64')
        : convertProtobufBinary(base64, 'base64', 'hex')
      if (direction === 'hexToBase64') setBase64(output)
      else setHex(output)
      succeed('notice.converted')
      recordOperation(t(direction === 'hexToBase64' ? 'action.hexToBase64' : 'action.base64ToHex'), String(output.length), 'success', {
        inputText: direction === 'hexToBase64' ? hex : base64, outputText: output, metadata: { direction }
      })
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
        <h1 className="visually-hidden">Protobuf</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar protobuf-toolbar">
        <div className="protobuf-tabs" role="tablist">
          {(['json', 'wire', 'convert'] as const).map((id) => (
            <button key={id} type="button" role="tab" aria-selected={tab === id} onClick={() => setTab(id)}>{t(`tab.${id}`)}</button>
          ))}
        </div>
        {tab === 'json' && <label className="utility-select">
          {t('field.message')}
          <select value={messageName} onChange={(event) => setMessageName(event.target.value)}>
            {schemaInfo.messageNames.length
              ? schemaInfo.messageNames.map((name) => <option key={name}>{name}</option>)
              : <option value="">{t('field.noMessages')}</option>}
          </select>
        </label>}
        {tab === 'json' && <div className="utility-segments">
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
        </div>}
        {tab === 'json' && <span className="protobuf-package"><FileCode2 />{schemaInfo.packageName || t('field.noPackage')}</span>}
      </section>

      {tab === 'json' && <ResizableColumns id="protobuf-json" className="protobuf-grid" initialPrimary={0.34} defaultSizes={[0.34, 0.66]} minPrimary={240} minSecondary={420}>
        <section className="utility-editor-card protobuf-schema">
          <header><span>{t('pane.schema')}</span><button className="utility-copy" type="button" onClick={formatProto}><Braces />{t('action.format')}</button><code>{t('pane.messages', { count: schemaInfo.messageNames.length })}</code></header>
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
      </ResizableColumns>}

      {tab === 'wire' && (
        <ResizableThreeColumns id="protobuf-wire" className="protobuf-io" initialLeft={1} initialRight={1} defaultSizes={[1, 0.28, 1]} minLeft={240} minCenter={110} minRight={240}>
          <section className="utility-editor-card">
            <header><span>{t('pane.wireInput')}</span></header>
            <CodeEditor ariaLabel={t('pane.wireInput')} value={wireInput} onChange={setWireInput} className="utility-code-editor" lineWrapping />
          </section>
          <div className="protobuf-actions">
            <select aria-label={t('field.wireFormat')} value={wireFormat} onChange={(event) => setWireFormat(event.target.value as ProtobufBinaryFormat)}>
              <option value="hex">Hex</option>
              <option value="base64">Base64</option>
            </select>
            <button type="button" onClick={decodeWireInput}><Play />{t('action.decodeWire')}</button>
          </div>
          <section className="utility-editor-card">
            <header><span>{t('pane.wireOutput')}</span></header>
            <CodeEditor ariaLabel={t('pane.wireOutput')} value={wireOutput} onChange={setWireOutput} readOnly className="utility-code-editor" lineWrapping />
          </section>
        </ResizableThreeColumns>
      )}

      {tab === 'convert' && (
        <ResizableThreeColumns id="protobuf-convert" className="protobuf-io" initialLeft={1} initialRight={1} defaultSizes={[1, 0.28, 1]} minLeft={240} minCenter={110} minRight={240}>
          <section className="utility-editor-card">
            <header><span>Hex</span></header>
            <CodeEditor ariaLabel="Hex" value={hex} onChange={setHex} className="utility-code-editor" lineWrapping />
          </section>
          <div className="protobuf-actions">
            <button type="button" onClick={() => convertBinary('hexToBase64')}><ArrowRight />{t('action.hexToBase64')}</button>
            <button type="button" onClick={() => convertBinary('base64ToHex')}><ArrowLeft />{t('action.base64ToHex')}</button>
          </div>
          <section className="utility-editor-card">
            <header><span>Base64</span></header>
            <CodeEditor ariaLabel="Base64" value={base64} onChange={setBase64} className="utility-code-editor" lineWrapping />
          </section>
        </ResizableThreeColumns>
      )}

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}
