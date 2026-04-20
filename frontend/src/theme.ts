import type { GlobalThemeOverrides } from 'naive-ui'

export const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#0a6b7c',
    primaryColorHover: '#0e8594',
    primaryColorPressed: '#0a4e5c',
    primaryColorSuppl: '#12a594',
    infoColor: '#3b82f6',
    successColor: '#1aa864',
    warningColor: '#e08a2c',
    errorColor: '#e5483c',
    borderRadius: '8px',
    borderRadiusSmall: '6px',
    fontFamily:
      '"Inter", "Noto Sans SC", -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif',
    fontFamilyMono: '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
    textColorBase: '#0b1a20',
    textColor1: '#0b1a20',
    textColor2: '#2a3942',
    textColor3: '#5a6a74',
    bodyColor: '#f6f7f8',
    cardColor: '#ffffff'
  },
  Button: {
    heightMedium: '36px',
    heightSmall: '28px',
    fontWeightStrong: '500'
  },
  Card: {
    borderRadius: '14px'
  }
}
