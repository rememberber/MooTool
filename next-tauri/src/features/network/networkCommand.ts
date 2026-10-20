const maxCustomPorts = 4096

export const commonTcpPorts = new Map<number, string>([
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
  if (!input) return [...commonTcpPorts.keys()]
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

export function formatOpenPorts(host: string, scanned: number[], openPorts: number[]): string {
  const lines = openPorts.map((port) => {
    const service = commonTcpPorts.get(port)
    return `${port}/tcp open${service ? ` ${service}` : ''}`
  })
  return [`Open TCP ports on ${host}: ${openPorts.length} / ${scanned.length}`, '', ...(lines.length ? lines : ['No open TCP port found'])].join('\n')
}

function validPort(value: string): number {
  if (!/^\d{1,5}$/.test(value)) throw new Error('INVALID_TARGET')
  const port = Number(value)
  if (port < 1 || port > 65535) throw new Error('INVALID_TARGET')
  return port
}
