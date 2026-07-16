export type HostProfile = {
  id: number
  name: string
  content: string
  createTime: string
  modifiedTime: string
}

export type SaveHostProfileInput = {
  id?: number
  name: string
  content: string
}

export type SystemHostsFile = {
  path: string
  content: string
  writable: boolean
}

export type NetworkAction = 'interfaces' | 'connections' | 'ping' | 'flush-dns' | 'resolve' | 'whois'

export type NetworkCommandInput = {
  requestId: string
  action: NetworkAction
  target?: string
  timeoutMs: number
}

export type NetworkCommandResult = {
  requestId: string
  action: NetworkAction
  output: string
  durationMs: number
  errorCode?: 'ABORTED' | 'TIMEOUT' | 'PERMISSION' | 'UNSUPPORTED' | 'COMMAND_FAILED' | 'INVALID_TARGET'
}

export type EnvironmentEntry = {
  key: string
  value: string
}

export type EnvironmentSnapshot = {
  environment: EnvironmentEntry[]
  runtime: EnvironmentEntry[]
}

export type LocalAddressSnapshot = {
  ipv4: string[]
  ipv6: string[]
}

export type SystemInfoItem = {
  label: string
  value: string
}

export type SystemInfoGroup = {
  title: string
  items: SystemInfoItem[]
}

export type SystemInfoSectionId = 'system' | 'cpu' | 'memory' | 'storage' | 'network'

export type SystemInfoSnapshot = {
  collectedAt: string
  sections: Record<SystemInfoSectionId, SystemInfoGroup[]>
}
