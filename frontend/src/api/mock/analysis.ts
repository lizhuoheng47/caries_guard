import type { ApiResponse } from '../dto/base'
import type { AnalysisDetailViewDTO, AnalysisTaskPageDTO } from '../dto/analysis'

export const mockAnalysisApi = {
  getTasks(params: any): Promise<ApiResponse<AnalysisTaskPageDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        total: 3,
        pageNo: params.pageNo || 1,
        pageSize: params.pageSize || 10,
        records: [
          {
            taskId: 1001,
            taskNo: 'TSK-20260420-001',
            taskStatusCode: 'REVIEW',
            taskTypeCode: 'CARIES_ANALYSIS',
            modelVersion: 'caries-v1',
            createdAt: new Date().toISOString(),
            inferenceMillis: 1820
          },
          {
            taskId: 1002,
            taskNo: 'TSK-20260420-002',
            taskStatusCode: 'SUCCESS',
            taskTypeCode: 'CARIES_ANALYSIS',
            modelVersion: 'caries-v1',
            createdAt: new Date(Date.now() - 3600000).toISOString(),
            inferenceMillis: 1260
          },
          {
            taskId: 1003,
            taskNo: 'TSK-20260420-003',
            taskStatusCode: 'RUNNING',
            taskTypeCode: 'CARIES_ANALYSIS',
            modelVersion: 'caries-v1',
            createdAt: new Date(Date.now() - 60000).toISOString()
          }
        ]
      }
    })
  },

  getTaskDetail(taskId: string | number): Promise<ApiResponse<AnalysisDetailViewDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        task: {
          taskId: Number(taskId),
          taskNo: `TSK-${taskId}`,
          caseId: 501,
          taskStatusCode: 'REVIEW',
          taskTypeCode: 'CARIES_ANALYSIS',
          modelVersion: 'caries-v1',
          createdAt: new Date().toISOString(),
          inferenceMillis: 1820,
          visualAssets: [
            {
              assetTypeCode: 'OVERLAY',
              assetTypeLabel: 'Overlay',
              accessUrl: '/mock-xray-overlay.jpg'
            },
            {
              assetTypeCode: 'HEATMAP',
              assetTypeLabel: 'Heatmap',
              accessUrl: '/mock-xray-heatmap.jpg'
            }
          ]
        },
        patient: {
          patientIdMasked: 'P-***1023',
          patientNameMasked: 'Patient A',
          gender: 'F',
          age: 34
        },
        caseInfo: {
          caseId: 501,
          caseNo: 'CASE-2026-001',
          visitTime: new Date().toISOString()
        },
        image: {
          imageId: 801,
          imageUrl: '/mock-xray.jpg',
          sourceDevice: 'BITEWING'
        },
        analysisSummary: {
          overallHighestSeverity: 'C2',
          uncertaintyScore: 0.22,
          lesionCount: 2,
          abnormalToothCount: 2,
          riskLevel: 'HIGH',
          riskLevelLabel: 'High',
          gradingLabel: 'C2',
          confidenceScore: 0.85,
          needsReview: true,
          followUpRecommendation: 'Recommend dentist confirmation and prompt restorative treatment.',
          citations: [
            {
              rankNo: 1,
              docTitle: 'Clinical assessment note',
              chunkText: 'Moderate lesions should be clinically confirmed and treated promptly.',
              score: 0.92,
              sourceUri: 'case://assessment-note-001'
            }
          ]
        },
        rawResultJson: {
          annotationImageWidth: 512,
          annotationImageHeight: 256,
          lesionCount: 2,
          abnormalToothCount: 2,
          gradingLabel: 'C2',
          confidenceScore: 0.85,
          uncertaintyScore: 0.22,
          riskLevel: 'HIGH',
          riskFactors: [
            { factorName: 'Deep dentin involvement' },
            { factorName: 'Multiple affected teeth' }
          ],
          clinicalSummary: 'Two suspicious dentin-level lesions are visible in the posterior region.',
          followUpRecommendation: 'Recommend dentist confirmation and prompt restorative treatment.',
          treatmentPlan: [
            {
              priority: 'HIGH',
              title: 'Restorative treatment',
              details: 'Schedule prompt restorative treatment after chairside confirmation.'
            }
          ],
          lesionResults: [
            {
              toothCode: '16',
              severityCode: 'C2',
              confidenceScore: 0.88,
              uncertaintyScore: 0.12,
              lesionAreaPx: 3015,
              lesionAreaRatio: 0.023,
              bbox: [150, 80, 235, 150],
              polygon: [[150, 80], [235, 80], [235, 150], [150, 150]],
              summary: 'Approximate dentin-level lesion on tooth 16.',
              treatmentSuggestion: 'Composite restoration after clinical confirmation.'
            },
            {
              toothCode: '17',
              severityCode: 'C1',
              confidenceScore: 0.78,
              uncertaintyScore: 0.22,
              lesionAreaPx: 1840,
              lesionAreaRatio: 0.014,
              bbox: [268, 92, 332, 144],
              polygon: [[268, 92], [332, 92], [332, 144], [268, 144]],
              summary: 'Smaller enamel lesion on tooth 17.',
              treatmentSuggestion: 'Close follow-up and preventive care.'
            }
          ],
          citations: [
            {
              rankNo: 1,
              docTitle: 'Clinical assessment note',
              chunkText: 'Moderate lesions should be clinically confirmed and treated promptly.',
              score: 0.92,
              sourceUri: 'case://assessment-note-001'
            }
          ]
        },
        timeline: [
          {
            time: new Date(Date.now() - 20000).toISOString(),
            title: 'Created',
            content: 'Analysis task created',
            status: 'DONE'
          },
          {
            time: new Date(Date.now() - 10000).toISOString(),
            title: 'Inference',
            content: 'Python pipeline running',
            status: 'DONE'
          },
          {
            time: new Date().toISOString(),
            title: 'Completed',
            content: 'Java callback stored final result',
            status: 'DONE'
          }
        ]
      }
    })
  }
}
