import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import KbOverviewView from '../views/rag-kb/KbOverviewView.vue'
import DocumentListView from '../views/rag-kb/DocumentListView.vue'
import DocumentUploadView from '../views/rag-kb/DocumentUploadView.vue'
import DocumentDetailView from '../views/rag-kb/DocumentDetailView.vue'
import ReviewPublishView from '../views/rag-kb/ReviewPublishView.vue'
import PublishHistoryView from '../views/rag-kb/PublishHistoryView.vue'
import IngestJobsView from '../views/rag-kb/IngestJobsView.vue'
import RebuildJobsView from '../views/rag-kb/RebuildJobsView.vue'
import GraphStatsView from '../views/rag-kb/GraphStatsView.vue'
import DoctorQaDebugView from '../views/rag-debug/DoctorQaDebugView.vue'
import PatientExplanationDebugView from '../views/rag-debug/PatientExplanationDebugView.vue'
import GraphEvidenceView from '../views/rag-debug/GraphEvidenceView.vue'
import RagLogsView from '../views/rag-logs/RagLogsView.vue'
import RetrievalDetailView from '../views/rag-logs/RetrievalDetailView.vue'
import RagEvalView from '../views/rag-eval/RagEvalView.vue'
import EvalResultsView from '../views/rag-eval/EvalResultsView.vue'

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
        { path: '/kb/publish-history', component: PublishHistoryView },
        { path: '/kb/ingest-jobs', component: IngestJobsView },
        { path: '/kb/rebuild-jobs', component: RebuildJobsView },
        { path: '/kb/graph-stats', component: GraphStatsView },
        { path: '/rag/doctor-qa', component: DoctorQaDebugView },
        { path: '/rag/patient-explanation', component: PatientExplanationDebugView },
        { path: '/rag/retrieval/:requestNo?', component: RetrievalDetailView },
        { path: '/rag/graph/:requestNo?', component: GraphEvidenceView },
        { path: '/rag/logs', component: RagLogsView },
        { path: '/rag/eval', component: RagEvalView },
        { path: '/rag/eval/:runNo', component: EvalResultsView },
      ],
    },
  ],
})
