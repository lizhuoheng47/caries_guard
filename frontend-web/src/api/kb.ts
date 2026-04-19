import { createApiClient } from './client'

const kbClient = createApiClient('/api/v1/kb')

export const kbApi = {
  overview: (kbCode?: string) => kbClient.get('/overview', { params: { kbCode } }),
  documents: (params?: Record<string, unknown>) => kbClient.get('/documents', { params }),
  documentDetail: (id: string | number) => kbClient.get(`/documents/${id}`),
  importText: (payload: Record<string, unknown>) => kbClient.post('/documents/import-text', payload),
  updateDocument: (id: string | number, payload: Record<string, unknown>) => kbClient.put(`/documents/${id}`, payload),
  submitReview: (id: string | number, payload: Record<string, unknown>) => kbClient.post(`/documents/${id}/submit-review`, payload),
  approve: (id: string | number, payload: Record<string, unknown>) => kbClient.post(`/documents/${id}/approve`, payload),
  reject: (id: string | number, payload: Record<string, unknown>) => kbClient.post(`/documents/${id}/reject`, payload),
  publish: (id: string | number, payload: Record<string, unknown>) => kbClient.post(`/documents/${id}/publish`, payload),
  rollback: (id: string | number, payload: Record<string, unknown>) => kbClient.post(`/documents/${id}/rollback`, payload),
  upload: (formData: FormData) =>
    kbClient.post('/documents/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  ingestJobs: () => kbClient.get('/ingest-jobs'),
  rebuildJobs: (kbCode?: string) => kbClient.get('/rebuild-jobs', { params: { kbCode } }),
  rebuild: (payload: Record<string, unknown>) => kbClient.post('/rebuild', payload),
}
