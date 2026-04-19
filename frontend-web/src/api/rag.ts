import { apiClient } from './client'

export const ragApi = {
  overview: (kbCode?: string) => apiClient.get('/kb/overview', { params: { kbCode } }),
  documents: (params?: Record<string, unknown>) => apiClient.get('/kb/documents', { params }),
  documentDetail: (id: string | number) => apiClient.get(`/kb/documents/${id}`),
  importText: (payload: Record<string, unknown>) => apiClient.post('/kb/documents/import-text', payload),
  updateDocument: (id: string | number, payload: Record<string, unknown>) => apiClient.put(`/kb/documents/${id}`, payload),
  submitReview: (id: string | number, payload: Record<string, unknown>) => apiClient.post(`/kb/documents/${id}/submit-review`, payload),
  approve: (id: string | number, payload: Record<string, unknown>) => apiClient.post(`/kb/documents/${id}/approve`, payload),
  reject: (id: string | number, payload: Record<string, unknown>) => apiClient.post(`/kb/documents/${id}/reject`, payload),
  publish: (id: string | number, payload: Record<string, unknown>) => apiClient.post(`/kb/documents/${id}/publish`, payload),
  rollback: (id: string | number, payload: Record<string, unknown>) => apiClient.post(`/kb/documents/${id}/rollback`, payload),
  upload: (formData: FormData) =>
    apiClient.post('/kb/documents/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  ingestJobs: () => apiClient.get('/kb/ingest-jobs'),
  rebuildJobs: (kbCode?: string) => apiClient.get('/kb/rebuild-jobs', { params: { kbCode } }),
  rebuild: (payload: Record<string, unknown>) => apiClient.post('/kb/rebuild', payload),
  ask: (payload: Record<string, unknown>) => apiClient.post('/ask', payload),
  requests: () => apiClient.get('/logs/requests'),
  requestDetail: (requestNo: string) => apiClient.get(`/logs/requests/${requestNo}`),
  retrievalLogs: (requestNo: string) => apiClient.get(`/logs/retrievals/${requestNo}`),
  graphLogs: (requestNo: string) => apiClient.get(`/logs/graph/${requestNo}`),
  evalRuns: () => apiClient.get('/eval/runs'),
  runEval: (payload: Record<string, unknown>) => apiClient.post('/eval/run', payload),
}
