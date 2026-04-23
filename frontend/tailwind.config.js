/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'neural-cyan': 'var(--cyan)',
        'soft-cyan': 'var(--cyan-soft)',
        'report-violet': 'var(--violet)',
        'alert-amber': 'var(--amber)',
        'critical-magenta': 'var(--magenta)',
        'safe-emerald': 'var(--emerald)',
        'void-black': 'var(--void)',
        'deep-surface': 'var(--surf)',
        'elevated': 'var(--elev)',
        'ice-white': 'var(--tp)',
        'steel-blue': 'var(--ts)',
        'ghost-dim': 'var(--td)',
        'line-subtle': 'var(--ln)',
        'line-hot': 'var(--lh)',
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'sans-serif'],
        mono: ['"SF Mono"', 'Consolas', 'Monaco', 'monospace'],
      },
      spacing: {
        'gap-xs': 'var(--gap-xs)',
        'gap-sm': 'var(--gap-sm)',
        'gap-md': 'var(--gap-md)',
        'gap-lg': 'var(--gap-lg)',
        'gap-xl': 'var(--gap-xl)',
        'gap-xxl': 'var(--gap-xxl)',
      },
      borderRadius: {
        'xs': 'var(--radius-xs)',
        'sm': 'var(--radius-sm)',
        'md': 'var(--radius-md)',
        'lg': 'var(--radius-lg)',
      }
    },
  },
  plugins: [],
}
