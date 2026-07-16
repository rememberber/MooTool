import { execFile, spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import { access, readFile, unlink, writeFile } from 'node:fs/promises'
import { constants } from 'node:fs'
import { lookup } from 'node:dns/promises'
import { createConnection } from 'node:net'
import { networkInterfaces } from 'node:os'
import { join } from 'node:path'
import si from 'systeminformation'
import type {
  EnvironmentEntry,
  EnvironmentSnapshot,
  NetworkAction,
  NetworkCommandInput,
  NetworkCommandResult,
  LocalAddressSnapshot,
  SystemHostsFile,
  SystemInfoGroup,
  SystemInfoSnapshot
} from '../../src/shared/contracts/system'

const maxCommandOutput = 2 * 1024 * 1024

export class SystemService {
  private readonly processes = new Map<string, ChildProcessWithoutNullStreams>()

  constructor(private readonly tempDirectory: string) {}

  async readHosts(): Promise<SystemHostsFile> {
    const path = hostsPath()
    const content = await readFile(path, 'utf8')
    let writable = true
    try {
      await access(path, constants.W_OK)
    } catch {
      writable = false
    }
    return { path, content, writable }
  }

  async writeHosts(content: string): Promise<SystemHostsFile> {
    const normalized = normalizeHostsContent(content)
    const path = hostsPath()
    try {
      await writeFile(path, normalized, 'utf8')
    } catch (error) {
      if (!isPermissionError(error)) throw error
      await this.writeHostsElevated(path, normalized)
    }
    return this.readHosts()
  }

  async runNetwork(input: NetworkCommandInput): Promise<NetworkCommandResult> {
    const startedAt = Date.now()
    try {
      const output = await this.executeNetworkAction(input)
      return { requestId: input.requestId, action: input.action, output, durationMs: Date.now() - startedAt }
    } catch (error) {
      return {
        requestId: input.requestId,
        action: input.action,
        output: errorMessage(error),
        durationMs: Date.now() - startedAt,
        errorCode: classifySystemError(error)
      }
    } finally {
      this.processes.delete(input.requestId)
    }
  }

  cancel(requestId: string): boolean {
    const process = this.processes.get(requestId)
    if (!process) return false
    process.kill('SIGTERM')
    this.processes.delete(requestId)
    return true
  }

  getEnvironment(): EnvironmentSnapshot {
    const environment = Object.entries(process.env)
      .map(([key, value]) => ({ key, value: value ?? '' }))
      .sort(compareEntries)
    const runtime = [
      ['process.version', process.version],
      ['process.versions.electron', process.versions.electron ?? ''],
      ['process.versions.chrome', process.versions.chrome ?? ''],
      ['process.versions.node', process.versions.node],
      ['process.execPath', process.execPath],
      ['process.platform', process.platform],
      ['process.arch', process.arch],
      ['process.cwd', process.cwd()],
      ['process.locale', Intl.DateTimeFormat().resolvedOptions().locale],
      ['process.timeZone', Intl.DateTimeFormat().resolvedOptions().timeZone]
    ].map(([key, value]) => ({ key, value })).sort(compareEntries)
    return { environment, runtime }
  }

  getLocalAddresses(): LocalAddressSnapshot {
    return { ipv4: localAddresses(4), ipv6: localAddresses(6) }
  }

  async getSystemInfo(): Promise<SystemInfoSnapshot> {
    const [osInfo, system, cpu, load, memory, time, disks, fileSystems, interfaces, stats] = await Promise.all([
      si.osInfo(), si.system(), si.cpu(), si.currentLoad(), si.mem(), si.time(), si.diskLayout(), si.fsSize(), si.networkInterfaces(), si.networkStats()
    ])
    const networkStats = new Map(stats.map((entry) => [entry.iface, entry]))
    return {
      collectedAt: new Date().toISOString(),
      sections: {
        system: [group('Operating system', [
          item('Platform', osInfo.platform), item('Distribution', `${osInfo.distro} ${osInfo.release}`.trim()), item('Kernel', osInfo.kernel),
          item('Architecture', osInfo.arch), item('Host name', osInfo.hostname), item('Serial', mask(system.serial)),
          item('Manufacturer', system.manufacturer), item('Model', system.model), item('Uptime', formatDuration(time.uptime)), item('Time zone', time.timezoneName || time.timezone)
        ])],
        cpu: [group('Processor', [
          item('Manufacturer', cpu.manufacturer), item('Brand', cpu.brand), item('Vendor', cpu.vendor), item('Family', cpu.family),
          item('Model', cpu.model), item('Physical cores', cpu.physicalCores), item('Logical cores', cpu.cores),
          item('Performance cores', cpu.performanceCores), item('Efficiency cores', cpu.efficiencyCores),
          item('Base speed', `${cpu.speed} GHz`), item('Maximum speed', `${cpu.speedMax} GHz`), item('Current load', `${load.currentLoad.toFixed(1)}%`)
        ])],
        memory: [group('Physical memory', [
          item('Total', formatBytes(memory.total)), item('Used', formatBytes(memory.used)), item('Available', formatBytes(memory.available)),
          item('Active', formatBytes(memory.active)), item('Usage', memory.total ? `${(memory.used * 100 / memory.total).toFixed(1)}%` : '-'),
          item('Swap total', formatBytes(memory.swaptotal)), item('Swap used', formatBytes(memory.swapused))
        ])],
        storage: [
          ...disks.map((disk) => group(disk.name || disk.device || 'Disk', [item('Device', disk.device), item('Type', disk.type), item('Interface', disk.interfaceType), item('Vendor', disk.vendor), item('Model', disk.name), item('Serial', mask(disk.serialNum)), item('Capacity', formatBytes(disk.size))])),
          ...fileSystems.map((fs) => group(fs.mount || fs.fs, [item('Filesystem', fs.fs), item('Type', fs.type), item('Total', formatBytes(fs.size)), item('Used', formatBytes(fs.used)), item('Available', formatBytes(fs.available)), item('Usage', `${fs.use.toFixed(1)}%`)]))
        ],
        network: interfaces.map((entry) => {
          const stat = networkStats.get(entry.iface)
          return group(entry.ifaceName || entry.iface, [
            item('Interface', entry.iface), item('Type', entry.type), item('IPv4', entry.ip4), item('IPv6', entry.ip6), item('MAC', entry.mac),
            item('MTU', entry.mtu), item('Speed', entry.speed ? `${entry.speed} Mbps` : '-'), item('Status', entry.operstate),
            item('Received', stat ? formatBytes(stat.rx_bytes) : '-'), item('Sent', stat ? formatBytes(stat.tx_bytes) : '-')
          ])
        })
      }
    }
  }

  private async executeNetworkAction(input: NetworkCommandInput): Promise<string> {
    const timeoutMs = clampTimeout(input.timeoutMs)
    switch (input.action) {
      case 'resolve': {
        const target = normalizeHostTarget(input.target)
        const addresses = await lookup(target, { all: true })
        return addresses.map((entry) => `${entry.address}\tIPv${entry.family}`).join('\n')
      }
      case 'whois':
        return queryWhois(normalizeWhoisTarget(input.target), timeoutMs)
      case 'interfaces':
      case 'connections':
      case 'ping':
      case 'flush-dns': {
        const command = networkCommand(input.action, input.target)
        return this.spawnCommand(input.requestId, command.file, command.args, timeoutMs)
      }
    }
  }

  private spawnCommand(requestId: string, file: string, args: string[], timeoutMs: number): Promise<string> {
    return new Promise((resolve, reject) => {
      const child = spawn(file, args, { windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] })
      this.processes.set(requestId, child)
      let stdout = ''
      let stderr = ''
      let overflow = false
      const timer = setTimeout(() => {
        child.kill('SIGTERM')
        reject(new Error('TIMEOUT'))
      }, timeoutMs)
      timer.unref()
      child.stdout.on('data', (chunk: Buffer) => {
        stdout += chunk.toString()
        if (stdout.length > maxCommandOutput) { overflow = true; child.kill('SIGTERM') }
      })
      child.stderr.on('data', (chunk: Buffer) => {
        stderr += chunk.toString()
        if (stderr.length > maxCommandOutput) { overflow = true; child.kill('SIGTERM') }
      })
      child.on('error', (error) => { clearTimeout(timer); reject(error) })
      child.on('close', (code, signal) => {
        clearTimeout(timer)
        if (overflow) return reject(new Error('Command output exceeds 2 MB'))
        if (signal === 'SIGTERM') return reject(new Error('ABORTED'))
        if (code !== 0) return reject(new Error(stderr.trim() || `${file} exited with code ${code}`))
        resolve((stdout || stderr).trim())
      })
    })
  }

  private async writeHostsElevated(destination: string, content: string): Promise<void> {
    const temporary = join(this.tempDirectory, `mootool-hosts-${process.pid}-${Date.now()}`)
    await writeFile(temporary, content, { encoding: 'utf8', mode: 0o600 })
    try {
      if (process.platform === 'darwin') {
        const command = `/bin/cp ${shellQuote(temporary)} ${shellQuote(destination)} && /usr/bin/chmod 644 ${shellQuote(destination)} && /usr/bin/dscacheutil -flushcache`
        await execFilePromise('/usr/bin/osascript', ['-e', `do shell script ${appleScriptString(command)} with administrator privileges`], 120_000)
      } else if (process.platform === 'win32') {
        const script = `Copy-Item -LiteralPath '${psQuote(temporary)}' -Destination '${psQuote(destination)}' -Force`
        await execFilePromise('powershell.exe', ['-NoProfile', '-Command', `Start-Process powershell.exe -Verb RunAs -Wait -ArgumentList @('-NoProfile','-Command',${psString(script)})`], 120_000)
      } else {
        await execFilePromise('pkexec', ['/bin/cp', temporary, destination], 120_000)
      }
    } finally {
      await unlink(temporary).catch(() => undefined)
    }
  }
}

export function ipv4ToLong(value: string): number {
  const parts = value.trim().split('.')
  if (parts.length !== 4 || parts.some((part) => !/^\d{1,3}$/.test(part) || Number(part) > 255)) throw new Error('Invalid IPv4 address')
  return parts.reduce((result, part) => result * 256 + Number(part), 0) >>> 0
}

export function longToIpv4(value: number | string): string {
  const number = typeof value === 'string' ? Number(value.trim()) : value
  if (!Number.isSafeInteger(number) || number < 0 || number > 0xffffffff) throw new Error('Invalid IPv4 number')
  return [24, 16, 8, 0].map((shift) => Math.floor(number / 2 ** shift) % 256).join('.')
}

export function localAddresses(family: 4 | 6): string[] {
  return [...new Set(Object.values(networkInterfaces()).flatMap((entries) => entries ?? []).filter((entry) => entry.family === `IPv${family}`).map((entry) => entry.address))].sort()
}

export function normalizeHostsContent(value: string): string {
  if (typeof value !== 'string' || value.length > 2 * 1024 * 1024 || value.includes('\0')) throw new Error('Invalid hosts content')
  return `${value.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\n*$/, '')}\n`
}

function networkCommand(action: NetworkAction, target?: string): { file: string; args: string[] } {
  if (action === 'interfaces') {
    if (process.platform === 'win32') return { file: 'ipconfig.exe', args: ['/all'] }
    if (process.platform === 'darwin') return { file: '/sbin/ifconfig', args: [] }
    return { file: 'ip', args: ['-details', 'address'] }
  }
  if (action === 'connections') return process.platform === 'win32'
    ? { file: 'netstat.exe', args: ['-ano'] }
    : { file: 'netstat', args: ['-nat'] }
  if (action === 'ping') {
    const host = normalizeHostTarget(target)
    return process.platform === 'win32' ? { file: 'ping.exe', args: ['-n', '4', host] } : { file: '/sbin/ping', args: ['-c', '4', host] }
  }
  if (action === 'flush-dns') {
    if (process.platform === 'win32') return { file: 'ipconfig.exe', args: ['/flushdns'] }
    if (process.platform === 'darwin') return { file: '/usr/bin/dscacheutil', args: ['-flushcache'] }
    return { file: 'resolvectl', args: ['flush-caches'] }
  }
  throw new Error('UNSUPPORTED')
}

async function queryWhois(target: string, timeoutMs: number): Promise<string> {
  const first = await whoisServerQuery('whois.iana.org', target, timeoutMs)
  const referral = first.match(/^(?:refer|whois):\s*(\S+)/im)?.[1]
  if (!referral || referral === 'whois.iana.org') return first
  return whoisServerQuery(referral, target, timeoutMs)
}

function whoisServerQuery(server: string, target: string, timeoutMs: number): Promise<string> {
  return new Promise((resolve, reject) => {
    const socket = createConnection({ host: server, port: 43 })
    let output = ''
    const timer = setTimeout(() => { socket.destroy(); reject(new Error('TIMEOUT')) }, timeoutMs)
    timer.unref()
    socket.setEncoding('utf8')
    socket.on('connect', () => socket.write(`${target}\r\n`))
    socket.on('data', (chunk: string) => {
      output += chunk
      if (output.length > maxCommandOutput) { socket.destroy(); reject(new Error('WHOIS response exceeds 2 MB')) }
    })
    socket.on('end', () => { clearTimeout(timer); resolve(output.trim()) })
    socket.on('error', (error) => { clearTimeout(timer); reject(error) })
  })
}

function hostsPath(): string {
  return process.platform === 'win32' ? `${process.env.SystemRoot || 'C:\\Windows'}\\System32\\drivers\\etc\\hosts` : '/etc/hosts'
}

function normalizeHostTarget(value?: string): string {
  const target = value?.trim() ?? ''
  if (!target || target.length > 253 || !/^[a-zA-Z0-9.:_-]+$/.test(target)) throw new Error('INVALID_TARGET')
  return target
}

function normalizeWhoisTarget(value?: string): string {
  const target = value?.trim().toLowerCase() ?? ''
  if (!target || target.length > 253 || !/^[a-z0-9.-]+$/.test(target)) throw new Error('INVALID_TARGET')
  return target
}

function clampTimeout(value: number): number {
  return Number.isFinite(value) ? Math.max(1_000, Math.min(120_000, Math.round(value))) : 15_000
}

function compareEntries(left: EnvironmentEntry, right: EnvironmentEntry): number {
  return left.key.localeCompare(right.key)
}

function group(title: string, items: ReturnType<typeof item>[]): SystemInfoGroup {
  return { title, items: items.filter((entry) => entry.value && entry.value !== 'undefined') }
}

function item(label: string, value: unknown) {
  return { label, value: value == null || value === '' ? '-' : String(value) }
}

function formatBytes(bytes: number): string {
  if (!Number.isFinite(bytes) || bytes < 0) return '-'
  const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']
  let value = bytes
  let index = 0
  while (value >= 1024 && index < units.length - 1) { value /= 1024; index += 1 }
  return `${value.toFixed(2)} ${units[index]}`
}

function formatDuration(seconds: number): string {
  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${days}d ${hours}h ${minutes}m`
}

function mask(value: string): string {
  if (!value || value.toLowerCase() === 'unknown') return '-'
  return value.length <= 4 ? value : `${value.slice(0, 2)}****${value.slice(-2)}`
}

function classifySystemError(error: unknown): NetworkCommandResult['errorCode'] {
  const message = errorMessage(error)
  if (message.includes('ABORTED')) return 'ABORTED'
  if (message.includes('TIMEOUT')) return 'TIMEOUT'
  if (message.includes('INVALID_TARGET')) return 'INVALID_TARGET'
  if (message.includes('UNSUPPORTED') || message.includes('ENOENT')) return 'UNSUPPORTED'
  if (message.includes('EACCES') || message.includes('EPERM') || message.toLowerCase().includes('permission')) return 'PERMISSION'
  return 'COMMAND_FAILED'
}

function isPermissionError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'code' in error && (error.code === 'EACCES' || error.code === 'EPERM')
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}

function execFilePromise(file: string, args: string[], timeout: number): Promise<void> {
  return new Promise((resolve, reject) => execFile(file, args, { timeout, windowsHide: true }, (error) => error ? reject(error) : resolve()))
}

function shellQuote(value: string): string {
  return `'${value.replace(/'/g, `'"'"'`)}'`
}

function appleScriptString(value: string): string {
  return `"${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`
}

function psQuote(value: string): string {
  return value.replace(/'/g, "''")
}

function psString(value: string): string {
  return `'${psQuote(value)}'`
}
