import { createApiClient } from './client'

const ragClient = createApiClient('/api/v1/rag')

export const ragApi = {
  ask: (payload: Record<string, unknown>) => ragClient.post('/ask', payload),
  requests: () => ragClient.get('/logs/requests'),
  requestDetail: (requestNo: string) => ragClient.get(`/logs/requests/${requestNo}`),
  retrievalLogs: (requestNo: string) => ragClient.get(`/logs/retrievals/${requestNo}`),
  graphLogs: (requestNo: string) => ragClient.get(`/logs/graph/${requestNo}`),
  evalRuns: () => ragClient.get('/eval/runs'),
  runEval: (payload: Record<string, unknown>) => ragClient.post('/eval/run', payload),
}
