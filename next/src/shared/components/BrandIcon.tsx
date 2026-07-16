type BrandIconProps = {
  className?: string
  size?: number
}

export function BrandIcon({ className = '', size = 20 }: BrandIconProps) {
  return (
    <img
      className={`brand-icon ${className}`.trim()}
      src="./brand/mootool-logo.png"
      width={size}
      height={size}
      alt=""
      aria-hidden="true"
      draggable={false}
    />
  )
}
