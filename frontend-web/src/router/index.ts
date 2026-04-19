import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import KbOverviewView from '../views/rag-kb/KbOverviewView.vue'
import DocumentListView from '../views/rag-kb/DocumentListView.vue'
import DocumentUploadView from '../views/rag-kb/DocumentUploadView.vue'
import DocumentDetailView from '../views/rag-kb/DocumentDetailView.vue'
import ReviewPublishView from '../views/rag-kb/ReviewPublishView.vue'
import TaskListView from '../views/rag-kb/TaskListView.vue'
import RagDebugView from '../views/rag-debug/RagDebugView.vue'
import GraphEvidenceView from '../views/rag-debug/GraphEvidenceView.vue'
import RagLogsView from '../views/rag-logs/RagLogsView.vue'
import RagEvalView from '../views/rag-eval/RagEvalView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        { path: '', redirect: '/kb/overview' },
        { path: '/kb/overview', component: KbOverviewView },
        { path: '/kb/documents', component: DocumentListView },
        { path: '/kb/upload', component: DocumentUploadView },
        { path: '/kb/documents/:id', component: DocumentDetailView },
        { path: '/kb/review', component: ReviewPublishView },
        { path: '/kb/tasks', component: TaskListView },
        { path: '/rag/debug', component: RagDebugView },
        { path: '/rag/graph', component: GraphEvidenceView },
        { path: '/rag/logs', component: RagLogsView },
        { path: '/rag/eval', component: RagEvalView },
      ],
    },
  ],
})
