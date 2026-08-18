export type OperationStatus = 'info' | 'success' | 'error'

export interface OperationHistory {
  id: string
  toolId: string
  action: string
  summary: string
  status: OperationStatus
  createdAt: number
}

export interface HistoryApi {
  record(entry: OperationHistory, historyLimit: number): Promise<OperationHistory>
  list(limit: number): Promise<OperationHistory[]>
  delete(id: string): Promise<boolean>
  clear(): Promise<number>
  subscribe(listener: () => void): Promise<() => void>
}
