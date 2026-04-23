import { createI18n } from 'vue-i18n'

export const messages = {
  zh: {
    product: 'DentAI',
    nav: {
      home: '工作台',
      analyze: 'AI 分析',
      cases: '病例中心'
    },
    common: {
      search: '搜索患者、病例、任务号',
      newCase: '新建分析',
      today: '今天'
    }
  },
  en: {
    product: 'DentAI',
    nav: {
      home: 'Dashboard',
      analyze: 'AI Analysis',
      cases: 'Cases'
    },
    common: {
      search: 'Search patients, cases, task numbers',
      newCase: 'New analysis',
      today: 'Today'
    }
  }
}

export const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('lang') || 'zh',
  fallbackLocale: 'en',
  messages
})

document.documentElement.lang = String(i18n.global.locale.value)

export function setLang(lang: 'zh' | 'en') {
  localStorage.setItem('lang', lang)
  i18n.global.locale.value = lang
  document.documentElement.lang = lang
}
