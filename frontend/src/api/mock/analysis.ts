import type { ApiResponse, PageResult } from '../dto/base';
import type { AnalysisTaskListItemDTO, AnalysisDetailViewDTO } from '../dto/analysis';

export const mockAnalysisApi = {
  getTasks(params: any): Promise<ApiResponse<PageResult<AnalysisTaskListItemDTO>>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        total: 100,
        pageNum: params.pageNum || 1,
        pageSize: params.pageSize || 10,
        list: [
          {
            taskId: 1001,
            taskNo: 'TSK-20260419-001',
            patientNameMasked: '张*明',
            patientIdMasked: 'P-10023*',
            caseNo: 'CASE-2026-001',
            gradingLabel: 'G3',
            uncertaintyScore: 0.72,
            statusCode: 'REVIEW',
            createdAt: new Date().toISOString(),
            durationMs: 1250,
            needsReview: true,
          },
          {
            taskId: 1002,
            taskNo: 'TSK-20260419-002',
            patientNameMasked: '李*',
            patientIdMasked: 'P-10024*',
            caseNo: 'CASE-2026-002',
            gradingLabel: 'G1',
            uncertaintyScore: 0.15,
            statusCode: 'DONE',
            createdAt: new Date(Date.now() - 3600000).toISOString(),
            durationMs: 850,
            needsReview: false,
          },
          {
            taskId: 1003,
            taskNo: 'TSK-20260419-003',
            patientNameMasked: '王*花',
            patientIdMasked: 'P-10025*',
            caseNo: 'CASE-2026-003',
            statusCode: 'RUNNING',
            createdAt: new Date(Date.now() - 60000).toISOString(),
            needsReview: false,
          }
        ]
      }
    });
  },

  getTaskDetail(taskId: string | number): Promise<ApiResponse<AnalysisDetailViewDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        task: { taskId: Number(taskId), taskNo: `TSK-${taskId}`, statusCode: 'REVIEW', createdAt: new Date().toISOString() },
        patient: { patientId: 10023, nameMasked: '张*明', gender: 'M', age: 34 },
        caseInfo: { caseId: 501, caseNo: 'CASE-2026-001', visitTime: new Date().toISOString() },
        image: { imageId: 801, imageUrl: '/mock-xray.jpg' },
        analysisSummary: {
          gradingLabel: 'G3',
          confidenceScore: 0.85,
          uncertaintyScore: 0.72,
          needsReview: true,
          riskLevel: 'HIGH',
          riskFactors: ['Deep dentin involvement', 'Proximity to pulp']
        },
        timeline: [
          { nodeCode: 'UPLOAD', nodeName: '影像上传', status: 'COMPLETED' },
          { nodeCode: 'CREATE', nodeName: '任务创建', status: 'COMPLETED' },
          { nodeCode: 'INFERENCE', nodeName: '神经推理', status: 'COMPLETED' },
          { nodeCode: 'RAG', nodeName: 'RAG 解释', status: 'COMPLETED' },
          { nodeCode: 'REVIEW', nodeName: '医生复核', status: 'CURRENT' },
          { nodeCode: 'FEEDBACK', nodeName: '反馈留痕', status: 'PENDING' },
        ],
        ragHint: { enabled: true, latestAnswer: 'The AI detected a G3 lesion based on...' }
      }
    });
  }
};
