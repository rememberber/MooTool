export function ipv4ToLong(value: string): number {
  const parts = value.trim().split('.')
  if (parts.length !== 4 || parts.some((part) => !/^\d{1,3}$/.test(part) || Number(part) > 255)) throw new Error('Invalid IPv4 address')
  return parts.reduce((result, part) => result * 256 + Number(part), 0) >>> 0
}

export function longToIpv4(value: string | number): string {
  const number = typeof value === 'string' ? Number(value.trim()) : value
  if (!Number.isSafeInteger(number) || number < 0 || number > 0xffffffff) throw new Error('Invalid IPv4 number')
  return [24, 16, 8, 0].map((shift) => Math.floor(number / 2 ** shift) % 256).join('.')
}
