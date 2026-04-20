import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'login', component: () => import('@/pages/LoginPage.vue'), meta: { layout: 'blank' } },
  {
    path: '/app',
    component: () => import('@/layouts/AppShell.vue'),
    children: [
      { path: '', redirect: '/app/home' },
      { path: 'home', name: 'home', component: () => import('@/pages/HomePage.vue') },
      { path: 'analyze', name: 'analyze', component: () => import('@/pages/AnalyzePage.vue') },
      { path: 'report', name: 'report', component: () => import('@/pages/ReportPage.vue') },
      { path: 'library', name: 'library', component: () => import('@/pages/LibraryPage.vue') },
      { path: 'settings', name: 'settings', component: () => import('@/pages/SettingsPage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router
