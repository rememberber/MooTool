import {
  CheckCircle2,
  Clipboard,
  Copy,
  Dices,
  FileSearch,
  KeyRound,
  LockKeyhole,
  Play,
  RefreshCw,
  ShieldCheck,
  TriangleAlert,
  UnlockKeyhole
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { fileDigestApi } from '../../platform/api/fileDigestApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  decodeBase,
  decryptAesGcm,
  encodeBase,
  encryptAesGcm,
  generateRsaKeyPair,
  generateRandom,
  hashText,
  hmacSha256,
  rsaDecrypt,
  rsaEncrypt,
  rsaSign,
  rsaVerify,
  CryptoToolError,
  type BaseAlgorithm,
  type HashAlgorithm,
  type RandomKind
} from './cryptoTools'
import { cryptoMessages } from './cryptoMessages'

type CryptoTab = 'digest' | 'aes' | 'rsa' | 'base' | 'random'
type CryptoMessageKey = LocalizedMessageKey<typeof cryptoMessages>
type CryptoNotice = { key: CryptoMessageKey; values?: MessageValues } | { raw: string }

export function CryptoSurface() {
  const { t } = useLocalizedMessages(cryptoMessages)
  const [tab, setTab] = useState<CryptoTab>('digest')
  const [source, setSource] = useState('MooTool Next Tauri')
  const [output, setOutput] = useState('')
  const [hashAlgorithm, setHashAlgorithm] = useState<HashAlgorithm>('sha256')
  const [hmacSecret, setHmacSecret] = useState('')
  const [baseAlgorithm, setBaseAlgorithm] = useState<BaseAlgorithm>('base64')
  const [passphrase, setPassphrase] = useState('')
  const [rsaBits, setRsaBits] = useState<2048 | 3072 | 4096>(2048)
  const [rsaPublicKey, setRsaPublicKey] = useState('')
  const [rsaPrivateKey, setRsaPrivateKey] = useState('')
  const [rsaBusy, setRsaBusy] = useState(false)
  const [randomKind, setRandomKind] = useState<RandomKind>('password')
  const [randomLength, setRandomLength] = useState(32)
  const [notice, setNotice] = useState<CryptoNotice>({ key: 'notice.ready', values: { tab: 'SHA-256' } })
  const [failed, setFailed] = useState(false)
  const [copied, setCopied] = useState(false)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const tabLabel = (value: CryptoTab) => t(tabMessageKey(value))
  const session = useMemo(() => ({
    digest: JSON.stringify({
      tab,
      algorithm: hashAlgorithm,
      sourceHash: contentFingerprint(source),
      outputHash: contentFingerprint(output),
      outputLength: output.length
    }),
    summary: t('session.summary', { tab: tabLabel(tab), count: output.length })
  }), [hashAlgorithm, output, source, t, tab])
  const { sessionId, reportError } = useToolSessionReport('crypto', session.digest, session.summary)
  const recordOperation = useOperationHistory('crypto')
  useOperationRestore('crypto', (entry) => {
    const metadata = parseOperationMetadata(entry)
    const nextTab = metadata.tab
    if (nextTab === 'digest' || nextTab === 'base' || nextTab === 'random') setTab(nextTab)
    if (typeof metadata.hashAlgorithm === 'string') setHashAlgorithm(metadata.hashAlgorithm as HashAlgorithm)
    if (typeof metadata.baseAlgorithm === 'string') setBaseAlgorithm(metadata.baseAlgorithm as BaseAlgorithm)
    if (typeof metadata.randomKind === 'string') setRandomKind(metadata.randomKind as RandomKind)
    if (typeof metadata.randomLength === 'number') setRandomLength(metadata.randomLength)
    setSource(entry.inputText)
    setOutput(entry.outputText)
    setFailed(false)
  })

  function runDigest(): void {
    try {
      const result = hmacSecret ? hmacSha256(source, hmacSecret) : hashText(source, hashAlgorithm)
      setOutput(result)
      succeed(hmacSecret ? 'notice.hmacDone' : 'notice.digestDone', hmacSecret ? undefined : { algorithm: hashAlgorithm.toUpperCase() })
      recordOperation(t('operation.digest'), `${hmacSecret ? 'HMAC-SHA256' : hashAlgorithm.toUpperCase()} · ${source.length}`, 'success', hmacSecret
        ? { metadata: { tab: 'digest', sensitive: true } }
        : { inputText: source, outputText: result, metadata: { tab: 'digest', hashAlgorithm } })
    } catch (cause) {
      fail(cause)
    }
  }

  async function runFileDigest(): Promise<void> {
    try {
      const result = await fileDigestApi.digest(hashAlgorithm)
      if (!result) return
      setOutput(result.digest)
      succeed('notice.fileDigest', { name: result.name, algorithm: hashAlgorithm.toUpperCase() })
      recordOperation(t('operation.fileDigest'), `${result.name} · ${hashAlgorithm.toUpperCase()} · ${result.digest}`, 'success', {
        outputText: result.digest, metadata: { tab: 'digest', operation: 'fileDigest', fileName: result.name, hashAlgorithm }
      })
    } catch (cause) { fail(cause) }
  }

  async function runAes(mode: 'encrypt' | 'decrypt'): Promise<void> {
    try {
      const result = mode === 'encrypt'
        ? await encryptAesGcm(source, passphrase)
        : await decryptAesGcm(source, passphrase)
      setOutput(result)
      succeed(mode === 'encrypt' ? 'notice.encrypted' : 'notice.decrypted')
      recordOperation(t(mode === 'encrypt' ? 'operation.encrypt' : 'operation.decrypt'), `AES-256-GCM · ${source.length}`, 'success', {
        metadata: { tab: 'aes', sensitive: true }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  function runBase(mode: 'encode' | 'decode'): void {
    try {
      const result = mode === 'encode' ? encodeBase(source, baseAlgorithm) : decodeBase(source, baseAlgorithm)
      setOutput(result)
      succeed(mode === 'encode' ? 'notice.encoded' : 'notice.decoded', { algorithm: baseAlgorithm.toUpperCase() })
      recordOperation(t(mode === 'encode' ? 'operation.encode' : 'operation.decode'), `${baseAlgorithm.toUpperCase()} · ${source.length}`, 'success', {
        inputText: source, outputText: result, metadata: { tab: 'base', mode, baseAlgorithm }
      })
    } catch (cause) { fail(cause) }
  }

  async function generateKeys(): Promise<void> {
    setRsaBusy(true)
    try {
      const pair = await generateRsaKeyPair(rsaBits)
      setRsaPublicKey(pair.publicKey)
      setRsaPrivateKey(pair.privateKey)
      succeed('notice.keysGenerated', { bits: rsaBits })
      recordOperation(t('operation.keyPair'), `RSA ${rsaBits}`, 'success', { metadata: { tab: 'rsa', sensitive: true } })
    } catch (cause) { fail(cause) } finally { setRsaBusy(false) }
  }

  async function runRsa(mode: 'encrypt' | 'decrypt' | 'sign' | 'verify'): Promise<void> {
    try {
      if (mode === 'verify') {
        const valid = await rsaVerify(source, output, rsaPublicKey)
        succeed(valid ? 'notice.verified' : 'notice.notVerified')
        recordOperation(t('operation.verify'), `RSA-PSS · ${valid}`, valid ? 'success' : 'error', { metadata: { tab: 'rsa', sensitive: true } })
        return
      }
      const result = mode === 'encrypt'
        ? await rsaEncrypt(source, rsaPublicKey)
        : mode === 'decrypt'
          ? await rsaDecrypt(source, rsaPrivateKey)
          : await rsaSign(source, rsaPrivateKey)
      setOutput(result)
      succeed(mode === 'encrypt' ? 'notice.rsaEncrypted' : mode === 'decrypt' ? 'notice.rsaDecrypted' : 'notice.signed')
      recordOperation(t(`operation.${mode}` as CryptoMessageKey), `RSA · ${source.length}`, 'success', { metadata: { tab: 'rsa', sensitive: true } })
    } catch (cause) { fail(cause) }
  }

  function runRandom(): void {
    try {
      const result = generateRandom(randomKind, randomLength)
      setOutput(result)
      succeed(randomKind === 'uuid' ? 'notice.uuid' : 'notice.random')
      recordOperation(t('operation.random'), `${randomKind} · ${randomKind === 'uuid' ? 36 : randomLength}`, 'success', {
        outputText: result, metadata: { tab: 'random', randomKind, randomLength }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  async function copyOutput(): Promise<void> {
    try {
      await clipboardApi.writeText(output)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'error.clipboard' })
      setFailed(true)
    }
  }

  function succeed(key: CryptoMessageKey, values?: MessageValues): void {
    setNotice({ key, values })
    setFailed(false)
  }

  function fail(cause: unknown): void {
    setNotice(cause instanceof CryptoToolError ? { key: `error.${cause.code}` } : { raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
  }

  return (
    <main className="utility-workbench crypto-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI CRYPTO WORKBENCH</span>
          <h1>{t('title')}</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar">
        <div className="utility-segments" role="tablist">
          {(['digest', 'aes', 'rsa', 'base', 'random'] as const).map((item) => (
            <button
              className={tab === item ? 'utility-segment utility-segment--active' : 'utility-segment'}
              type="button"
              role="tab"
              aria-selected={tab === item}
              key={item}
              onClick={() => {
                setTab(item)
                setOutput('')
                succeed('notice.ready', { tab: tabLabel(item) })
              }}
            >
              {item === 'digest' ? <ShieldCheck /> : item === 'aes' || item === 'rsa' ? <KeyRound /> : item === 'base' ? <RefreshCw /> : <Dices />}
              {tabLabel(item)}
            </button>
          ))}
        </div>
        {tab === 'digest' && (
          <>
            <label className="utility-select">
              {t('field.digest')}
              <select
                value={hashAlgorithm}
                disabled={Boolean(hmacSecret)}
                onChange={(event) => setHashAlgorithm(event.target.value as HashAlgorithm)}
              >
                <option value="sha256">SHA-256</option>
                <option value="sha384">SHA-384</option>
                <option value="sha512">SHA-512</option>
                <option value="sha1">SHA-1 ({t('option.compatible')})</option>
                <option value="md5">MD5 ({t('option.compatible')})</option>
              </select>
            </label>
            <button className="secondary-button crypto-file-digest" type="button" disabled={hashAlgorithm === 'md5' || hashAlgorithm === 'sha1' || Boolean(hmacSecret)} onClick={() => void runFileDigest()}><FileSearch />{t('action.fileDigest')}</button>
            <label className="crypto-inline-field">
              {t('field.hmac')}
              <input
                type="password"
                value={hmacSecret}
                placeholder={t('placeholder.hmac')}
                onChange={(event) => setHmacSecret(event.target.value)}
              />
            </label>
          </>
        )}
        {tab === 'aes' && (
          <label className="crypto-inline-field">
            {t('field.passphrase')}
            <input
              type="password"
              value={passphrase}
              placeholder={t('placeholder.passphrase')}
              onChange={(event) => setPassphrase(event.target.value)}
            />
          </label>
        )}
        {tab === 'rsa' && (
          <>
            <label className="utility-select">{t('field.keySize')}<select value={rsaBits} onChange={(event) => setRsaBits(Number(event.target.value) as 2048 | 3072 | 4096)}><option value={2048}>2048</option><option value={3072}>3072</option><option value={4096}>4096</option></select></label>
            <button className="primary-button" type="button" disabled={rsaBusy} onClick={() => void generateKeys()}><KeyRound />{t(rsaBusy ? 'action.generating' : 'action.generateKeys')}</button>
            <details className="crypto-key-store"><summary className="secondary-button">{t('action.keys')}</summary><div><label>{t('field.publicKey')}<textarea value={rsaPublicKey} spellCheck={false} onChange={(event) => setRsaPublicKey(event.target.value)} /></label><label>{t('field.privateKey')}<textarea value={rsaPrivateKey} spellCheck={false} onChange={(event) => setRsaPrivateKey(event.target.value)} /></label></div></details>
          </>
        )}
        {tab === 'base' && <label className="utility-select">{t('field.encoding')}<select value={baseAlgorithm} onChange={(event) => setBaseAlgorithm(event.target.value as BaseAlgorithm)}><option value="base64">Base64</option><option value="base32">Base32</option></select></label>}
        {tab === 'random' && (
          <>
            <label className="utility-select">
              {t('field.type')}
              <select value={randomKind} onChange={(event) => setRandomKind(event.target.value as RandomKind)}>
                <option value="password">{t('random.password')}</option>
                <option value="alphanumeric">{t('random.alphanumeric')}</option>
                <option value="digits">{t('random.digits')}</option>
                <option value="hex">Hex</option>
                <option value="uuid">UUID v4</option>
              </select>
            </label>
            <label className="crypto-inline-field crypto-length-field">
              {t('field.length')}
              <input
                type="number"
                min={1}
                max={4096}
                disabled={randomKind === 'uuid'}
                value={randomLength}
                onChange={(event) => setRandomLength(Number(event.target.value))}
              />
            </label>
          </>
        )}
      </section>

      <section className="utility-editor-grid crypto-editor-grid">
        <section className="utility-editor-card">
          <header><span>{t(tab === 'random' ? 'pane.settings' : tab === 'aes' ? 'pane.aesInput' : tab === 'rsa' ? 'pane.rsaInput' : 'pane.source')}</span></header>
          {tab === 'random' ? (
            <div className="crypto-random-hero">
              <Dices />
              <strong>{t('random.secure')}</strong>
              <span>{t('random.detail')}</span>
              <button className="primary-button" type="button" onClick={runRandom}>
                <RefreshCw />{t('action.generate')}
              </button>
            </div>
          ) : (
            <CodeEditor
              ariaLabel={t('aria.input')}
              value={source}
              onChange={setSource}
              className="utility-code-editor"
              lineWrapping
            />
          )}
        </section>

        <section className="crypto-actions">
          {tab === 'digest' && (
            <button type="button" onClick={runDigest}><Play />{t('action.calculate')}</button>
          )}
          {tab === 'aes' && (
            <>
              <button type="button" onClick={() => void runAes('encrypt')}><LockKeyhole />{t('action.encrypt')}</button>
              <button type="button" onClick={() => void runAes('decrypt')}><UnlockKeyhole />{t('action.decrypt')}</button>
            </>
          )}
          {tab === 'rsa' && (
            <><button type="button" onClick={() => void runRsa('encrypt')}><LockKeyhole />{t('action.publicEncrypt')}</button><button type="button" onClick={() => void runRsa('decrypt')}><UnlockKeyhole />{t('action.privateDecrypt')}</button><button type="button" onClick={() => void runRsa('sign')}><KeyRound />{t('action.sign')}</button><button type="button" disabled={!output} onClick={() => void runRsa('verify')}><ShieldCheck />{t('action.verify')}</button></>
          )}
          {tab === 'base' && (
            <><button type="button" onClick={() => runBase('encode')}><LockKeyhole />{t('action.encode')}</button><button type="button" onClick={() => runBase('decode')}><UnlockKeyhole />{t('action.decode')}</button></>
          )}
        </section>

        <section className="utility-editor-card">
          <header>
            <span>{t('pane.output')}</span>
            <button className="utility-copy" type="button" disabled={!output} onClick={() => void copyOutput()}>
              {copied ? <Clipboard /> : <Copy />}{copied ? t('action.copied') : t('action.copy')}
            </button>
          </header>
          <CodeEditor
            ariaLabel={t('aria.output')}
            value={output}
            onChange={setOutput}
            className="utility-code-editor"
            lineWrapping
          />
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

function tabMessageKey(tab: CryptoTab): CryptoMessageKey {
  return { digest: 'tab.digest', aes: 'tab.aes', rsa: 'tab.rsa', base: 'tab.base', random: 'tab.random' }[tab] as CryptoMessageKey
}
