import { createApiClient } from './client'

const ragClient = createApiClient('/api/v1/rag')

export const ragApi = {
  doctorQa: (payload: Record<string, unknown>) => ragClient.post('/doctor-qa', payload),
  patientExplanation: (payload: Record<string, unknown>) => ragClient.post('/patient-explanation', payload),
  ask: (payload: Record<string, unknown>) => ragClient.post('/ask', payload),
  requests: () => ragClient.get('/logs/requests'),
  requestDetail: (requestNo: string) => ragClient.get(`/logs/requests/${requestNo}`),
  retrievalLogs: (requestNo: string) => ragClient.get(`/logs/retrievals/${requestNo}`),
  graphLogs: (requestNo: string) => ragClient.get(`/logs/graph/${requestNo}`),
  fusionLogs: (requestNo: string) => ragClient.get(`/logs/fusion/${requestNo}`),
  rerankLogs: (requestNo: string) => ragClient.get(`/logs/rerank/${requestNo}`),
  llmLogs: (requestNo: string) => ragClient.get(`/logs/llm/${requestNo}`),
  evalDatasets: () => ragClient.get('/eval/datasets'),
  evalDatasetDetail: (datasetId: number | string) => ragClient.get(`/eval/datasets/${datasetId}`),
  evalRuns: () => ragClient.get('/eval/runs'),
  evalRunDetail: (runNo: string) => ragClient.get(`/eval/runs/${runNo}`),
  evalRunResults: (runNo: string) => ragClient.get(`/eval/runs/${runNo}/results`),
  runEval: (payload: Record<string, unknown>) => ragClient.post('/eval/run', payload),
}
