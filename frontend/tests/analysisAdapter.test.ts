import { describe, expect, it } from 'vitest'

import { AnalysisAdapter } from '../src/api/adapters/analysis'

describe('AnalysisAdapter', () => {
  it('normalizes task status and review semantics', () => {
    const item = AnalysisAdapter.toTaskItem({
      taskId: 42,
      taskNo: 'TASK-42',
      taskStatusCode: 'SUCCESS',
      needsReview: true,
      total: 1,
    } as any)

    expect(item.id).toBe(42)
    expect(item.status).toBe('DONE')
    expect(item.needsReview).toBe(true)
  })

  it('normalizes grounded citations and treatment plans', () => {
    const detail = AnalysisAdapter.toDetail({
      task: {
        taskId: 7,
        taskNo: 'TASK-7',
        taskStatusCode: 'SUCCESS',
        visualAssets: [],
      },
      rawResultJson: {
        gradingLabel: 'C2',
        riskLevel: 'MEDIUM',
        knowledgeVersion: 'kb-v1',
        citations: [
          {
            rankNo: 1,
            docNo: 'DOC-1',
            docTitle: '风险管理',
            chunkText: '应结合风险因素安排复核。',
            score: 0.86,
            sourcePages: [2, 3],
          },
        ],
        treatmentPlan: [
          { priority: 'medium', title: '临床复核', details: '结合口内检查确认。' },
          { priority: 'low', title: 'invalid without details' },
        ],
      },
      timeline: [],
    } as any)

    expect(detail.summary.knowledgeVersion).toBe('kb-v1')
    expect(detail.summary.citations).toEqual([
      expect.objectContaining({ index: 1, title: '风险管理', sourcePages: [2, 3] }),
    ])
    expect(detail.summary.treatmentPlan).toEqual([
      { priority: 'MEDIUM', title: '临床复核', details: '结合口内检查确认。' },
    ])
  })
})
