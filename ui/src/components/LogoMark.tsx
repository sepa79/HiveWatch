export function LogoMark({ size = 22 }: { size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 220 220"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <radialGradient id="hwLensGrad" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stopColor="#FF3C2E" />
          <stop offset="40%" stopColor="#B20E00" />
          <stop offset="100%" stopColor="#400000" />
        </radialGradient>
      </defs>
      <path d="M110 20 190 65v90l-80 45-80-45V65z" fill="none" stroke="#FFC107" strokeWidth="14" strokeLinejoin="round" />
      <path
        d="M62 110c12-19 29-31 48-31s36 12 48 31c-12 19-29 31-48 31s-36-12-48-31z"
        fill="none"
        stroke="#ffffff"
        strokeWidth="12"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="110" cy="110" r="20" fill="#222" stroke="#999" strokeWidth="3" />
      <circle cx="110" cy="110" r="14" fill="url(#hwLensGrad)" />
    </svg>
  )
}
