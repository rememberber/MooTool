import {
  Bot,
  Box,
  CircleAlert,
  Cpu,
  FileCode2,
  FolderOpen,
  PlugZap,
  RefreshCw,
  ShieldCheck,
  Sparkles
} from 'lucide-react'
import { useEffect, useState, type ReactNode } from 'react'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import type {
  AiArtifactKind,
  AiDiagnosticCode,
  AiDoctorSnapshot,
  AiHealthStatus
} from '@/shared/contracts/ai'
import { aiClientNames } from '@/shared/contracts/ai'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'

const artifactKinds: AiArtifactKind[] = ['instruction', 'skill', 'mcpServer']

const artifactLabels: Record<AiArtifactKind, MessageKey> = {
  clientConfig: 'ai.inventory.configs',
  instruction: 'ai.inventory.instructions',
  skill: 'ai.inventory.skills',
  mcpServer: 'ai.inventory.mcp'
}

const artifactIcons: Record<AiArtifactKind, typeof FileCode2> = {
  clientConfig: FileCode2,
  instruction: FileCode2,
  skill: Sparkles,
  mcpServer: PlugZap
}

const diagnosticLabels: Record<AiDiagnosticCode, MessageKey> = {
  PROJECT_NOT_SELECTED: 'ai.diagnostic.PROJECT_NOT_SELECTED',
  CLIENT_CONFIG_WITHOUT_BINARY: 'ai.diagnostic.CLIENT_CONFIG_WITHOUT_BINARY',
  OLLAMA_NOT_RUNNING: 'ai.diagnostic.OLLAMA_NOT_RUNNING',
  SKILL_MISSING_ENTRY: 'ai.diagnostic.SKILL_MISSING_ENTRY',
  SYMLINK_SKIPPED: 'ai.diagnostic.SYMLINK_SKIPPED',
  UNREADABLE_PATH: 'ai.diagnostic.UNREADABLE_PATH',
  SCAN_LIMIT_REACHED: 'ai.diagnostic.SCAN_LIMIT_REACHED',
  MCP_CONFIG_INVALID: 'ai.diagnostic.MCP_CONFIG_INVALID',
  PLAINTEXT_SECRET_RISK: 'ai.diagnostic.PLAINTEXT_SECRET_RISK',
  SKILL_ENTRY_INVALID: 'ai.diagnostic.SKILL_ENTRY_INVALID',
  SKILL_REFERENCE_MISSING: 'ai.diagnostic.SKILL_REFERENCE_MISSING',
  SKILL_DANGEROUS_PATTERN: 'ai.diagnostic.SKILL_DANGEROUS_PATTERN',
  SKILL_ENTRY_TOO_LARGE: 'ai.diagnostic.SKILL_ENTRY_TOO_LARGE',
  INSTRUCTION_DUPLICATE: 'ai.diagnostic.INSTRUCTION_DUPLICATE',
  INSTRUCTION_CONFLICT: 'ai.diagnostic.INSTRUCTION_CONFLICT',
  INSTRUCTION_TOO_LARGE: 'ai.diagnostic.INSTRUCTION_TOO_LARGE'
}

export function AiOverviewTool() {
  const { t } = useI18n()
  const [snapshot, setSnapshot] = useState<AiDoctorSnapshot | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function scan(projectRoot = snapshot?.projectRoot): Promise<void> {
    setLoading(true)
    setError('')
    try {
      setSnapshot(await window.mootool.scanAiEnvironment(projectRoot ? { projectRoot } : undefined))
    } catch (scanError) {
      setError(scanError instanceof Error ? scanError.message : t('ai.scanFailed'))
    } finally {
      setLoading(false)
    }
  }

  async function chooseProject(): Promise<void> {
    const selected = await window.mootool.chooseDirectory(snapshot?.projectRoot)
    if (selected) await scan(selected)
  }

  useEffect(() => {
    void scan()
  }, [])

  return (
    <section className="tool-page p5-tool ai-overview-tool">
      <ToolPageHeader
        title={t('ai.title')}
        actions={(
          <>
            <span className="ai-readonly-badge"><ShieldCheck size={14} />{t('ai.readOnly')}</span>
            <button className="toolbar-button" type="button" onClick={() => { void chooseProject() }}>
              <FolderOpen size={14} />{snapshot?.projectRoot ? t('ai.changeProject') : t('ai.chooseProject')}
            </button>
            <button className="toolbar-button" type="button" disabled={loading} onClick={() => { void scan() }}>
              <RefreshCw size={14} className={loading ? 'spin' : undefined} />{t('common.refresh')}
            </button>
          </>
        )}
      />

      <div className="local-tool-shell ai-doctor-shell">
        <div className="ai-doctor-scroll">
          <header className="ai-doctor-context">
            <div>
              <span>{t('ai.project')}</span>
              <strong title={snapshot?.projectRoot}>{snapshot?.projectRoot ?? t('ai.userScope')}</strong>
            </div>
            <time>{snapshot ? t('ai.lastScan', { time: new Date(snapshot.scannedAt).toLocaleTimeString() }) : t('ai.loading')}</time>
          </header>

          {error && <div className="ai-error" role="alert"><CircleAlert size={16} /><span>{t('ai.scanFailed')}: {error}</span></div>}
          {loading && !snapshot ? <div className="history-empty">{t('ai.loading')}</div> : snapshot && (
            <>
              <section className="ai-summary-grid" aria-label={t('ai.section.summary')}>
                <SummaryCard icon={<Bot />} label={t('ai.summary.clients')} value={snapshot.summary.detectedClients} />
                <SummaryCard icon={<Cpu />} label={t('ai.summary.runtimes')} value={snapshot.summary.detectedRuntimes} />
                <SummaryCard icon={<Box />} label={t('ai.summary.artifacts')} value={snapshot.summary.artifacts} />
                <SummaryCard icon={<CircleAlert />} label={t('ai.summary.issues')} value={snapshot.summary.diagnostics} tone={snapshot.summary.diagnostics ? 'warning' : 'healthy'} />
              </section>

              <div className="ai-section-grid">
                <DoctorSection title={t('ai.section.clients')}>
                  <div className="ai-entity-list">
                    {snapshot.clients.map((client) => (
                      <article className="ai-entity-row" key={client.id}>
                        <span className="ai-entity-icon"><Bot size={18} /></span>
                        <div className="ai-entity-main">
                          <strong>{client.name}</strong>
                          <span title={client.binaryPath ?? client.configRoot}>{client.binaryPath ?? client.configRoot}</span>
                        </div>
                        <div className="ai-entity-meta">
                          <HealthBadge status={client.status} />
                          <span>{t('ai.client.artifacts', { count: String(client.artifactCount) })}</span>
                        </div>
                      </article>
                    ))}
                  </div>
                </DoctorSection>

                <DoctorSection title={t('ai.section.runtimes')}>
                  <div className="ai-entity-list">
                    {snapshot.runtimes.map((runtime) => (
                      <article className="ai-runtime-card" key={runtime.id}>
                        <div className="ai-entity-row">
                          <span className="ai-entity-icon"><Cpu size={18} /></span>
                          <div className="ai-entity-main">
                            <strong>{runtime.name}{runtime.version ? ` ${runtime.version}` : ''}</strong>
                            <span>{runtime.endpoint}</span>
                          </div>
                          <HealthBadge status={runtime.status} />
                        </div>
                        {runtime.models.length > 0 ? (
                          <div className="ai-model-list">
                            {runtime.models.map((model) => (
                              <div key={`${model.name}-${model.digest}`}>
                                <span className={model.running ? 'ai-model-dot ai-model-dot--running' : 'ai-model-dot'} />
                                <strong>{model.name}</strong>
                                <span>{[model.parameterSize, model.quantization, formatBytes(model.sizeBytes)].filter(Boolean).join(' · ')}</span>
                              </div>
                            ))}
                          </div>
                        ) : <div className="ai-inline-empty">{t('ai.empty.models')}</div>}
                      </article>
                    ))}
                  </div>
                </DoctorSection>
              </div>

              <DoctorSection title={t('ai.section.inventory')}>
                <div className="ai-inventory-grid">
                  {artifactKinds.map((kind) => {
                    const Icon = artifactIcons[kind]
                    const artifacts = snapshot.artifacts.filter((artifact) => artifact.kind === kind)
                    return (
                      <article key={kind}>
                        <header><Icon size={16} /><strong>{t(artifactLabels[kind])}</strong><span>{artifacts.length}</span></header>
                        {artifacts.length > 0 ? (
                          <ul>{artifacts.slice(0, 8).map((artifact) => <li key={artifact.id}><span>{artifact.name}</span><small title={artifact.path}>{artifact.scope === 'project' ? t('ai.scope.project') : t('ai.scope.user')} · {aiClientNames[artifact.clientId]}</small></li>)}</ul>
                        ) : <div className="ai-inline-empty">{t('ai.empty.inventory')}</div>}
                        {artifacts.length > 8 && <footer>{t('ai.moreItems', { count: String(artifacts.length - 8) })}</footer>}
                      </article>
                    )
                  })}
                </div>
              </DoctorSection>

              <DoctorSection title={t('ai.section.diagnostics')}>
                {snapshot.diagnostics.length > 0 ? (
                  <div className="ai-diagnostic-list">
                    {snapshot.diagnostics.map((diagnostic) => (
                      <article className={`ai-diagnostic ai-diagnostic--${diagnostic.severity}`} key={diagnostic.id}>
                        <CircleAlert size={16} />
                        <div><strong>{t(diagnosticLabels[diagnostic.code])}</strong>{diagnostic.path && <span title={diagnostic.path}>{diagnostic.path}</span>}</div>
                      </article>
                    ))}
                  </div>
                ) : <div className="ai-healthy-empty"><ShieldCheck size={18} />{t('ai.empty.diagnostics')}</div>}
              </DoctorSection>
            </>
          )}
        </div>
      </div>
    </section>
  )

  function HealthBadge({ status }: { status: AiHealthStatus }) {
    return <span className={`ai-health ai-health--${status}`}>{t(`ai.status.${status}` as MessageKey)}</span>
  }
}

function SummaryCard({ icon, label, value, tone }: { icon: ReactNode; label: string; value: number; tone?: 'healthy' | 'warning' }) {
  return <article className={`ai-summary-card${tone ? ` ai-summary-card--${tone}` : ''}`}><span>{icon}</span><div><strong>{value}</strong><small>{label}</small></div></article>
}

function DoctorSection({ title, children }: { title: string; children: ReactNode }) {
  return <section className="ai-doctor-section"><h2>{title}</h2>{children}</section>
}

function formatBytes(bytes: number): string {
  if (bytes < 1_000_000) return `${Math.round(bytes / 1_000)} KB`
  if (bytes < 1_000_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`
  return `${(bytes / 1_000_000_000).toFixed(1)} GB`
}
