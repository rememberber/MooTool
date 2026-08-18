export interface SystemHostsFile {
  path: string
  content: string
  writable: boolean
}

export interface HostApi {
  readSystem(): Promise<SystemHostsFile>
  writeSystem(content: string, expectedContent: string): Promise<SystemHostsFile>
  resolve(host: string): Promise<string[]>
}
