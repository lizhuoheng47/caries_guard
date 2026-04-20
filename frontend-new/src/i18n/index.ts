import { createI18n } from 'vue-i18n'

export const messages = {
  zh: {
    product: 'DentAI · 智齿',
    nav: { home: '工作台', analyze: 'AI 分析', reports: '诊断报告', library: '病例库', settings: '设置' },
    login: {
      title: '更精准的龋齿诊断，从一次点击开始。',
      sub: '面向口腔医师的 AI 辅助影像分析平台。病灶识别、程度分级、治疗建议 — 一站式。',
      account: '医师工号',
      password: '密码',
      signIn: '登录',
      sso: '使用医院 SSO 登录',
      remember: '保持登录',
      forgot: '忘记密码？',
      disclaimer: '本系统为辅助诊断工具，最终诊断须由执业医师作出。',
      eyebrow: '临床辅助诊断系统'
    },
    common: {
      search: '搜索患者、影像、报告…',
      newCase: '新建病例',
      today: '今日',
      upload: '上传影像开始分析',
      dragDrop: '拖拽影像到此处，或点击选择文件',
      supported: '支持 JPG / PNG / DICOM · 最大 50MB',
      analyzing: 'AI 正在分析影像',
      accept: '采纳', reject: '拒绝', edit: '修改',
      exportPdf: '导出 PDF', download: '下载', share: '分享',
      confidence: '置信度', severity: '病变程度', size: '病灶大小',
      suggestion: '治疗建议', surface: '牙面', tooth: '牙位',
      findings: '检出病灶', compare: '历史对比', reanalyze: '重新分析'
    }
  },
  en: {
    product: 'DentAI',
    nav: { home: 'Dashboard', analyze: 'AI Analysis', reports: 'Reports', library: 'Case Library', settings: 'Settings' },
    login: {
      title: 'Precision caries diagnosis, in one click.',
      sub: 'AI-assisted imaging analysis for dental clinicians. Detection, grading, recommendations — in one place.',
      account: 'Clinician ID',
      password: 'Password',
      signIn: 'Sign in',
      sso: 'Sign in with Hospital SSO',
      remember: 'Keep me signed in',
      forgot: 'Forgot password?',
      disclaimer: 'A clinical decision-support tool. Final diagnosis by licensed practitioners.',
      eyebrow: 'Clinical Decision Support'
    },
    common: {
      search: 'Search patients, images, reports…',
      newCase: 'New case',
      today: 'Today',
      upload: 'Upload an image to begin',
      dragDrop: 'Drop image here, or click to browse',
      supported: 'JPG / PNG / DICOM · up to 50MB',
      analyzing: 'AI is analyzing the image',
      accept: 'Accept', reject: 'Reject', edit: 'Edit',
      exportPdf: 'Export PDF', download: 'Download', share: 'Share',
      confidence: 'Confidence', severity: 'Severity', size: 'Lesion size',
      suggestion: 'Recommendation', surface: 'Surface', tooth: 'Tooth',
      findings: 'Detected lesions', compare: 'Compare', reanalyze: 'Re-analyze'
    }
  }
}

export const i18n = createI18n({
  legacy: false,
  locale: localStorage.getItem('lang') || 'zh',
  fallbackLocale: 'en',
  messages
})

export function setLang(l: 'zh' | 'en') {
  localStorage.setItem('lang', l)
  i18n.global.locale.value = l
  document.documentElement.lang = l
}
