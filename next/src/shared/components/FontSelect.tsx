import { useMemo } from 'react'
import { cssFontFamily, fontSelectOptions, useSystemFontFamilies } from '@/shared/fonts/systemFonts'

type FontSelectProps = {
  value: string
  ariaLabel: string
  disabled?: boolean
  className?: string
  emptyLabel?: string
  labels?: Record<string, string>
  onChange: (value: string) => void
}

export function FontSelect({
  value,
  ariaLabel,
  disabled = false,
  className = '',
  emptyLabel,
  labels = {},
  onChange
}: FontSelectProps) {
  const fonts = useSystemFontFamilies()
  const options = useMemo(() => fontSelectOptions(fonts, value), [fonts, value])

  return (
    <select
      className={className}
      aria-label={ariaLabel}
      title={ariaLabel}
      disabled={disabled}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    >
      {emptyLabel !== undefined ? <option value="">{emptyLabel}</option> : null}
      {options.map((font) => (
        <option key={font} value={font} style={{ fontFamily: cssFontFamily(font) }}>
          {labels[font] ?? font}
        </option>
      ))}
    </select>
  )
}
