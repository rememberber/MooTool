import { ArrowDown, ArrowUp, Equal, History, RotateCcw } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import {
  CalculatorToolError,
  type CalculatorToolErrorCode,
  combination,
  convertBase,
  evaluateExpression,
  greatestCommonDivisor,
  hexFloatToDecimal,
  leastCommonMultiple,
  permutation
} from './calculator'
import { calculatorMessages } from './calculatorMessages'
import { normalizeCalculatorDraft, readCalculatorDraft, type CalculatorDraft } from './calculatorDraft'
import { readStoredToolDraft, writeStoredToolDraft } from '../toolDraft'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { localDataApi } from '../../platform/api/localDataApi'
import { FuncHistoryDialog } from '../history/FuncHistoryDialog'

type CalculatorMessageKey = LocalizedMessageKey<typeof calculatorMessages>
export interface CalculatorErrorState { code?: CalculatorToolErrorCode; raw?: string; values?: Record<string, string | number> }

export interface CalculatorSessionState extends CalculatorDraft {
  error: CalculatorErrorState | null
}

export function CalculatorPage({ onStateChange }: {
  onStateChange?(state: CalculatorSessionState): void
}) {
  const { t } = useLocalizedMessages(calculatorMessages)
  const [draft, setDraft] = useState(readCalculatorDraft)
  const [draftReady, setDraftReady] = useState(false)
  const draftRevision = useRef(0)
  const [error, setError] = useState<CalculatorErrorState | null>(null)
  const [historyOpen, setHistoryOpen] = useState(false)
  const {
    expression, result, decimal, hex, binary, floatHex, floatDecimal, floatBits, floatByteOrder,
    gcdFirst, gcdSecond, lcmFirst, lcmSecond, permutationN, permutationM, combinationN, combinationM, log
  } = draft
  const floatBitCount = floatBits === '64' ? 64 : 32
  const errorText = error ? error.code ? t(`error.${error.code}`, error.values) : error.raw || t('error.unknown') : ''

  useEffect(() => {
    let alive = true
    void readStoredToolDraft('calculator').then((raw) => {
      if (!alive || draftRevision.current !== 0 || raw === null) return
      try {
        setDraft(normalizeCalculatorDraft(JSON.parse(raw)))
      } catch {
        // Keep the defaults when the stored document is not JSON.
      }
    }).finally(() => {
      if (alive) setDraftReady(true)
    })
    return () => { alive = false }
  }, [])

  useEffect(() => {
    if (!draftReady) return
    writeStoredToolDraft('calculator', JSON.stringify(draft))
  }, [draft, draftReady])

  useEffect(() => {
    onStateChange?.({ ...draft, error })
  }, [draft, error, onStateChange])

  function setField<K extends keyof CalculatorDraft>(key: K, value: CalculatorDraft[K]): void {
    draftRevision.current += 1
    setDraft((current) => ({ ...current, [key]: value }))
  }

  function run(summary: string, input: string, operation: () => string, apply?: (output: string) => Partial<CalculatorDraft>): void {
    draftRevision.current += 1
    try {
      const output = operation()
      setError(null)
      setDraft((current) => ({
        ...current,
        ...apply?.(output),
        result: output,
        log: [`${summary}: ${input} = ${output}`, ...current.log].slice(0, 12)
      }))
      void localDataApi.saveFuncHistory({ funcType: 'calculator', summary, inputText: input, outputText: output })
    } catch (cause) {
      setError(cause instanceof CalculatorToolError
        ? { code: cause.code, values: cause.values }
        : { raw: cause instanceof Error ? cause.message : t('error.unknown') })
    }
  }

  function evaluate(): void {
    run('expression', expression, () => evaluateExpression(expression))
  }

  return (
    <section className="tool-page p3-tool">
      <header className="tool-page__header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <button className="toolbar-button" type="button" onClick={() => setField('log', [])}>
          <RotateCcw size={14} />{t('action.clearHistory')}
        </button>
      </header>

      <div className="local-tool-shell calculator-workspace">
      <ResizableColumns id="calculator-panels" className="calculator-layout" initialPrimary={1} defaultSizes={[1, 1]} minPrimary={360} minSecondary={320}>
        <div className="calculator-controls">
          <section className="calculator-section calculator-arithmetic">
            <div className="embedded-tool-heading">
              <h2>{t('arithmetic.title')}</h2>
              <button className="toolbar-button" type="button" onClick={() => setHistoryOpen(true)}><History size={14} />{t('action.history')}</button>
            </div>
            <div className="calculator-expression-row">
              <input
                id="calculator-expression"
                aria-label={t('expression.label')}
                value={expression}
                onChange={(event) => setField('expression', event.target.value)}
                onKeyDown={(event) => { if (event.key === 'Enter') evaluate() }}
              />
              <button className="primary-command calculator-evaluate-button" type="button" aria-label={t('action.calculate')} onClick={evaluate}>
                <Equal size={16} />
              </button>
            </div>
            {error && <p className="calculator-hint error-message" role="alert">{errorText}</p>}
          </section>

          <section className="calculator-section calculator-base-panel">
            <h2>{t('base.title')}</h2>
            <p className="calculator-hint">{t('base.hint')}</p>
            <div className="calculator-base-grid">
              <label htmlFor="calculator-hex">{t('base.hex')}</label>
              <input id="calculator-hex" value={hex} onChange={(event) => setField('hex', event.target.value)} />
              <div className="calculator-base-actions">
                <button className="panel-command" type="button" aria-label="HEX → DEC" onClick={() => run('HEX → DEC', hex, () => convertBase(hex, 16, 10), (value) => ({ decimal: value }))}><ArrowDown size={15} />{t('action.convert')}</button>
                <button className="panel-command" type="button" aria-label="DEC → HEX" onClick={() => run('DEC → HEX', decimal, () => convertBase(decimal, 10, 16), (value) => ({ hex: value }))}><ArrowUp size={15} />{t('action.convert')}</button>
              </div>
              <label htmlFor="calculator-decimal">{t('base.decimal')}</label>
              <input id="calculator-decimal" value={decimal} onChange={(event) => setField('decimal', event.target.value)} />
              <div className="calculator-base-actions">
                <button className="panel-command" type="button" aria-label="DEC → BIN" onClick={() => run('DEC → BIN', decimal, () => convertBase(decimal, 10, 2), (value) => ({ binary: value }))}><ArrowDown size={15} />{t('action.convert')}</button>
                <button className="panel-command" type="button" aria-label="BIN → DEC" onClick={() => run('BIN → DEC', binary, () => convertBase(binary, 2, 10), (value) => ({ decimal: value }))}><ArrowUp size={15} />{t('action.convert')}</button>
              </div>
              <label htmlFor="calculator-binary">{t('base.binary')}</label>
              <input id="calculator-binary" value={binary} onChange={(event) => setField('binary', event.target.value)} />
            </div>
          </section>

          <section className="calculator-section calculator-float-panel">
            <h2>{t('float.title')}</h2>
            <div className="calculator-float-options">
              <label className="utility-select">
                <span>{t('float.format')}</span>
                <select aria-label={t('float.format')} value={floatBits} onChange={(event) => setField('floatBits', event.target.value === '64' ? '64' : '32')}>
                  <option value="32">{t('float.bits32')}</option>
                  <option value="64">{t('float.bits64')}</option>
                </select>
              </label>
              <label className="utility-select">
                <span>{t('float.byteOrder')}</span>
                <select aria-label={t('float.byteOrder')} value={floatByteOrder} onChange={(event) => setField('floatByteOrder', event.target.value === 'little' ? 'little' : 'big')}>
                  <option value="big">{t('float.bigEndian')}</option>
                  <option value="little">{t('float.littleEndian')}</option>
                </select>
              </label>
            </div>
            <p className="calculator-hint" id="calculator-float-hint">{t('float.hint', { digits: floatBitCount / 4 })}</p>
            <div className="calculator-base-grid">
              <label htmlFor="calculator-float-hex">{t('base.hex')}</label>
              <input id="calculator-float-hex" aria-describedby="calculator-float-hint" placeholder={floatBitCount === 32 ? '3F800000' : '3FF0000000000000'} value={floatHex} onChange={(event) => setField('floatHex', event.target.value)} />
              <div className="calculator-base-actions">
                <button className="panel-command" type="button" aria-label="IEEE 754 HEX → DEC" onClick={() => run(`IEEE 754 ${floatBitCount} (${floatByteOrder}) HEX → DEC`, floatHex, () => hexFloatToDecimal(floatHex, floatBitCount, floatByteOrder), (value) => ({ floatDecimal: value }))}><ArrowDown size={15} />{t('action.convert')}</button>
              </div>
              <label htmlFor="calculator-float-decimal">{t('base.decimal')}</label>
              <input id="calculator-float-decimal" value={floatDecimal} readOnly />
            </div>
          </section>

          <IntegerOperation
            title={t('integer.gcd')}
            values={[gcdFirst, gcdSecond]}
            onChange={([first, second]) => { draftRevision.current += 1; setDraft((current) => ({ ...current, gcdFirst: first, gcdSecond: second })) }}
            action={t('integer.calculateGcd')}
            onRun={() => run(t('integer.gcd'), `${gcdFirst}, ${gcdSecond}`, () => greatestCommonDivisor(gcdFirst, gcdSecond))}
          />
          <IntegerOperation
            title={t('integer.lcm')}
            values={[lcmFirst, lcmSecond]}
            onChange={([first, second]) => { draftRevision.current += 1; setDraft((current) => ({ ...current, lcmFirst: first, lcmSecond: second })) }}
            action={t('integer.calculateLcm')}
            onRun={() => run(t('integer.lcm'), `${lcmFirst}, ${lcmSecond}`, () => leastCommonMultiple(lcmFirst, lcmSecond))}
          />
          <IntegerOperation
            title={t('integer.permutation')}
            labels={['n', 'm']}
            values={[permutationN, permutationM]}
            onChange={([first, second]) => { draftRevision.current += 1; setDraft((current) => ({ ...current, permutationN: first, permutationM: second })) }}
            action="A(n,m)"
            onRun={() => run(t('integer.permutation'), `${permutationN}, ${permutationM}`, () => permutation(permutationN, permutationM))}
          />
          <IntegerOperation
            title={t('integer.combination')}
            labels={['n', 'm']}
            values={[combinationN, combinationM]}
            onChange={([first, second]) => { draftRevision.current += 1; setDraft((current) => ({ ...current, combinationN: first, combinationM: second })) }}
            action="C(n,m)"
            onRun={() => run(t('integer.combination'), `${combinationN}, ${combinationM}`, () => combination(combinationN, combinationM))}
          />
        </div>

        <section className="calculator-output-panel">
          <output className="calculator-result" aria-label={t('result.title')}>{result}</output>
          <div className="calculator-log">
            <h2>{t('history.title')}</h2>
            {log.length === 0
              ? <p className="empty-state">{t('history.empty')}</p>
              : log.map((item, index) => <p key={`${index}-${item}`}>{item}</p>)}
          </div>
        </section>
      </ResizableColumns>
      </div>
      {historyOpen && <FuncHistoryDialog funcType="calculator" onClose={() => setHistoryOpen(false)} onApply={(record) => {
        draftRevision.current += 1
        setDraft((current) => ({ ...current, expression: record.inputText, result: record.outputText }))
        setHistoryOpen(false)
      }} />}
    </section>
  )
}

function IntegerOperation({ title, labels, values, onChange, action, onRun }: {
  title: string
  labels?: [string, string]
  values: string[]
  onChange(values: string[]): void
  action: string
  onRun(): void
}) {
  const { t } = useLocalizedMessages(calculatorMessages)
  const resolvedLabels = labels ?? [t('integer.value1'), t('integer.value2')]
  return (
    <section className="calculator-section calculator-operation-panel">
      <h2>{title}</h2>
      <div className="calculator-operation-row">
        {values.map((value, index) => (
          <label className="labeled-input" key={resolvedLabels[index]}>
            <span>{resolvedLabels[index]}</span>
            <input
              value={value}
              onChange={(event) => onChange(values.map((current, itemIndex) => itemIndex === index ? event.target.value : current))}
            />
          </label>
        ))}
        <button className="panel-command" type="button" onClick={onRun}>{action}</button>
      </div>
    </section>
  )
}
