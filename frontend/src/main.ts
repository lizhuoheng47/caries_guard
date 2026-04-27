import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import { i18n } from './i18n'
import './index.css'
import './styles/tokens.css'
import './styles/app.css'
import './styles/medical-workspace.css'
import App from './App.vue'

const app = createApp(App)

app.use(createPinia())
app.use(i18n)
app.use(router)
app.mount('#app')
