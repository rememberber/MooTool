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
import { useEffect, useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { fileDigestApi } from '../../platform/api/fileDigestApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import {
  compatibilityDecrypt,
  compatibilityEncrypt,
  decryptAesGcm,
  encryptAesGcm,
  generateRsaKeyPair,
  generateSm2KeyPair,
  hashText,
  rsaDecrypt,
  rsaEncrypt,
  rsaSign,
  rsaVerify,
  sm2Decrypt,
  sm2Encrypt,
  sm2Sign,
  sm2Verify,
  CryptoToolError,
  type BaseAlgorithm,
  type CompatibilityAlgorithm,
  type HashAlgorithm
} from './cryptoTools'
import {
  asymmetricDecrypt,
  asymmetricEncrypt,
  generateAsymmetricKeyPair,
  privateEncrypt,
  publicDecrypt,
  decodeBase as decodeElectronBase,
  encodeBase as encodeElectronBase,
  randomDigits,
  randomPassword,
  randomString,
  randomUuid,
  signContent,
  symmetricDecrypt,
  symmetricEncrypt,
  verifySignature,
  type AsymmetricAlgorithm,
  type SymmetricAlgorithm
} from './electronCrypto'
import { cryptoMessages } from './cryptoMessages'

type CryptoTab = 'symmetric' | 'asymmetric' | 'digest' | 'aes' | 'rsa' | 'compatibility' | 'base' | 'random'
type CryptoMessageKey = LocalizedMessageKey<typeof cryptoMessages>
type CryptoNotice = { key: CryptoMessageKey; values?: MessageValues } | { raw: string }

export function CryptoSurface() {
  const { t } = useLocalizedMessages(cryptoMessages)
  const { settings, ready: settingsReady, save: saveSettings } = useSettings()
  const [tab, setTab] = useState<CryptoTab>('symmetric')
  const [source, setSource] = useState('MooTool parity')
  const [symAlgorithm, setSymAlgorithm] = useState<SymmetricAlgorithm>('AES')
  const [symKey, setSymKey] = useState('')
  const [legacyAes, setLegacyAes] = useState(false)
  const [asymAlgorithm, setAsymAlgorithm] = useState<AsymmetricAlgorithm>('RSA')
  const [publicKey, setPublicKey] = useState('')
  const [privateKey, setPrivateKey] = useState('')
  const [asymBusy, setAsymBusy] = useState(false)
  const [output, setOutput] = useState('')
  const [hashAlgorithm, setHashAlgorithm] = useState<HashAlgorithm>('sha256')
  const [digestInput, setDigestInput] = useState('MooTool')
  const [digestOutput, setDigestOutput] = useState('')
  const [digestFileName, setDigestFileName] = useState('')
  const [baseAlgorithm, setBaseAlgorithm] = useState<BaseAlgorithm>('base64')
  const [passphrase, setPassphrase] = useState('')
  const [rsaBits, setRsaBits] = useState<2048 | 3072 | 4096>(2048)
  const [rsaPublicKey, setRsaPublicKey] = useState('')
  const [rsaPrivateKey, setRsaPrivateKey] = useState('')
  const [rsaBusy, setRsaBusy] = useState(false)
  const [compatibilityAlgorithm, setCompatibilityAlgorithm] = useState<CompatibilityAlgorithm>('sm4')
  const [compatibilityKey, setCompatibilityKey] = useState('1234567890abcdef')
  const [sm2PublicKey, setSm2PublicKey] = useState('')
  const [sm2PrivateKey, setSm2PrivateKey] = useState('')
  const [randomLength, setRandomLength] = useState(32)
  const [uuid, setUuid] = useState('')
  const [digits, setDigits] = useState('')
  const [randomText, setRandomText] = useState('')
  const [password, setPassword] = useState('')
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
  useEffect(() => {
    if (settingsReady) setRandomLength(settings.tools.randomStringLength)
  }, [settings.tools.randomStringLength, settingsReady])
  useOperationRestore('crypto', (entry) => {
    const metadata = parseOperationMetadata(entry)
    const nextTab = metadata.tab
    if (nextTab === 'digest' || nextTab === 'base' || nextTab === 'random' || nextTab === 'compatibility' || nextTab === 'symmetric' || nextTab === 'asymmetric' || nextTab === 'aes' || nextTab === 'rsa') setTab(nextTab)
    if (typeof metadata.hashAlgorithm === 'string') setHashAlgorithm(metadata.hashAlgorithm as HashAlgorithm)
    if (typeof metadata.baseAlgorithm === 'string') setBaseAlgorithm(metadata.baseAlgorithm as BaseAlgorithm)
    if (typeof metadata.randomLength === 'number') setRandomLength(metadata.randomLength)
    if (typeof metadata.randomKind === 'string') {
      if (metadata.randomKind === 'uuid') setUuid(entry.outputText)
      else if (metadata.randomKind === 'digits') setDigits(entry.outputText)
      else if (metadata.randomKind === 'password') setPassword(entry.outputText)
      else if (metadata.randomKind === 'string' || metadata.randomKind === 'alphanumeric') setRandomText(entry.outputText)
    }
    if (metadata.compatibilityAlgorithm === 'des' || metadata.compatibilityAlgorithm === 'sm4' || metadata.compatibilityAlgorithm === 'sm2') setCompatibilityAlgorithm(metadata.compatibilityAlgorithm)
    if (metadata.tab === 'digest') {
      setDigestInput(entry.inputText)
      setDigestOutput(entry.outputText)
    } else {
      setSource(entry.inputText)
      setOutput(entry.outputText)
    }
    setFailed(false)
  })

  function runDigest(): void {
    try {
      const result = hashText(digestInput, hashAlgorithm)
      setDigestOutput(result)
      setDigestFileName('')
      succeed('notice.digestDone', { algorithm: hashAlgorithm.toUpperCase() })
      recordOperation(t('operation.digest'), `${hashAlgorithm.toUpperCase()} · ${digestInput.length}`, 'success', {
        inputText: digestInput, outputText: result, metadata: { tab: 'digest', hashAlgorithm }
      })
    } catch (cause) {
      fail(cause)
    }
  }

  async function runFileDigest(): Promise<void> {
    try {
      const result = await fileDigestApi.digest(hashAlgorithm)
      if (!result) return
      setDigestOutput(result.digest)
      setDigestFileName(result.name)
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

  function runSymmetric(mode: 'encrypt' | 'decrypt'): void {
    try {
      const result = mode === 'encrypt'
        ? symmetricEncrypt(symAlgorithm, source, symKey)
        : symmetricDecrypt(symAlgorithm, source, symKey, legacyAes)
      setOutput(result)
      succeed(mode === 'encrypt' ? 'notice.symmetricEncrypted' : 'notice.symmetricDecrypted')
      recordOperation(t(mode === 'encrypt' ? 'action.encrypt' : 'action.decrypt'), `${symAlgorithm} · ${result.length}`, 'success', {
        inputText: source, outputText: result, metadata: { tab: 'symmetric', algorithm: symAlgorithm, sensitive: true }
      })
    } catch (cause) { fail(cause) }
  }

  function generateAsymKeys(): void {
    setAsymBusy(true)
    try {
      const pair = generateAsymmetricKeyPair(asymAlgorithm, rsaBits)
      setPublicKey(pair.publicKey)
      setPrivateKey(pair.privateKey)
      succeed('notice.keyPairGenerated')
      recordOperation(t('action.generateKeys'), asymAlgorithm, 'success', { metadata: { tab: 'asymmetric', algorithm: asymAlgorithm, sensitive: true } })
    } catch (cause) { fail(cause) } finally { setAsymBusy(false) }
  }

  function runAsymmetric(mode: 'publicEncrypt' | 'privateDecrypt' | 'privateEncrypt' | 'publicDecrypt' | 'sign' | 'verify'): void {
    try {
      if (mode === 'verify') {
        const valid = verifySignature(asymAlgorithm, source, output, publicKey)
        succeed(valid ? 'notice.signatureValid' : 'notice.signatureInvalid')
        recordOperation(t('action.verify'), `${asymAlgorithm} · ${valid}`, valid ? 'success' : 'error', { metadata: { tab: 'asymmetric', sensitive: true } })
        return
      }
      const result = mode === 'publicEncrypt'
        ? asymmetricEncrypt(asymAlgorithm, source, publicKey)
        : mode === 'privateDecrypt'
          ? asymmetricDecrypt(asymAlgorithm, source, privateKey)
          : mode === 'privateEncrypt'
            ? privateEncrypt(source, privateKey)
            : mode === 'publicDecrypt'
              ? publicDecrypt(source, publicKey)
              : signContent(asymAlgorithm, source, privateKey, publicKey)
      setOutput(result)
      succeed(mode === 'sign' ? 'notice.signedContent' : mode.endsWith('Encrypt') ? 'notice.asymmetricEncrypted' : 'notice.asymmetricDecrypted')
      recordOperation(t(`action.${mode}`), asymAlgorithm, 'success', {
        inputText: source, outputText: result, metadata: { tab: 'asymmetric', algorithm: asymAlgorithm, sensitive: true }
      })
    } catch (cause) { fail(cause) }
  }

  function runBase(mode: 'encode' | 'decode'): void {
    try {
      const algorithm = baseAlgorithm === 'base32' ? 'Base32' : 'Base64'
      const result = mode === 'encode' ? encodeElectronBase(algorithm, source) : decodeElectronBase(algorithm, source)
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

  function generateSm2Keys(): void {
    try {
      const pair = generateSm2KeyPair()
      setSm2PublicKey(pair.publicKey)
      setSm2PrivateKey(pair.privateKey)
      succeed('notice.sm2KeysGenerated')
      recordOperation(t('operation.keyPair'), 'SM2', 'success', { metadata: { tab: 'compatibility', sensitive: true } })
    } catch (cause) { fail(cause) }
  }

  function runCompatibility(mode: 'encrypt' | 'decrypt' | 'sign' | 'verify'): void {
    try {
      if (compatibilityAlgorithm === 'sm2') {
        if (mode === 'verify') {
          const valid = sm2Verify(source, output, sm2PublicKey)
          succeed(valid ? 'notice.sm2Verified' : 'notice.sm2NotVerified')
          return
        }
        const result = mode === 'encrypt'
          ? sm2Encrypt(source, sm2PublicKey)
          : mode === 'decrypt'
            ? sm2Decrypt(source, sm2PrivateKey)
            : sm2Sign(source, sm2PrivateKey, sm2PublicKey)
        setOutput(result)
        succeed(mode === 'encrypt' ? 'notice.compatEncrypted' : mode === 'decrypt' ? 'notice.compatDecrypted' : 'notice.sm2Signed', { algorithm: 'SM2' })
      } else {
        if (mode === 'sign' || mode === 'verify') return
        const result = mode === 'encrypt'
          ? compatibilityEncrypt(compatibilityAlgorithm, source, compatibilityKey)
          : compatibilityDecrypt(compatibilityAlgorithm, source, compatibilityKey)
        setOutput(result)
        succeed(mode === 'encrypt' ? 'notice.compatEncrypted' : 'notice.compatDecrypted', { algorithm: compatibilityAlgorithm.toUpperCase() })
      }
      recordOperation(t(mode === 'encrypt' ? 'operation.encrypt' : mode === 'decrypt' ? 'operation.decrypt' : 'operation.sign'), `${compatibilityAlgorithm.toUpperCase()} · ${source.length}`, 'success', {
        metadata: { tab: 'compatibility', compatibilityAlgorithm, sensitive: true }
      })
    } catch (cause) { fail(cause) }
  }

  function generateRandomValue(kind: 'uuid' | 'digits' | 'string' | 'password'): void {
    try {
      const length = Math.min(4096, Math.max(1, Math.round(randomLength) || 1))
      if (length !== randomLength) setRandomLength(length)
      const result = kind === 'uuid' ? randomUuid() : kind === 'digits' ? randomDigits(length) : kind === 'string' ? randomString(length) : randomPassword(length)
      if (kind === 'uuid') setUuid(result)
      else if (kind === 'digits') setDigits(result)
      else if (kind === 'string') setRandomText(result)
      else setPassword(result)
      setOutput(result)
      succeed(kind === 'uuid' ? 'notice.uuid' : 'notice.random')
      recordOperation(t('operation.random'), `${kind} · ${kind === 'uuid' ? 36 : length}`, 'success', {
        outputText: result, metadata: { tab: 'random', randomKind: kind, randomLength: length }
      })
      if (settingsReady && length !== settings.tools.randomStringLength) void saveSettings({ ...settings, tools: { ...settings.tools, randomStringLength: length } })
    } catch (cause) {
      fail(cause)
    }
  }

  async function copyRandom(value: string): Promise<void> {
    try {
      await clipboardApi.writeText(value)
      setCopied(true)
      window.setTimeout(() => setCopied(false), 1200)
    } catch {
      setNotice({ key: 'error.clipboard' })
      setFailed(true)
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
        <h1 className="visually-hidden">{t('title')}</h1>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar">
        <div className="utility-segments" role="tablist">
          {(['symmetric', 'asymmetric', 'digest', 'base', 'random'] as const).map((item) => (
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
              {item === 'digest' ? <ShieldCheck /> : item === 'symmetric' || item === 'asymmetric' ? <KeyRound /> : item === 'base' ? <RefreshCw /> : <Dices />}
              {tabLabel(item)}
            </button>
          ))}
        </div>
        {tab === 'digest' && (
          <>
            <label className="utility-select">
              {t('field.digest')}
              <select aria-label={t('field.digest')} value={hashAlgorithm} onChange={(event) => setHashAlgorithm(event.target.value as HashAlgorithm)}>
                <option value="md5">MD5</option>
                <option value="sha1">SHA-1</option>
                <option value="sha256">SHA-256</option>
                <option value="sha384">SHA-384</option>
                <option value="sha512">SHA-512</option>
                <option value="sm3">SM3</option>
              </select>
            </label>
            <button className="primary-button" type="button" onClick={runDigest}><Play />{t('action.textDigest')}</button>
            <button className="secondary-button crypto-file-digest" type="button" onClick={() => void runFileDigest()}><FileSearch />{t('action.fileDigest')}</button>
            {digestFileName && <span className="selected-file-name">{digestFileName}</span>}
          </>
        )}
        {tab === 'symmetric' && (
          <>
            <label className="utility-select">
              {t('field.compatibilityAlgorithm')}
              <select value={symAlgorithm} onChange={(event) => { setSymAlgorithm(event.target.value as SymmetricAlgorithm); setOutput('') }}>
                <option value="AES">AES</option>
                <option value="DES">DES</option>
                <option value="SM4">SM4</option>
              </select>
            </label>
            <label className="crypto-inline-field">
              {t('field.key')}
              <input value={symKey} placeholder={symAlgorithm === 'AES' ? t('hint.aesKey') : t('hint.symmetricKey')} onChange={(event) => setSymKey(event.target.value)} />
            </label>
            {symAlgorithm === 'AES' && (
              <label className="crypto-inline-field">
                <input type="checkbox" checked={legacyAes} onChange={(event) => setLegacyAes(event.target.checked)} />
                {t('option.legacyAes')}
              </label>
            )}
          </>
        )}
        {tab === 'asymmetric' && (
          <>
            <label className="utility-select">
              {t('field.compatibilityAlgorithm')}
              <select value={asymAlgorithm} onChange={(event) => { setAsymAlgorithm(event.target.value as AsymmetricAlgorithm); setOutput('') }}>
                <option value="RSA">RSA</option>
                <option value="SM2">SM2</option>
              </select>
            </label>
            {asymAlgorithm === 'RSA' && (
              <label className="utility-select">{t('field.keySize')}<select value={rsaBits} onChange={(event) => setRsaBits(Number(event.target.value) as 2048 | 3072 | 4096)}><option value={2048}>2048</option><option value={3072}>3072</option><option value={4096}>4096</option></select></label>
            )}
            <button className="primary-button" type="button" disabled={asymBusy} onClick={generateAsymKeys}><KeyRound />{t(asymBusy ? 'action.generating' : 'action.generateKeys')}</button>
          </>
        )}
        {tab === 'base' && <label className="utility-select">{t('field.encoding')}<select value={baseAlgorithm} onChange={(event) => setBaseAlgorithm(event.target.value as BaseAlgorithm)}><option value="base64">Base64</option><option value="base32">Base32</option></select></label>}
      </section>

      {tab === 'asymmetric' && (
        <section className="crypto-key-pair">
          <label>
            {t('field.public')}
            <textarea value={publicKey} spellCheck={false} onChange={(event) => setPublicKey(event.target.value)} />
          </label>
          <label>
            {t('field.private')}
            <textarea value={privateKey} spellCheck={false} onChange={(event) => setPrivateKey(event.target.value)} />
          </label>
        </section>
      )}

      {tab === 'digest' ? (
        <div className="digest-panel">
          <CodeEditor ariaLabel={t('digest.input')} value={digestInput} onChange={setDigestInput} className="utility-code-editor" lineWrapping />
          <label className="digest-output"><span>{t('digest.result')}</span><output>{digestOutput || '—'}</output><button type="button" aria-label={t('action.copy')} disabled={!digestOutput} onClick={() => void copyRandom(digestOutput)}><Copy /></button></label>
        </div>
      ) : tab === 'random' ? (
        <div className="random-generator">
          <label className="random-length">{t('field.length')}<input type="number" min={1} max={4096} value={randomLength} onChange={(event) => setRandomLength(Number(event.target.value))} onBlur={() => {
            const nextLength = Math.min(4096, Math.max(1, Math.round(randomLength) || 1))
            setRandomLength(nextLength)
            if (settingsReady && nextLength !== settings.tools.randomStringLength) void saveSettings({ ...settings, tools: { ...settings.tools, randomStringLength: nextLength } })
          }} /></label>
          {([
            ['uuid', t('random.uuid'), uuid],
            ['digits', t('random.digits'), digits],
            ['string', t('random.string'), randomText],
            ['password', t('random.password'), password]
          ] as const).map(([kind, label, value]) => (
            <section className="random-row" key={kind}>
              <span>{label}</span>
              <output>{value || '—'}</output>
              <button type="button" aria-label={t('action.copy')} disabled={!value} onClick={() => void copyRandom(value)}><Copy /></button>
              <button type="button" onClick={() => generateRandomValue(kind)}><RefreshCw />{t('action.generate')}</button>
            </section>
          ))}
        </div>
      ) : (
      <section className="utility-editor-grid crypto-editor-grid">
        <section className="utility-editor-card">
          <header><span>{t(tab === 'symmetric' || tab === 'asymmetric' ? 'pane.aesInput' : 'pane.source')}</span></header>
          <CodeEditor
            ariaLabel={t('aria.input')}
            value={source}
            onChange={setSource}
            className="utility-code-editor"
            lineWrapping
          />
        </section>

        <section className="crypto-actions">
          {tab === 'symmetric' && (
            <>
              <button type="button" onClick={() => runSymmetric('encrypt')}><LockKeyhole />{t('action.encrypt')}</button>
              <button type="button" onClick={() => runSymmetric('decrypt')}><UnlockKeyhole />{t('action.decrypt')}</button>
            </>
          )}
          {tab === 'asymmetric' && (
            <>
              <button type="button" onClick={() => runAsymmetric('publicEncrypt')}><LockKeyhole />{t('action.publicEncrypt')}</button>
              <button type="button" onClick={() => runAsymmetric('privateDecrypt')}><UnlockKeyhole />{t('action.privateDecrypt')}</button>
              <button type="button" disabled={asymAlgorithm !== 'RSA'} onClick={() => runAsymmetric('privateEncrypt')}><LockKeyhole />{t('action.privateEncrypt')}</button>
              <button type="button" disabled={asymAlgorithm !== 'RSA'} onClick={() => runAsymmetric('publicDecrypt')}><UnlockKeyhole />{t('action.publicDecrypt')}</button>
              <button type="button" onClick={() => runAsymmetric('sign')}><KeyRound />{t('action.sign')}</button>
              <button type="button" disabled={!output} onClick={() => runAsymmetric('verify')}><ShieldCheck />{t('action.verify')}</button>
            </>
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

function tabMessageKey(tab: CryptoTab): CryptoMessageKey {
  return {
    symmetric: 'tab.symmetric', asymmetric: 'tab.asymmetric', digest: 'tab.digest', aes: 'tab.aes', rsa: 'tab.rsa', compatibility: 'tab.compatibility', base: 'tab.base', random: 'tab.random'
  }[tab] as CryptoMessageKey
}
