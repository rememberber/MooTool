export const aiClients = ['codex', 'claude-code', 'cursor'] as const
export type AiClient = typeof aiClients[number]
export type AiInstallMode = 'mcp' | 'skill' | 'both'
export type AiInstallRequest = { client: AiClient; mode: AiInstallMode; operation?: 'install' | 'uninstall' }
export type AiInstallPreview = {
  id: string
  files: Array<{ path: string; action: 'create' | 'update' | 'delete' | 'unchanged'; content: string }>
  configuration: string
}
export type AiInstallResult = { paths: string[]; backups: string[] }
export type AiConnectionResult = { serverName: string; tools: string[] }
export type AiComponentState = 'not-installed' | 'installed' | 'needs-repair' | 'conflict'
export type AiIntegrationStatus = { mcp: AiComponentState; skill: AiComponentState; version: string }
export type AiDataAccess = { notes: string | null; json: string | null }
export type AiDataAccessRequest = { notes: boolean; json: boolean }
