import { createI18n } from 'vue-i18n'

export const messages = {
  zh: {
    product: 'DentAI 龋智',
    nav: {
      home: '工作台',
      analyze: 'AI 分析',
      knowledge: '知识库',
      rag: '医生问答',
      cases: '病例中心'
    },
    login: {
      eyebrow: '临床辅助诊断系统',
      title: '更准确的龋齿影像分析，从一次登录开始。',
      sub: '面向口腔医生的 AI 影像工作台，覆盖分析、复核、知识检索与医生问答。',
      account: '账号',
      password: '密码',
      signIn: '登录',
      sso: '使用医院 SSO 登录',
      remember: '保持登录',
      forgot: '忘记密码？',
      disclaimer: '本系统仅提供辅助建议，最终诊断需由执业医生确认。'
    },
    common: {
      search: '搜索患者、病例、任务号',
      newCase: '新建分析',
      today: '今天',
      upload: '上传影像开始分析',
      dragDrop: '拖拽影像到这里，或点击上传',
      supported: '支持 JPG / PNG / DICOM，单文件不超过 50MB',
      analyzing: 'AI 正在分析影像',
      accept: '采纳',
      reject: '驳回',
      edit: '修改',
      exportPdf: '导出 PDF',
      download: '下载',
      share: '分享',
      confidence: '置信度',
      severity: '严重程度',
      size: '病灶大小',
      suggestion: '处理建议',
      surface: '牙面',
      tooth: '牙位',
      findings: '病灶结果',
      compare: '历史对比',
      reanalyze: '重新分析'
    }
  },
  en: {
    product: 'DentAI',
    nav: {
      home: 'Dashboard',
      analyze: 'AI Analysis',
      knowledge: 'Knowledge',
      rag: 'Doctor QA',
      cases: 'Cases'
    },
    login: {
      eyebrow: 'Clinical Decision Support',
      title: 'More accurate caries imaging analysis starts with one sign-in.',
      sub: 'An AI workstation for dental clinicians across analysis, review, knowledge retrieval, and doctor QA.',
      account: 'Account',
      password: 'Password',
      signIn: 'Sign in',
      sso: 'Sign in with Hospital SSO',
      remember: 'Keep me signed in',
      forgot: 'Forgot password?',
      disclaimer: 'This system provides decision support only. Final diagnosis remains with licensed clinicians.'
    },
    common: {
      search: 'Search patients, cases, task numbers',
      newCase: 'New analysis',
      today: 'Today',
      upload: 'Upload an image to begin',
      dragDrop: 'Drop an image here, or click to upload',
      supported: 'JPG / PNG / DICOM, up to 50MB per file',
      analyzing: 'AI is analyzing the image',
      accept: 'Accept',
      reject: 'Reject',
      edit: 'Edit',
      exportPdf: 'Export PDF',
      download: 'Download',
      share: 'Share',
      confidence: 'Confidence',
      severity: 'Severity',
      size: 'Size',
      suggestion: 'Recommendation',
      surface: 'Surface',
      tooth: 'Tooth',
      findings: 'Findings',
      compare: 'Compare',
      reanalyze: 'Re-analyze'
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
