import {
  CheckCircle2,
  Clipboard,
  Copy,
  Dices,
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
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import {
  decryptAesGcm,
  encryptAesGcm,
  generateRandom,
  hashText,
  hmacSha256,
  CryptoToolError,
  type HashAlgorithm,
  type RandomKind
} from './cryptoTools'
import { cryptoMessages } from './cryptoMessages'

type CryptoTab = 'digest' | 'aes' | 'random'
type CryptoMessageKey = LocalizedMessageKey<typeof cryptoMessages>
type CryptoNotice = { key: CryptoMessageKey; values?: MessageValues } | { raw: string }

export function CryptoSurface() {
  const { t } = useLocalizedMessages(cryptoMessages)
  const [tab, setTab] = useState<CryptoTab>('digest')
  const [source, setSource] = useState('MooTool Next Tauri')
  const [output, setOutput] = useState('')
  const [hashAlgorithm, setHashAlgorithm] = useState<HashAlgorithm>('sha256')
  const [hmacSecret, setHmacSecret] = useState('')
  const [passphrase, setPassphrase] = useState('')
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

  function runDigest(): void {
    try {
      setOutput(hmacSecret ? hmacSha256(source, hmacSecret) : hashText(source, hashAlgorithm))
      succeed(hmacSecret ? 'notice.hmacDone' : 'notice.digestDone', hmacSecret ? undefined : { algorithm: hashAlgorithm.toUpperCase() })
    } catch (cause) {
      fail(cause)
    }
  }

  async function runAes(mode: 'encrypt' | 'decrypt'): Promise<void> {
    try {
      const result = mode === 'encrypt'
        ? await encryptAesGcm(source, passphrase)
        : await decryptAesGcm(source, passphrase)
      setOutput(result)
      succeed(mode === 'encrypt' ? 'notice.encrypted' : 'notice.decrypted')
    } catch (cause) {
      fail(cause)
    }
  }

  function runRandom(): void {
    try {
      setOutput(generateRandom(randomKind, randomLength))
      succeed(randomKind === 'uuid' ? 'notice.uuid' : 'notice.random')
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
          {(['digest', 'aes', 'random'] as const).map((item) => (
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
              {item === 'digest' ? <ShieldCheck /> : item === 'aes' ? <KeyRound /> : <Dices />}
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
          <header><span>{t(tab === 'random' ? 'pane.settings' : tab === 'aes' ? 'pane.aesInput' : 'pane.source')}</span></header>
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
        <span>Web Crypto · AES-256-GCM · PBKDF2-SHA256</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function tabMessageKey(tab: CryptoTab): CryptoMessageKey {
  return { digest: 'tab.digest', aes: 'tab.aes', random: 'tab.random' }[tab] as CryptoMessageKey
}
