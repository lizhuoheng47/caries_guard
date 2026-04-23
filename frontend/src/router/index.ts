import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/',
      component: () => import('../components/layout/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/dashboard/ai'
        },
        {
          path: 'dashboard/ai',
          name: 'dashboard-ai',
          component: () => import('../views/DashboardView.vue')
        },
        {
          path: 'analysis',
          name: 'analysis-queue',
          component: () => import('../views/TaskQueueView.vue')
        },
        {
          path: 'analysis/:taskId',
          name: 'analysis-detail',
          component: () => import('../views/AnalysisDetailView.vue')
        },
        {
          path: 'review/:taskId',
          name: 'review-workbench',
          component: () => import('../views/ReviewWorkbenchView.vue')
        },
        {
          path: 'rag',
          name: 'rag-console',
          component: () => import('../views/RagConsoleView.vue')
        },
        {
          path: 'knowledge',
          name: 'knowledge-repo',
          component: () => import('../views/KnowledgeRepoView.vue')
        },
        {
          path: 'cases',
          name: 'case-portal',
          component: () => import('../views/CasePortalView.vue')
        }
      ]
    }
  ]
});

router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();
  
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login', query: { redirect: to.fullPath } });
  } else if (to.name === 'login' && authStore.isAuthenticated) {
    next({ path: '/' });
  } else {
    if (authStore.isAuthenticated && !authStore.user) {
      try {
        await authStore.fetchUserInfo();
        next();
      } catch (e) {
        next({ name: 'login' });
      }
    } else {
      next();
    }
  }
});

export default router;
