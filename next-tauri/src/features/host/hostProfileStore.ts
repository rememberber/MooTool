import type { HostProfile, SaveHostProfileInput } from '../../platform/contracts/localData'

export function sqliteDate(date = new Date()): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function listStoredHostProfiles(profiles: HostProfile[], keyword = '', includeContent = true): HostProfile[] {
  const needle = keyword.trim()
  const visible = needle
    ? profiles.filter((profile) => matchesLike(profile.name, needle) || (includeContent && matchesLike(profile.content, needle)))
    : profiles
  return [...visible].sort((left, right) => right.modifiedTime.localeCompare(left.modifiedTime) || right.id - left.id)
}

export function saveStoredHostProfile(profiles: HostProfile[], input: SaveHostProfileInput, now = new Date()): HostProfile {
  const name = input.name.trim()
  if (!name) throw new Error('Host profile name is required')
  if ([...name].length > 200 || input.content.length > 2 * 1024 * 1024) throw new Error('Invalid string value')
  const id = Number.isSafeInteger(input.id) && Number(input.id) > 0 ? Number(input.id) : undefined
  const existing = id ? profiles.find((profile) => profile.id === id) : profiles.find((profile) => profile.name === name)
  if (existing && profiles.some((profile) => profile.id !== existing.id && profile.name === name)) throw new Error('UNIQUE constraint failed: t_host.name')
  const stamp = sqliteDate(now)
  const saved: HostProfile = existing
    ? { ...existing, name, content: input.content, modifiedTime: stamp }
    : { id: Math.max(0, ...profiles.map((profile) => profile.id)) + 1, name, content: input.content, createTime: stamp, modifiedTime: stamp }
  return saved
}

export function replaceStoredHostProfile(profiles: HostProfile[], saved: HostProfile): HostProfile[] {
  return listStoredHostProfiles([saved, ...profiles.filter((profile) => profile.id !== saved.id)], '')
}

function matchesLike(value: string, keyword: string): boolean {
  const pattern = `%${keyword}%`.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replaceAll('%', '.*').replaceAll('_', '.')
  return new RegExp(`^${pattern}$`, 'i').test(value)
}
