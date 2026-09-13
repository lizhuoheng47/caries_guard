import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import { i18n } from './i18n'
import './index.css'
import './styles/tokens.css'
import './styles/app.css'
import './styles/medical-workspace.css'
import App from './App.vue'

if (import.meta.env.PROD && import.meta.env.VITE_USE_MOCK === 'true') {
  throw new Error('VITE_USE_MOCK=true is forbidden in production builds')
}

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
app.use(router)
app.mount('#app')
