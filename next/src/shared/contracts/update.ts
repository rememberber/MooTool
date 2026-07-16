export type UpdateStatus = 'latest' | 'available'

export type UpdateCheckResult = {
  status: UpdateStatus
  currentVersion: string
  latestVersion: string
  releaseUrl: string
  releaseNotes: string
  checkedAt: string
}

export type UpdateCheckEvent =
  | { type: 'result'; result: UpdateCheckResult }
  | { type: 'error'; message: string }
