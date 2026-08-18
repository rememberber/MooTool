export interface Ipv4CidrResult {
  address: string
  prefix: number
  netmask: string
  wildcard: string
  network: string
  broadcast: string
  firstHost: string
  lastHost: string
  totalAddresses: number
  usableHosts: number
  binary: string
  integer: number
  category: Ipv4Category
}

export type Ipv4Category = 'private' | 'loopback' | 'linkLocal' | 'multicast' | 'limitedBroadcast' | 'publicReserved'
export type NetworkToolErrorCode = 'ipv4Input' | 'octetRange' | 'prefixRange' | 'integerFormat' | 'integerRange'

export class NetworkToolError extends Error {
  constructor(readonly code: NetworkToolErrorCode) {
    super(`NETWORK_TOOL_${code}`)
    this.name = 'NetworkToolError'
  }
}

export function analyzeIpv4Cidr(input: string): Ipv4CidrResult {
  const match = /^\s*(\d{1,3}(?:\.\d{1,3}){3})(?:\/(\d{1,2}))?\s*$/.exec(input)
  if (!match) throw new NetworkToolError('ipv4Input')
  const octets = match[1].split('.').map(Number)
  if (octets.some((part) => part > 255)) throw new NetworkToolError('octetRange')
  const prefix = match[2] === undefined ? 32 : Number(match[2])
  if (prefix < 0 || prefix > 32) throw new NetworkToolError('prefixRange')
  const integer = octets.reduce((value, part) => ((value << 8) | part) >>> 0, 0)
  const mask = prefix === 0 ? 0 : (0xffffffff << (32 - prefix)) >>> 0
  const wildcard = (~mask) >>> 0
  const network = (integer & mask) >>> 0
  const broadcast = (network | wildcard) >>> 0
  const totalAddresses = 2 ** (32 - prefix)
  const usableHosts = prefix === 32 ? 1 : prefix === 31 ? 2 : Math.max(0, totalAddresses - 2)
  const firstHost = prefix >= 31 ? network : network + 1
  const lastHost = prefix >= 31 ? broadcast : broadcast - 1
  return {
    address: formatIpv4(integer),
    prefix,
    netmask: formatIpv4(mask),
    wildcard: formatIpv4(wildcard),
    network: formatIpv4(network),
    broadcast: formatIpv4(broadcast),
    firstHost: formatIpv4(firstHost),
    lastHost: formatIpv4(lastHost),
    totalAddresses,
    usableHosts,
    binary: octets.map((part) => part.toString(2).padStart(8, '0')).join('.'),
    integer,
    category: classifyIpv4(integer)
  }
}

export function integerToIpv4(value: string): string {
  if (!/^\d+$/.test(value.trim())) throw new NetworkToolError('integerFormat')
  const parsed = Number(value)
  if (!Number.isSafeInteger(parsed) || parsed < 0 || parsed > 0xffffffff) {
    throw new NetworkToolError('integerRange')
  }
  return formatIpv4(parsed)
}

function formatIpv4(value: number): string {
  const unsigned = value >>> 0
  return [24, 16, 8, 0].map((shift) => (unsigned >>> shift) & 255).join('.')
}

function classifyIpv4(value: number): Ipv4Category {
  const first = value >>> 24
  const second = (value >>> 16) & 255
  if (first === 10 || (first === 172 && second >= 16 && second <= 31) || (first === 192 && second === 168)) {
    return 'private'
  }
  if (first === 127) return 'loopback'
  if (first === 169 && second === 254) return 'linkLocal'
  if (first >= 224 && first <= 239) return 'multicast'
  if (value === 0xffffffff) return 'limitedBroadcast'
  return 'publicReserved'
}
