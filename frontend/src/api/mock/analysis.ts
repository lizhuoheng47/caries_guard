import type { ApiResponse } from '../dto/base'
import type { AnalysisDetailViewDTO, AnalysisTaskPageDTO } from '../dto/analysis'

type MockTaskSnapshot = {
  taskId: number
  taskNo: string
  taskStatusCode: 'SUCCESS' | 'RUNNING' | 'REVIEW' | 'FAILED' | 'PENDING'
  taskTypeCode: string
  modelVersion: string
  createdAt: string
  inferenceMillis?: number
  caseNo?: string
  patientName?: string
  patientId?: string
  gradingLabel?: string
  uncertaintyScore?: number
  needsReview?: boolean
  imageUrl?: string
  sourceDevice?: string
  chiefComplaint?: string
}

const STORAGE_KEY = 'dentai.mock.analysis.tasks'
const COUNTER_KEY = 'dentai.mock.analysis.counter'

const defaultTasks = (): MockTaskSnapshot[] => [
  {
    taskId: 1001,
    taskNo: 'TSK-20260420-001',
    taskStatusCode: 'REVIEW',
    taskTypeCode: 'CARIES_ANALYSIS',
    modelVersion: 'caries-v1',
    createdAt: new Date().toISOString(),
    inferenceMillis: 1820,
    caseNo: 'CASE-2026-001',
    patientName: '陈嘉宁',
    patientId: 'P-10023',
    gradingLabel: 'C2',
    uncertaintyScore: 0.22,
    needsReview: true,
    chiefComplaint: '后牙冷刺激敏感',
    sourceDevice: 'BITEWING',
  },
  {
    taskId: 1002,
    taskNo: 'TSK-20260420-002',
    taskStatusCode: 'SUCCESS',
    taskTypeCode: 'CARIES_ANALYSIS',
    modelVersion: 'caries-v1',
    createdAt: new Date(Date.now() - 3600000).toISOString(),
    inferenceMillis: 1260,
    caseNo: 'CASE-2026-002',
    patientName: '李安琪',
    patientId: 'P-10041',
    gradingLabel: 'C1',
    uncertaintyScore: 0.14,
    needsReview: false,
    chiefComplaint: '例行龋病筛查',
    sourceDevice: 'PANORAMIC',
  },
  {
    taskId: 1003,
    taskNo: 'TSK-20260420-003',
    taskStatusCode: 'RUNNING',
    taskTypeCode: 'CARIES_ANALYSIS',
    modelVersion: 'caries-v1',
    createdAt: new Date(Date.now() - 600000).toISOString(),
    caseNo: 'CASE-2026-003',
    patientName: '王诗雨',
    patientId: 'P-10052',
    gradingLabel: 'C0',
    uncertaintyScore: 0.09,
    needsReview: false,
    chiefComplaint: '术前影像检查',
    sourceDevice: 'INTRAORAL',
  }
]

const canUseStorage = () => typeof window !== 'undefined' && typeof localStorage !== 'undefined'

const saveSnapshots = (tasks: MockTaskSnapshot[]) => {
  if (!canUseStorage()) return
  localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks))
}

const ensureSnapshots = (): MockTaskSnapshot[] => {
  const defaults = defaultTasks().sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  if (!canUseStorage()) return defaults

  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      saveSnapshots(defaults)
      localStorage.setItem(COUNTER_KEY, String(1003))
      return defaults
    }

    const parsed = JSON.parse(raw) as MockTaskSnapshot[]
    if (!Array.isArray(parsed) || parsed.length === 0) {
      saveSnapshots(defaults)
      return defaults
    }
    return parsed.sort((a, b) => +new Date(b.createdAt) - +new Date(a.createdAt))
  } catch {
    saveSnapshots(defaults)
    return defaults
  }
}

const nextTaskId = () => {
  if (!canUseStorage()) return Math.floor(Date.now() / 1000)
  const current = Number(localStorage.getItem(COUNTER_KEY) || '1003') + 1
  localStorage.setItem(COUNTER_KEY, String(current))
  return current
}

const toTaskNo = (taskId: number) => `TSK-${new Date().toISOString().slice(0, 10).replace(/-/g, '')}-${String(taskId).slice(-4)}`

const riskLevelByGrade = (grade?: string) => {
  switch ((grade || '').toUpperCase()) {
    case 'C3':
    case 'C4':
    case 'G3':
    case 'G4':
      return { code: 'HIGH', label: '高风险' }
    case 'C2':
    case 'G2':
      return { code: 'MEDIUM', label: '中风险' }
    default:
      return { code: 'LOW', label: '低风险' }
  }
}

const buildLesions = (seed: number, grade = 'C2') => {
  const base = [
    {
      id: `lesion-${seed}-1`,
      toothCode: '16',
      severityCode: grade,
      confidenceScore: 0.88,
      uncertaintyScore: 0.12,
      lesionAreaPx: 3015,
      lesionAreaRatio: 0.023,
      bbox: [150, 80, 235, 150],
      polygon: [[150, 80], [235, 80], [235, 150], [150, 150]],
      summary: '后牙咬合面可见疑似龋坏病灶。',
      treatmentSuggestion: '建议椅旁确认后进行树脂充填修复。'
    },
    {
      id: `lesion-${seed}-2`,
      toothCode: '17',
      severityCode: grade === 'C0' ? 'C1' : 'C1',
      confidenceScore: 0.78,
      uncertaintyScore: 0.22,
      lesionAreaPx: 1840,
      lesionAreaRatio: 0.014,
      bbox: [268, 92, 332, 144],
      polygon: [[268, 92], [332, 92], [332, 144], [268, 144]],
      summary: '邻牙近中面存在浅表脱矿表现。',
      treatmentSuggestion: '建议随访观察并进行预防性干预。'
    }
  ]

  if ((grade || '').toUpperCase() === 'C0') {
    return [
      {
        ...base[1],
        id: `lesion-${seed}-0`,
        toothCode: '15',
        severityCode: 'C0',
        confidenceScore: 0.94,
        uncertaintyScore: 0.06,
        summary: '未见明确龋坏征象，牙体结构整体完整。',
        treatmentSuggestion: '建议常规复查与口腔卫生管理。'
      }
    ]
  }

  return base
}

export const listMockAnalysisTasks = () => ensureSnapshots()

export const createMockAnalysisTask = (input: Partial<MockTaskSnapshot>) => {
  const tasks = ensureSnapshots()
  const taskId = input.taskId ?? nextTaskId()
  const snapshot: MockTaskSnapshot = {
    taskId,
    taskNo: input.taskNo || toTaskNo(taskId),
    taskStatusCode: input.taskStatusCode || 'REVIEW',
    taskTypeCode: input.taskTypeCode || 'CARIES_ANALYSIS',
    modelVersion: input.modelVersion || 'caries-v1',
    createdAt: input.createdAt || new Date().toISOString(),
    inferenceMillis: input.inferenceMillis ?? 1480,
    caseNo: input.caseNo || `CASE-${taskId}`,
    patientName: input.patientName || '新建患者',
    patientId: input.patientId || `P-${taskId}`,
    gradingLabel: input.gradingLabel || 'C2',
    uncertaintyScore: input.uncertaintyScore ?? 0.24,
    needsReview: input.needsReview ?? true,
    imageUrl: input.imageUrl,
    sourceDevice: input.sourceDevice || 'PANORAMIC',
    chiefComplaint: input.chiefComplaint || '病例中心创建的影像分析任务',
  }

  const next = [snapshot, ...tasks.filter((item) => item.taskId !== taskId)]
  saveSnapshots(next)
  return snapshot
}

const getTaskSnapshot = (taskId: string | number) => {
  const normalized = Number(taskId)
  return ensureSnapshots().find((item) => item.taskId === normalized) || createMockAnalysisTask({ taskId: normalized, taskNo: `TSK-${normalized}` })
}

export const buildMockAnalysisDetail = (taskId: string | number): AnalysisDetailViewDTO => {
  const snapshot = getTaskSnapshot(taskId)
  const lesions = buildLesions(snapshot.taskId, snapshot.gradingLabel)
  const risk = riskLevelByGrade(snapshot.gradingLabel)
  const createdAt = snapshot.createdAt
  const completedAt = snapshot.taskStatusCode === 'RUNNING' ? undefined : new Date(new Date(createdAt).getTime() + (snapshot.inferenceMillis || 1500)).toISOString()
  const imageUrl = snapshot.imageUrl || '/mock-xray.jpg'

  return {
    task: {
      taskId: snapshot.taskId,
      taskNo: snapshot.taskNo,
      caseId: snapshot.taskId + 400,
      taskStatusCode: snapshot.taskStatusCode,
      taskTypeCode: snapshot.taskTypeCode,
      modelVersion: snapshot.modelVersion,
      createdAt,
      completedAt,
      inferenceMillis: snapshot.inferenceMillis,
      visualAssets: [
        {
          assetTypeCode: 'OVERLAY',
          assetTypeLabel: 'Overlay',
          accessUrl: imageUrl,
        },
        {
          assetTypeCode: 'HEATMAP',
          assetTypeLabel: 'Heatmap',
          accessUrl: imageUrl,
        }
      ]
    },
    patient: {
      patientIdMasked: snapshot.patientId,
      patientNameMasked: snapshot.patientName,
      gender: snapshot.taskId % 2 === 0 ? 'F' : 'M',
      age: 25 + (snapshot.taskId % 18)
    },
    caseInfo: {
      caseId: snapshot.taskId + 400,
      caseNo: snapshot.caseNo,
      visitTime: createdAt
    },
    image: {
      imageId: snapshot.taskId + 800,
      imageUrl,
      sourceDevice: snapshot.sourceDevice || 'PANORAMIC'
    },
    analysisSummary: {
      overallHighestSeverity: snapshot.gradingLabel,
      uncertaintyScore: snapshot.uncertaintyScore,
      lesionCount: lesions.length,
      abnormalToothCount: lesions.length,
      riskLevel: risk.code,
      riskLevelLabel: risk.label,
      gradingLabel: snapshot.gradingLabel,
      confidenceScore: Math.max(0.52, 1 - (snapshot.uncertaintyScore ?? 0.2) * 0.9),
      needsReview: snapshot.needsReview,
      followUpRecommendation: lesions.length > 1 ? '建议结合临床检查与叩诊结果尽快安排修复。' : '建议继续常规随访并强化预防。',
      citations: [
        {
          rankNo: 1,
          docTitle: 'DentAI mock knowledge note',
          chunkText: '该任务来自前端 mock 工作流，用于串联病例创建、分析详情、复核与报告页面。',
          score: 0.93,
          sourceUri: 'mock://dentai/knowledge/caries'
        }
      ]
    },
    rawResultJson: {
      annotationImageWidth: 512,
      annotationImageHeight: 256,
      lesionCount: lesions.length,
      abnormalToothCount: lesions.length,
      gradingLabel: snapshot.gradingLabel,
      confidenceScore: Math.max(0.52, 1 - (snapshot.uncertaintyScore ?? 0.2) * 0.9),
      uncertaintyScore: snapshot.uncertaintyScore,
      riskLevel: risk.code,
      riskFactors: [
        { factorName: '疑似龋损累及后牙区域' },
        { factorName: '影像对比度存在轻微波动' }
      ],
      clinicalSummary: snapshot.chiefComplaint || '影像显示后牙区可见疑似龋病灶，需要进一步临床确认。',
      followUpRecommendation: lesions.length > 1 ? '建议复核并评估修复时机。' : '建议加强预防性处理与复查。',
      treatmentPlan: [
        {
          priority: snapshot.needsReview ? 'HIGH' : 'MEDIUM',
          title: '临床确认与治疗排程',
          details: lesions.length > 1 ? '建议在 24 小时内完成医生复核并制定修复治疗计划。' : '建议按照随访周期安排复诊与再评估。'
        }
      ],
      lesionResults: lesions,
      citations: [
        {
          rankNo: 1,
          docTitle: 'DentAI mock knowledge note',
          chunkText: '该任务来自前端 mock 工作流，用于串联病例创建、分析详情、复核与报告页面。',
          score: 0.93,
          sourceUri: 'mock://dentai/knowledge/caries'
        }
      ]
    },
    timeline: [
      {
        time: createdAt,
        title: 'Task created',
        content: '分析任务已创建并进入工作流。',
        status: 'DONE'
      },
      {
        time: new Date(new Date(createdAt).getTime() + 800).toISOString(),
        title: 'Inference',
        content: snapshot.taskStatusCode === 'RUNNING' ? '模型推理进行中。' : '模型推理已完成。',
        status: snapshot.taskStatusCode === 'RUNNING' ? 'ACTIVE' : 'DONE'
      },
      {
        time: completedAt,
        title: 'Clinical review',
        content: snapshot.needsReview ? '任务已送入复核队列。' : '任务已完成，可直接生成报告。',
        status: snapshot.needsReview ? 'ACTIVE' : 'DONE'
      }
    ]
  }
}

export const mockAnalysisApi = {
  getTasks(params: any): Promise<ApiResponse<AnalysisTaskPageDTO>> {
    const all = ensureSnapshots()
    const pageNo = Number(params?.pageNo || params?.pageNum || 1)
    const pageSize = Number(params?.pageSize || 10)
    const start = (pageNo - 1) * pageSize
    const pageItems = all.slice(start, start + pageSize)

    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: {
        total: all.length,
        pageNo,
        pageSize,
        records: pageItems,
      }
    })
  },

  getTaskDetail(taskId: string | number): Promise<ApiResponse<AnalysisDetailViewDTO>> {
    return Promise.resolve({
      code: '00000',
      message: 'success',
      data: buildMockAnalysisDetail(taskId)
    })
  }
}
