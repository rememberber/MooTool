import { execFile, spawn, type ChildProcessWithoutNullStreams } from 'node:child_process'
import { access, mkdir, readFile, unlink, writeFile } from 'node:fs/promises'
import { constants } from 'node:fs'
import { lookup } from 'node:dns/promises'
import { createConnection, type Socket } from 'node:net'
import { homedir, networkInterfaces } from 'node:os'
import { dirname, join } from 'node:path'
import si from 'systeminformation'
import type {
  DeleteEnvironmentVariableInput,
  EnvironmentEntry,
  EnvironmentScope,
  EnvironmentSnapshot,
  EnvironmentVariableInput,
  NetworkAction,
  NetworkCommandInput,
  NetworkCommandResult,
  LocalAddressSnapshot,
  SystemHostsFile,
  SystemInfoGroup,
  SystemInfoSnapshot
} from '../../src/shared/contracts/system'

const maxCommandOutput = 2 * 1024 * 1024
const maxCustomPorts = 4096
const ipRangeConcurrency = 32
const portScanConcurrency = 96
const environmentAssignment = /^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/
const environmentProfileMarker = '# >>> MooTool environment >>>'
const commonPorts = new Map<number, string>([
  [20, 'ftp-data'], [21, 'ftp'], [22, 'ssh'], [23, 'telnet'], [25, 'smtp'], [53, 'dns'],
  [67, 'dhcp'], [68, 'dhcp'], [69, 'tftp'], [80, 'http'], [110, 'pop3'], [123, 'ntp'],
  [135, 'msrpc'], [139, 'netbios'], [143, 'imap'], [161, 'snmp'], [389, 'ldap'], [443, 'https'],
  [445, 'smb'], [465, 'smtps'], [587, 'smtp-submission'], [631, 'ipp'], [636, 'ldaps'],
  [873, 'rsync'], [993, 'imaps'], [995, 'pop3s'], [1080, 'socks'], [1433, 'mssql'],
  [1521, 'oracle'], [2049, 'nfs'], [2181, 'zookeeper'], [2375, 'docker'], [3000, 'http-alt'],
  [3306, 'mysql'], [3389, 'rdp'], [5432, 'postgresql'], [5601, 'kibana'], [5672, 'amqp'],
  [5900, 'vnc'], [6379, 'redis'], [8080, 'http-alt'], [8081, 'http-alt'], [8443, 'https-alt'],
  [8888, 'http-alt'], [9092, 'kafka'], [9200, 'elasticsearch'], [9300, 'elasticsearch'],
  [11211, 'memcached'], [27017, 'mongodb']
])

export class SystemService {
  private readonly processes = new Map<string, Set<ChildProcessWithoutNullStreams>>()
  private readonly scanControllers = new Map<string, AbortController>()

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
    const controller = new AbortController()
    this.scanControllers.set(input.requestId, controller)
    try {
      const output = await this.executeNetworkAction(input, controller.signal)
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
      this.scanControllers.delete(input.requestId)
    }
  }

  cancel(requestId: string): boolean {
    const controller = this.scanControllers.get(requestId)
    const processes = this.processes.get(requestId)
    if (!controller && !processes) return false
    controller?.abort()
    for (const process of processes ?? []) process.kill('SIGTERM')
    this.processes.delete(requestId)
    return true
  }

  async getEnvironment(): Promise<EnvironmentSnapshot> {
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
    const [user, system] = await Promise.all([
      this.listPersistentEnvironment('user'),
      this.listPersistentEnvironment('system')
    ])
    return { environment, runtime, user, system }
  }

  async setEnvironmentVariable(input: EnvironmentVariableInput): Promise<void> {
    if (process.platform === 'win32') {
      await writeWindowsEnvironment(input.scope, input.key, input.value)
    } else {
      await this.writeUnixEnvironment(input.scope, input.key, input.value)
    }
    await this.syncCurrentProcessEnvironment(input.key)
  }

  async deleteEnvironmentVariable(input: DeleteEnvironmentVariableInput): Promise<void> {
    if (process.platform === 'win32') {
      await writeWindowsEnvironment(input.scope, input.key)
    } else {
      await this.writeUnixEnvironment(input.scope, input.key)
    }
    await this.syncCurrentProcessEnvironment(input.key)
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

  private async executeNetworkAction(input: NetworkCommandInput, signal: AbortSignal): Promise<string> {
    const timeoutMs = clampTimeout(input.timeoutMs)
    switch (input.action) {
      case 'resolve': {
        const target = normalizeHostTarget(input.target)
        const addresses = await lookup(target, { all: true })
        return addresses.map((entry) => `${entry.address}\tIPv${entry.family}`).join('\n')
      }
      case 'whois':
        return queryWhois(normalizeWhoisTarget(input.target), timeoutMs)
      case 'ping-range':
        return this.scanIpRange(input.requestId, input.target, timeoutMs, signal)
      case 'port-scan':
        return scanPorts(normalizeHostTarget(input.target), input.ports, timeoutMs, signal)
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
      this.registerProcess(requestId, child)
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
      child.on('error', (error) => { clearTimeout(timer); this.unregisterProcess(requestId, child); reject(error) })
      child.on('close', (code, signal) => {
        clearTimeout(timer)
        this.unregisterProcess(requestId, child)
        if (overflow) return reject(new Error('Command output exceeds 2 MB'))
        if (signal === 'SIGTERM') return reject(new Error('ABORTED'))
        if (code !== 0) return reject(new Error(stderr.trim() || `${file} exited with code ${code}`))
        resolve((stdout || stderr).trim())
      })
    })
  }

  private async scanIpRange(requestId: string, value: string | undefined, timeoutMs: number, signal: AbortSignal): Promise<string> {
    const addresses = parseIpv4Range(value)
    const reachable = (await concurrentMap(addresses, ipRangeConcurrency, signal,
      async (address) => await this.pingOnce(requestId, address, Math.min(timeoutMs, 800), signal),
      Date.now() + timeoutMs))
      .filter((entry) => entry.reachable)
      .map((entry) => entry.address)
    return [`Reachable hosts: ${reachable.length} / ${addresses.length}`, '', ...(reachable.length ? reachable : ['No reachable host found'])].join('\n')
  }

  private pingOnce(requestId: string, address: string, timeoutMs: number, signal: AbortSignal): Promise<{ address: string; reachable: boolean }> {
    return new Promise((resolve, reject) => {
      if (signal.aborted) return reject(new Error('ABORTED'))
      const command = pingOnceCommand(address, timeoutMs)
      const child = spawn(command.file, command.args, { windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] })
      this.registerProcess(requestId, child)
      let settled = false
      const finish = (reachable: boolean, error?: Error): void => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        signal.removeEventListener('abort', abort)
        this.unregisterProcess(requestId, child)
        if (error) reject(error)
        else resolve({ address, reachable })
      }
      const abort = (): void => {
        child.kill('SIGTERM')
        finish(false, new Error('ABORTED'))
      }
      const timer = setTimeout(() => {
        child.kill('SIGTERM')
        finish(false)
      }, timeoutMs + 1_000)
      timer.unref()
      signal.addEventListener('abort', abort, { once: true })
      child.on('error', (error) => finish(false, error))
      child.on('close', (code, closeSignal) => {
        if (signal.aborted || closeSignal === 'SIGTERM' && !settled) return finish(false, new Error('ABORTED'))
        finish(code === 0)
      })
    })
  }

  private registerProcess(requestId: string, child: ChildProcessWithoutNullStreams): void {
    const processes = this.processes.get(requestId) ?? new Set<ChildProcessWithoutNullStreams>()
    processes.add(child)
    this.processes.set(requestId, processes)
  }

  private unregisterProcess(requestId: string, child: ChildProcessWithoutNullStreams): void {
    const processes = this.processes.get(requestId)
    if (!processes) return
    processes.delete(child)
    if (!processes.size) this.processes.delete(requestId)
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
        await execFilePromise('pkexec', ['/bin/sh', '-c', '/bin/cp "$1" "$2" && /bin/chmod 644 "$2"', 'mootool', temporary, destination], 120_000)
      }
    } finally {
      await unlink(temporary).catch(() => undefined)
    }
  }

  private async listPersistentEnvironment(scope: EnvironmentScope): Promise<EnvironmentEntry[]> {
    if (process.platform === 'win32') return readWindowsEnvironment(scope)
    const path = unixEnvironmentPath(scope)
    const content = await readFile(path, 'utf8').catch((error: NodeJS.ErrnoException) => {
      if (error.code === 'ENOENT') return ''
      throw error
    })
    return parseEnvironmentContent(content)
  }

  private async writeUnixEnvironment(scope: EnvironmentScope, key: string, value?: string): Promise<void> {
    const destination = unixEnvironmentPath(scope)
    const current = await readFile(destination, 'utf8').catch((error: NodeJS.ErrnoException) => {
      if (error.code === 'ENOENT') return ''
      throw error
    })
    const content = updateEnvironmentContent(current, key, value, scope === 'user' || process.platform === 'darwin')
    if (scope === 'user') {
      await mkdir(dirname(destination), { recursive: true })
      await writeFile(destination, content, { encoding: 'utf8', mode: 0o600 })
      await ensureUnixEnvironmentProfile(destination)
      if (process.platform === 'darwin') {
        const args = value === undefined ? ['unsetenv', key] : ['setenv', key, value]
        await execFilePromise('/bin/launchctl', args, 30_000).catch(() => undefined)
      }
      return
    }
    try {
      await writeFile(destination, content, 'utf8')
    } catch (error) {
      if (!isPermissionError(error)) throw error
      await this.writeEnvironmentElevated(destination, content)
    }
  }

  private async syncCurrentProcessEnvironment(key: string): Promise<void> {
    const [user, system] = await Promise.all([
      this.listPersistentEnvironment('user'),
      this.listPersistentEnvironment('system')
    ])
    const matches = (entry: EnvironmentEntry): boolean => process.platform === 'win32'
      ? entry.key.toLowerCase() === key.toLowerCase()
      : entry.key === key
    const effective = user.find(matches) ?? system.find(matches)
    if (effective) process.env[key] = effective.value
    else delete process.env[key]
  }

  private async writeEnvironmentElevated(destination: string, content: string): Promise<void> {
    const temporary = join(this.tempDirectory, `mootool-environment-${process.pid}-${Date.now()}`)
    await writeFile(temporary, content, { encoding: 'utf8', mode: 0o600 })
    try {
      if (process.platform === 'darwin') {
        const command = `/bin/cp ${shellQuote(temporary)} ${shellQuote(destination)} && /bin/chmod 644 ${shellQuote(destination)}`
        await execFilePromise('/usr/bin/osascript', ['-e', `do shell script ${appleScriptString(command)} with administrator privileges`], 120_000)
      } else {
        await execFilePromise('pkexec', ['/bin/sh', '-c', '/bin/cp "$1" "$2" && /bin/chmod 644 "$2"', 'mootool', temporary, destination], 120_000)
      }
    } finally {
      await unlink(temporary).catch(() => undefined)
    }
  }
}

export function parseEnvironmentContent(content: string): EnvironmentEntry[] {
  const values = new Map<string, string>()
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(environmentAssignment)
    if (!match) continue
    values.set(match[1], unquoteEnvironmentValue(match[2].trim()))
  }
  return [...values].map(([key, value]) => ({ key, value })).sort(compareEntries)
}

export function updateEnvironmentContent(content: string, key: string, value: string | undefined, shellExport: boolean): string {
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) throw new Error('Invalid environment variable name')
  if (value !== undefined && (value.length > 65_535 || /[\0\r\n]/.test(value))) throw new Error('Invalid environment variable value')
  const replacement = value === undefined ? undefined : serializeEnvironmentValue(key, value, shellExport)
  const lines = content.split(/\r?\n/)
  const result: string[] = []
  let replaced = false
  for (const line of lines) {
    const match = line.match(environmentAssignment)
    if (match?.[1] === key) {
      if (!replaced && replacement !== undefined) {
        result.push(replacement)
        replaced = true
      }
      continue
    }
    result.push(line)
  }
  while (result.at(-1) === '') result.pop()
  if (replacement !== undefined && !replaced) result.push(replacement)
  return result.length ? `${result.join('\n')}\n` : ''
}

async function readWindowsEnvironment(scope: EnvironmentScope): Promise<EnvironmentEntry[]> {
  const target = scope === 'user' ? 'User' : 'Machine'
  const script = `[Console]::OutputEncoding=[Text.Encoding]::UTF8; `
    + `[Environment]::GetEnvironmentVariables([EnvironmentVariableTarget]::${target}).GetEnumerator() `
    + `| Sort-Object Name | ForEach-Object { [PSCustomObject]@{ key=[string]$_.Key; value=[string]$_.Value } } `
    + '| ConvertTo-Json -Compress'
  const output = (await execFileText('powershell.exe', ['-NoProfile', '-Command', script], 30_000)).trim()
  if (!output) return []
  const parsed = JSON.parse(output) as EnvironmentEntry | EnvironmentEntry[]
  return (Array.isArray(parsed) ? parsed : [parsed]).sort(compareEntries)
}

async function writeWindowsEnvironment(scope: EnvironmentScope, key: string, value?: string): Promise<void> {
  const target = scope === 'user' ? 'User' : 'Machine'
  const psValue = value === undefined ? '$null' : `'${psQuote(value)}'`
  const script = `[Environment]::SetEnvironmentVariable('${psQuote(key)}',${psValue},[EnvironmentVariableTarget]::${target})`
  const encoded = Buffer.from(script, 'utf16le').toString('base64')
  if (scope === 'user') {
    await execFilePromise('powershell.exe', ['-NoProfile', '-EncodedCommand', encoded], 30_000)
    return
  }
  const elevated = `$p=Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru -ArgumentList @('-NoProfile','-EncodedCommand','${encoded}'); exit $p.ExitCode`
  await execFilePromise('powershell.exe', ['-NoProfile', '-Command', elevated], 120_000)
}

function unixEnvironmentPath(scope: EnvironmentScope): string {
  if (scope === 'user') return join(homedir(), '.MooTool', 'environment')
  return process.platform === 'darwin' ? '/etc/zshenv' : '/etc/environment'
}

async function ensureUnixEnvironmentProfile(environmentPath: string): Promise<void> {
  const profile = join(homedir(), process.platform === 'darwin' ? '.zshenv' : '.profile')
  const current = await readFile(profile, 'utf8').catch((error: NodeJS.ErrnoException) => {
    if (error.code === 'ENOENT') return ''
    throw error
  })
  if (current.includes(environmentProfileMarker)) return
  const separator = !current || current.endsWith('\n') ? '' : '\n'
  const block = `${environmentProfileMarker}\n[ -f ${shellQuote(environmentPath)} ] && . ${shellQuote(environmentPath)}\n# <<< MooTool environment <<<\n`
  await writeFile(profile, `${current}${separator}${block}`, 'utf8')
}

function serializeEnvironmentValue(key: string, value: string, shellExport: boolean): string {
  if (shellExport) return `export ${key}='${value.replace(/'/g, `'\\''`)}'`
  return `${key}="${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`
}

function unquoteEnvironmentValue(value: string): string {
  if (value.length >= 2 && value.startsWith("'") && value.endsWith("'")) {
    return value.slice(1, -1).replace(/'\\''/g, "'")
  }
  if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
    return value.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, '\\')
  }
  return value
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

export function parseIpv4Range(value?: string): string[] {
  const match = value?.trim().match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.?$/)
  if (!match) throw new Error('INVALID_TARGET')
  const octets = match.slice(1).map(Number)
  if (octets.some((octet) => octet > 255)) throw new Error('INVALID_TARGET')
  const prefix = octets.join('.')
  return Array.from({ length: 254 }, (_, index) => `${prefix}.${index + 1}`)
}

export function parsePortSpec(value?: string): number[] {
  const input = value?.trim() ?? ''
  if (!input) return [...commonPorts.keys()]
  const ports = new Set<number>()
  for (const rawToken of input.split(',')) {
    const token = rawToken.trim()
    const range = token.match(/^(\d{1,5})\s*-\s*(\d{1,5})$/)
    if (range) {
      const start = validPort(range[1])
      const end = validPort(range[2])
      if (start > end || end - start + 1 > maxCustomPorts) throw new Error('INVALID_TARGET')
      for (let port = start; port <= end; port += 1) ports.add(port)
    } else {
      ports.add(validPort(token))
    }
    if (ports.size > maxCustomPorts) throw new Error('INVALID_TARGET')
  }
  return [...ports].sort((left, right) => left - right)
}

async function scanPorts(host: string, portSpec: string | undefined, timeoutMs: number, signal: AbortSignal): Promise<string> {
  const ports = parsePortSpec(portSpec)
  const results = await concurrentMap(ports, portScanConcurrency, signal, async (port) => ({
    port,
    open: await probePort(host, port, Math.min(timeoutMs, 500), signal)
  }), Date.now() + timeoutMs)
  const openPorts = results.filter((entry) => entry.open)
  const lines = openPorts.map(({ port }) => {
    const service = commonPorts.get(port)
    return `${port}/tcp open${service ? ` ${service}` : ''}`
  })
  return [`Open TCP ports on ${host}: ${openPorts.length} / ${ports.length}`, '', ...(lines.length ? lines : ['No open TCP port found'])].join('\n')
}

function probePort(host: string, port: number, timeoutMs: number, signal: AbortSignal): Promise<boolean> {
  return new Promise((resolve, reject) => {
    if (signal.aborted) return reject(new Error('ABORTED'))
    let socket: Socket | undefined
    let settled = false
    const finish = (open: boolean, error?: Error): void => {
      if (settled) return
      settled = true
      signal.removeEventListener('abort', abort)
      socket?.destroy()
      if (error) reject(error)
      else resolve(open)
    }
    const abort = (): void => finish(false, new Error('ABORTED'))
    signal.addEventListener('abort', abort, { once: true })
    socket = createConnection({ host, port })
    socket.setTimeout(Math.max(100, timeoutMs))
    socket.once('connect', () => finish(true))
    socket.once('timeout', () => finish(false))
    socket.once('error', () => finish(false))
  })
}

async function concurrentMap<T, R>(
  values: T[],
  concurrency: number,
  signal: AbortSignal,
  mapper: (value: T) => Promise<R>,
  deadline?: number
): Promise<R[]> {
  const results = new Array<R>(values.length)
  let nextIndex = 0
  const worker = async (): Promise<void> => {
    while (true) {
      if (signal.aborted) throw new Error('ABORTED')
      if (deadline && Date.now() >= deadline) throw new Error('TIMEOUT')
      const index = nextIndex
      nextIndex += 1
      if (index >= values.length) return
      results[index] = await mapper(values[index])
    }
  }
  await Promise.all(Array.from({ length: Math.min(concurrency, values.length) }, worker))
  return results
}

function validPort(value: string): number {
  if (!/^\d{1,5}$/.test(value)) throw new Error('INVALID_TARGET')
  const port = Number(value)
  if (port < 1 || port > 65535) throw new Error('INVALID_TARGET')
  return port
}

function pingOnceCommand(host: string, timeoutMs: number): { file: string; args: string[] } {
  if (process.platform === 'win32') return { file: 'ping.exe', args: ['-n', '1', '-w', String(timeoutMs), host] }
  if (process.platform === 'darwin') return { file: '/sbin/ping', args: ['-c', '1', '-W', String(timeoutMs), host] }
  return { file: 'ping', args: ['-c', '1', '-W', String(Math.max(1, Math.ceil(timeoutMs / 1_000))), host] }
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

function execFileText(file: string, args: string[], timeout: number): Promise<string> {
  return new Promise((resolve, reject) => execFile(file, args, { timeout, windowsHide: true, encoding: 'utf8' }, (error, stdout) => error ? reject(error) : resolve(stdout)))
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
