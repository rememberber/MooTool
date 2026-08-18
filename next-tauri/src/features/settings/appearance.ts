import type { ThemePreference } from '../../platform/contracts/settings'

export function resolveTheme(
  preference: ThemePreference,
  systemDark: boolean
): 'light' | 'dark' {
  return preference === 'system' ? (systemDark ? 'dark' : 'light') : preference
}
