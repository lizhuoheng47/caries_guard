import type { GlobalThemeOverrides } from 'naive-ui'

export const themeOverrides: GlobalThemeOverrides = {
  common: {
    primaryColor: '#21cdfd',
    primaryColorHover: '#35f8ff',
    primaryColorPressed: '#3f79ff',
    primaryColorSuppl: '#7856ff',
    infoColor: '#3f79ff',
    successColor: '#35f8ff',
    warningColor: '#f7a23a',
    errorColor: '#ff636e',
    borderRadius: '8px',
    borderRadiusSmall: '6px',
    fontFamily:
      '"Inter", "Noto Sans SC", -apple-system, BlinkMacSystemFont, "Segoe UI", system-ui, sans-serif',
    fontFamilyMono: '"JetBrains Mono", ui-monospace, Menlo, Consolas, monospace',
    textColorBase: '#f2f7ff',
    textColor1: '#f2f7ff',
    textColor2: '#d8e5fb',
    textColor3: '#b6c7e8',
    bodyColor: '#020914',
    cardColor: 'rgba(15, 31, 63, 0.92)',
  },
  Button: {
    heightMedium: '36px',
    heightSmall: '28px',
    fontWeightStrong: '500',
  },
  Card: {
    borderRadius: '14px',
  },
}
