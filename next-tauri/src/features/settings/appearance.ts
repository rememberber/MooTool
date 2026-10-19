import type { AccentColor, ThemePreference } from '../../platform/contracts/settings'

/** Same accent values as Next Electron, plus Tauri's extra indigo, teal, and orange. */
export const accentPresets: Record<AccentColor, { value: string; strong: string; contrast: string }> = {
  blue: { value: '#4f83cc', strong: '#3f6fae', contrast: '#ffffff' },
  yellow: { value: '#e0b22b', strong: '#b78300', contrast: '#241c00' },
  coral: { value: '#de8f7d', strong: '#cd6f5e', contrast: '#ffffff' },
  green: { value: '#4e9275', strong: '#34755b', contrast: '#ffffff' },
  red: { value: '#c96761', strong: '#a84e49', contrast: '#ffffff' },
  purple: { value: '#8a72b5', strong: '#6f579c', contrast: '#ffffff' },
  indigo: { value: '#7057d9', strong: '#5c45bd', contrast: '#ffffff' },
  teal: { value: '#168f82', strong: '#08766c', contrast: '#ffffff' },
  orange: { value: '#d96b2b', strong: '#b9541d', contrast: '#ffffff' }
}

export const electronInterfaceFontFamily = 'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif'

export function resolveTheme(
  preference: ThemePreference,
  systemDark: boolean
): 'light' | 'dark' {
  return preference === 'system' ? (systemDark ? 'dark' : 'light') : preference
}
