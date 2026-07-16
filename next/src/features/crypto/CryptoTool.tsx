import { ArrowLeft, ArrowRight, Copy, FileSearch, History, KeyRound, Play, RefreshCw, ShieldCheck } from 'lucide-react'
import { useState } from 'react'
import { HistoryDialog } from '@/features/history/HistoryDialog'
import { useSettings } from '@/features/settings/SettingsProvider'
import { ToolPageHeader, ToolTabs } from '@/shared/components/ToolPage'
import type { FuncHistoryRecord } from '@/shared/contracts/history'
import { useToolActions } from '@/shared/hooks/useToolActions'
import { useI18n } from '@/shared/i18n/I18nProvider'
import {
  asymmetricAlgorithms,
  asymmetricDecrypt,
  asymmetricEncrypt,
  baseAlgorithms,
  decodeBase,
  digestAlgorithms,
  digestText,
  encodeBase,
  generateAsymmetricKeyPair,
  privateEncrypt,
  publicDecrypt,
  randomDigits,
  randomPassword,
  randomString,
  randomUuid,
  signContent,
  symmetricAlgorithms,
  symmetricDecrypt,
  symmetricEncrypt,
  verifySignature,
  type AsymmetricAlgorithm,
  type BaseAlgorithm,
  type DigestAlgorithm,
  type SymmetricAlgorithm
} from './cryptoTools'

type CryptoTab = 'symmetric' | 'asymmetric' | 'digest' | 'base' | 'random'

export function CryptoTool() {
  const { t } = useI18n()
  const actions = useToolActions('crypto')
  const { settings, updateSettings } = useSettings()
  const [tab, setTab] = useState<CryptoTab>('symmetric')
  const [historyOpen, setHistoryOpen] = useState(false)

  const [symAlgorithm, setSymAlgorithm] = useState<SymmetricAlgorithm>('AES')
  const [symKey, setSymKey] = useState('1234567890abcdef')
  const [symPlain, setSymPlain] = useState('MooTool')
  const [symCipher, setSymCipher] = useState('')

  const [asymAlgorithm, setAsymAlgorithm] = useState<AsymmetricAlgorithm>('RSA')
  const [publicKey, setPublicKey] = useState('')
  const [privateKey, setPrivateKey] = useState('')
  const [asymPlain, setAsymPlain] = useState('MooTool')
  const [asymCipher, setAsymCipher] = useState('')
  const [asymBusy, setAsymBusy] = useState(false)

  const [digestAlgorithm, setDigestAlgorithm] = useState<DigestAlgorithm>('SHA-256')
  const [digestInput, setDigestInput] = useState('MooTool')
  const [digestOutput, setDigestOutput] = useState('')
  const [digestFileName, setDigestFileName] = useState('')

  const [baseAlgorithm, setBaseAlgorithm] = useState<BaseAlgorithm>('Base64')
  const [basePlain, setBasePlain] = useState('MooTool')
  const [baseCipher, setBaseCipher] = useState('')

  const [randomLength, setRandomLength] = useState(settings.tools.randomStringLength)
  const [uuid, setUuid] = useState('')
  const [digits, setDigits] = useState('')
  const [randomText, setRandomText] = useState('')
  const [password, setPassword] = useState('')

  function runSafely(operation: () => void): void {
    try { operation() } catch (error) { actions.reportError(error) }
  }

  function encryptSymmetric(): void {
    runSafely(() => {
      const output = symmetricEncrypt(symAlgorithm, symPlain, symKey)
      setSymCipher(output)
      void actions.saveHistory(`${symAlgorithm} ${t('crypto.encrypt')}`, symPlain, output, 'symmetric|encrypt')
    })
  }

  function decryptSymmetric(): void {
    runSafely(() => {
      const output = symmetricDecrypt(symAlgorithm, symCipher, symKey)
      setSymPlain(output)
      void actions.saveHistory(`${symAlgorithm} ${t('crypto.decrypt')}`, symCipher, output, 'symmetric|decrypt')
    })
  }

  function generateKeys(): void {
    setAsymBusy(true)
    window.setTimeout(() => {
      runSafely(() => {
        const pair = generateAsymmetricKeyPair(asymAlgorithm)
        setPublicKey(pair.publicKey)
        setPrivateKey(pair.privateKey)
        actions.toast.success(t('crypto.keyGenerated'))
      })
      setAsymBusy(false)
    }, 0)
  }

  function encryptAsymmetric(): void {
    runSafely(() => {
      const output = asymmetricEncrypt(asymAlgorithm, asymPlain, publicKey)
      setAsymCipher(output)
      void actions.saveHistory(`${asymAlgorithm} ${t('crypto.publicEncrypt')}`, asymPlain, output, 'asymmetric|publicEncrypt')
    })
  }

  function decryptAsymmetric(): void {
    runSafely(() => {
      const output = asymmetricDecrypt(asymAlgorithm, asymCipher, privateKey)
      setAsymPlain(output)
      void actions.saveHistory(`${asymAlgorithm} ${t('crypto.privateDecrypt')}`, asymCipher, output, 'asymmetric|privateDecrypt')
    })
  }

  function runPrivateEncrypt(): void {
    runSafely(() => {
      if (asymAlgorithm !== 'RSA') throw new Error(t('crypto.rsaOnly'))
      const output = privateEncrypt(asymPlain, privateKey)
      setAsymCipher(output)
      void actions.saveHistory(`RSA ${t('crypto.privateEncrypt')}`, asymPlain, output, 'asymmetric|privateEncrypt')
    })
  }

  function runPublicDecrypt(): void {
    runSafely(() => {
      if (asymAlgorithm !== 'RSA') throw new Error(t('crypto.rsaOnly'))
      const output = publicDecrypt(asymCipher, publicKey)
      setAsymPlain(output)
      void actions.saveHistory(`RSA ${t('crypto.publicDecrypt')}`, asymCipher, output, 'asymmetric|publicDecrypt')
    })
  }

  function sign(): void {
    runSafely(() => {
      const output = signContent(asymAlgorithm, asymPlain, privateKey, publicKey)
      setAsymCipher(output)
      void actions.saveHistory(`${asymAlgorithm} ${t('crypto.sign')}`, asymPlain, output, 'asymmetric|sign')
    })
  }

  function verify(): void {
    runSafely(() => {
      const valid = verifySignature(asymAlgorithm, asymPlain, asymCipher, publicKey)
      actions.toast[valid ? 'success' : 'error'](valid ? t('crypto.verified') : t('crypto.notVerified'))
    })
  }

  function hashText(): void {
    runSafely(() => {
      const output = digestText(digestAlgorithm, digestInput)
      setDigestOutput(output)
      setDigestFileName('')
      void actions.saveHistory(`${digestAlgorithm} ${t('crypto.digest')}`, digestInput, output, 'digest|text')
    })
  }

  async function hashFile(): Promise<void> {
    try {
      const result = await window.mootool.digestFile(digestAlgorithm)
      if (!result) return
      setDigestFileName(result.name)
      setDigestOutput(result.digest)
      void actions.saveHistory(`${digestAlgorithm} ${t('crypto.fileDigest')}`, result.path, result.digest, 'digest|file')
    } catch (error) { actions.reportError(error) }
  }

  function encode(): void {
    runSafely(() => {
      const output = encodeBase(baseAlgorithm, basePlain)
      setBaseCipher(output)
      void actions.saveHistory(`${baseAlgorithm} ${t('crypto.encode')}`, basePlain, output, 'base|encode')
    })
  }

  function decode(): void {
    runSafely(() => {
      const output = decodeBase(baseAlgorithm, baseCipher)
      setBasePlain(output)
      void actions.saveHistory(`${baseAlgorithm} ${t('crypto.decode')}`, baseCipher, output, 'base|decode')
    })
  }

  function generateRandom(kind: 'uuid' | 'digits' | 'string' | 'password'): void {
    runSafely(() => {
      const output = kind === 'uuid' ? randomUuid() : kind === 'digits' ? randomDigits(randomLength) : kind === 'string' ? randomString(randomLength) : randomPassword(randomLength)
      if (kind === 'uuid') setUuid(output)
      else if (kind === 'digits') setDigits(output)
      else if (kind === 'string') setRandomText(output)
      else setPassword(output)
      void actions.saveHistory(t(`crypto.random.${kind}` as 'crypto.random.uuid'), kind === 'uuid' ? '' : String(randomLength), output, `random|${kind}`)
      if (settings.tools.randomStringLength !== randomLength) void updateSettings({ tools: { randomStringLength: randomLength } })
    })
  }

  function applyHistory(record: FuncHistoryRecord): void {
    const [historyTab, operation] = (record.extraData ?? '').split('|') as [CryptoTab, string]
    if (historyTab) setTab(historyTab)
    if (historyTab === 'symmetric') {
      if (operation === 'decrypt') { setSymCipher(record.inputText); setSymPlain(record.outputText) } else { setSymPlain(record.inputText); setSymCipher(record.outputText) }
    } else if (historyTab === 'asymmetric') {
      if (operation?.includes('Decrypt')) { setAsymCipher(record.inputText); setAsymPlain(record.outputText) } else { setAsymPlain(record.inputText); setAsymCipher(record.outputText) }
    } else if (historyTab === 'digest') {
      setDigestInput(record.inputText); setDigestOutput(record.outputText)
    } else if (historyTab === 'base') {
      if (operation === 'decode') { setBaseCipher(record.inputText); setBasePlain(record.outputText) } else { setBasePlain(record.inputText); setBaseCipher(record.outputText) }
    } else if (historyTab === 'random') {
      if (operation === 'uuid') setUuid(record.outputText)
      else if (operation === 'digits') setDigits(record.outputText)
      else if (operation === 'string') setRandomText(record.outputText)
      else if (operation === 'password') setPassword(record.outputText)
    }
  }

  return (
    <section className="tool-page p4-tool">
      <ToolPageHeader title={t('crypto.title')} actions={<button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('common.action.history')}</button>} />
      <div className="local-tool-shell crypto-workspace">
        <ToolTabs tabs={[
          { id: 'symmetric', label: t('crypto.tab.symmetric') },
          { id: 'asymmetric', label: t('crypto.tab.asymmetric') },
          { id: 'digest', label: t('crypto.tab.digest') },
          { id: 'base', label: t('crypto.tab.base') },
          { id: 'random', label: t('crypto.tab.random') }
        ]} active={tab} onChange={setTab} />
        {tab === 'symmetric' && <SymmetricPanel algorithm={symAlgorithm} setAlgorithm={setSymAlgorithm} keyValue={symKey} setKeyValue={setSymKey} plain={symPlain} setPlain={setSymPlain} cipher={symCipher} setCipher={setSymCipher} encrypt={encryptSymmetric} decrypt={decryptSymmetric} copy={actions.copy} t={t} />}
        {tab === 'asymmetric' && <AsymmetricPanel algorithm={asymAlgorithm} setAlgorithm={setAsymAlgorithm} publicKey={publicKey} setPublicKey={setPublicKey} privateKey={privateKey} setPrivateKey={setPrivateKey} plain={asymPlain} setPlain={setAsymPlain} cipher={asymCipher} setCipher={setAsymCipher} busy={asymBusy} generate={generateKeys} encrypt={encryptAsymmetric} decrypt={decryptAsymmetric} privateEncrypt={runPrivateEncrypt} publicDecrypt={runPublicDecrypt} sign={sign} verify={verify} copy={actions.copy} t={t} />}
        {tab === 'digest' && <DigestPanel algorithm={digestAlgorithm} setAlgorithm={setDigestAlgorithm} input={digestInput} setInput={setDigestInput} output={digestOutput} fileName={digestFileName} hashText={hashText} hashFile={hashFile} copy={actions.copy} t={t} />}
        {tab === 'base' && <BasePanel algorithm={baseAlgorithm} setAlgorithm={setBaseAlgorithm} plain={basePlain} setPlain={setBasePlain} cipher={baseCipher} setCipher={setBaseCipher} encode={encode} decode={decode} copy={actions.copy} t={t} />}
        {tab === 'random' && <RandomPanel length={randomLength} setLength={setRandomLength} values={{ uuid, digits, string: randomText, password }} generate={generateRandom} copy={actions.copy} t={t} />}
      </div>
      <HistoryDialog funcType="crypto" open={historyOpen} onClose={() => setHistoryOpen(false)} onApply={() => undefined} onApplyRecord={applyHistory} />
    </section>
  )
}

type Translate = ReturnType<typeof useI18n>['t']

function SymmetricPanel(props: { algorithm: SymmetricAlgorithm; setAlgorithm: (value: SymmetricAlgorithm) => void; keyValue: string; setKeyValue: (value: string) => void; plain: string; setPlain: (value: string) => void; cipher: string; setCipher: (value: string) => void; encrypt: () => void; decrypt: () => void; copy: (value: string) => Promise<void>; t: Translate }) {
  return <div className="crypto-panel"><div className="p4-toolbar"><label className="compact-field"><span>{props.t('crypto.algorithm')}</span><select value={props.algorithm} onChange={(event) => props.setAlgorithm(event.target.value as SymmetricAlgorithm)}>{symmetricAlgorithms.map((value) => <option key={value}>{value}</option>)}</select></label><label className="compact-field compact-field--grow"><span>{props.t('crypto.key')}</span><input value={props.keyValue} spellCheck={false} onChange={(event) => props.setKeyValue(event.target.value)} /></label><span className="field-hint">{props.t('crypto.keyHint')}</span></div><div className="crypto-io-grid"><CryptoTextArea label={props.t('crypto.plainText')} value={props.plain} onChange={props.setPlain} onCopy={() => props.copy(props.plain)} /><div className="vertical-actions"><button className="primary-command" type="button" onClick={props.encrypt}><ArrowRight size={14} />{props.t('crypto.encrypt')}</button><button className="dialog-button" type="button" onClick={props.decrypt}><ArrowLeft size={14} />{props.t('crypto.decrypt')}</button></div><CryptoTextArea label={props.t('crypto.cipherText')} value={props.cipher} onChange={props.setCipher} onCopy={() => props.copy(props.cipher)} /></div></div>
}

function AsymmetricPanel(props: { algorithm: AsymmetricAlgorithm; setAlgorithm: (value: AsymmetricAlgorithm) => void; publicKey: string; setPublicKey: (value: string) => void; privateKey: string; setPrivateKey: (value: string) => void; plain: string; setPlain: (value: string) => void; cipher: string; setCipher: (value: string) => void; busy: boolean; generate: () => void; encrypt: () => void; decrypt: () => void; privateEncrypt: () => void; publicDecrypt: () => void; sign: () => void; verify: () => void; copy: (value: string) => Promise<void>; t: Translate }) {
  return <div className="crypto-panel crypto-panel--asymmetric"><div className="p4-toolbar"><label className="compact-field"><span>{props.t('crypto.algorithm')}</span><select value={props.algorithm} onChange={(event) => props.setAlgorithm(event.target.value as AsymmetricAlgorithm)}>{asymmetricAlgorithms.map((value) => <option key={value}>{value}</option>)}</select></label><button className="primary-command" type="button" disabled={props.busy} onClick={props.generate}><KeyRound size={14} />{props.busy ? props.t('common.processing') : props.t('crypto.generateKeyPair')}</button></div><div className="crypto-key-grid"><CryptoTextArea label={props.t('crypto.publicKey')} value={props.publicKey} onChange={props.setPublicKey} onCopy={() => props.copy(props.publicKey)} /><CryptoTextArea label={props.t('crypto.privateKey')} value={props.privateKey} onChange={props.setPrivateKey} onCopy={() => props.copy(props.privateKey)} /></div><div className="crypto-data-grid"><CryptoTextArea label={props.t('crypto.plainText')} value={props.plain} onChange={props.setPlain} onCopy={() => props.copy(props.plain)} /><CryptoTextArea label={props.t('crypto.cipherOrSignature')} value={props.cipher} onChange={props.setCipher} onCopy={() => props.copy(props.cipher)} /></div><div className="crypto-action-row"><button className="dialog-button" type="button" onClick={props.encrypt}>{props.t('crypto.publicEncrypt')}</button><button className="dialog-button" type="button" onClick={props.decrypt}>{props.t('crypto.privateDecrypt')}</button><button className="dialog-button" type="button" disabled={props.algorithm !== 'RSA'} onClick={props.privateEncrypt}>{props.t('crypto.privateEncrypt')}</button><button className="dialog-button" type="button" disabled={props.algorithm !== 'RSA'} onClick={props.publicDecrypt}>{props.t('crypto.publicDecrypt')}</button><button className="dialog-button" type="button" onClick={props.sign}>{props.t('crypto.sign')}</button><button className="dialog-button" type="button" onClick={props.verify}><ShieldCheck size={14} />{props.t('crypto.verify')}</button></div></div>
}

function DigestPanel(props: { algorithm: DigestAlgorithm; setAlgorithm: (value: DigestAlgorithm) => void; input: string; setInput: (value: string) => void; output: string; fileName: string; hashText: () => void; hashFile: () => Promise<void>; copy: (value: string) => Promise<void>; t: Translate }) {
  return <div className="crypto-panel digest-panel"><div className="p4-toolbar"><label className="compact-field"><span>{props.t('crypto.algorithm')}</span><select value={props.algorithm} onChange={(event) => props.setAlgorithm(event.target.value as DigestAlgorithm)}>{digestAlgorithms.map((value) => <option key={value}>{value}</option>)}</select></label><button className="primary-command" type="button" onClick={props.hashText}><Play size={14} />{props.t('crypto.textDigest')}</button><button className="dialog-button" type="button" onClick={() => { void props.hashFile() }}><FileSearch size={14} />{props.t('crypto.fileDigest')}</button>{props.fileName && <span className="selected-file-name">{props.fileName}</span>}</div><CryptoTextArea label={props.t('crypto.digestInput')} value={props.input} onChange={props.setInput} /><label className="digest-output"><span>{props.t('crypto.digestResult')}</span><output>{props.output || '—'}</output><button className="toolbar-button toolbar-button--icon" type="button" aria-label={props.t('common.action.copy')} onClick={() => { void props.copy(props.output) }}><Copy size={14} /></button></label></div>
}

function BasePanel(props: { algorithm: BaseAlgorithm; setAlgorithm: (value: BaseAlgorithm) => void; plain: string; setPlain: (value: string) => void; cipher: string; setCipher: (value: string) => void; encode: () => void; decode: () => void; copy: (value: string) => Promise<void>; t: Translate }) {
  return <div className="crypto-panel"><div className="p4-toolbar"><label className="compact-field"><span>{props.t('crypto.algorithm')}</span><select value={props.algorithm} onChange={(event) => props.setAlgorithm(event.target.value as BaseAlgorithm)}>{baseAlgorithms.map((value) => <option key={value}>{value}</option>)}</select></label></div><div className="crypto-io-grid"><CryptoTextArea label={props.t('crypto.plainText')} value={props.plain} onChange={props.setPlain} onCopy={() => props.copy(props.plain)} /><div className="vertical-actions"><button className="primary-command" type="button" onClick={props.encode}><ArrowRight size={14} />{props.t('crypto.encode')}</button><button className="dialog-button" type="button" onClick={props.decode}><ArrowLeft size={14} />{props.t('crypto.decode')}</button></div><CryptoTextArea label={props.algorithm} value={props.cipher} onChange={props.setCipher} onCopy={() => props.copy(props.cipher)} /></div></div>
}

function RandomPanel(props: { length: number; setLength: (value: number) => void; values: { uuid: string; digits: string; string: string; password: string }; generate: (kind: 'uuid' | 'digits' | 'string' | 'password') => void; copy: (value: string) => Promise<void>; t: Translate }) {
  const rows = [
    { id: 'uuid' as const, label: props.t('crypto.random.uuid'), value: props.values.uuid, length: false },
    { id: 'digits' as const, label: props.t('crypto.random.digits'), value: props.values.digits, length: true },
    { id: 'string' as const, label: props.t('crypto.random.string'), value: props.values.string, length: true },
    { id: 'password' as const, label: props.t('crypto.random.password'), value: props.values.password, length: true }
  ]
  return <div className="random-generator"><label className="random-length"><span>{props.t('crypto.length')}</span><input type="number" min={1} max={4096} value={props.length} onChange={(event) => props.setLength(Number(event.target.value))} /></label>{rows.map((row) => <section className="random-row" key={row.id}><span>{row.label}</span><output>{row.value || '—'}</output><button className="toolbar-button toolbar-button--icon" type="button" aria-label={props.t('common.action.copy')} disabled={!row.value} onClick={() => { void props.copy(row.value) }}><Copy size={14} /></button><button className="dialog-button" type="button" onClick={() => props.generate(row.id)}><RefreshCw size={14} />{props.t('crypto.generate')}</button></section>)}</div>
}

function CryptoTextArea({ label, value, onChange, onCopy }: { label: string; value: string; onChange: (value: string) => void; onCopy?: () => void }) {
  return <label className="crypto-textarea"><span>{label}{onCopy && <button type="button" aria-label={`Copy ${label}`} onClick={onCopy}><Copy size={13} /></button>}</span><textarea value={value} spellCheck={false} onChange={(event) => onChange(event.target.value)} /></label>
}
