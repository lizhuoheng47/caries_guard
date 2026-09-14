import axios from 'axios'

export interface SegmentationRegion {
  regionIndex: number
  bbox: [number, number, number, number]
  polygon: number[][]
  score: number
}

export interface SegmentationResult {
  requestId: string
  modelCode: string
  implementationType: 'ML_MODEL'
  device: string
  inferenceMode: string
  maskThreshold: number
  segmentationScore: number
  regionCount: number
  regions: SegmentationRegion[]
  assets: {
    maskUrl: string
    overlayUrl: string
    heatmapUrl: string
  }
  needsReview: boolean
  limitations: string[]
}

interface ApiEnvelope<T> {
  code: string
  message: string
  data: T
  traceId?: string
}

interface SegmentationHealth {
  status: string
  ready: boolean
  modelCode: string
  implementationType: string
  device: string
}

const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
  timeout: 120_000,
})

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

const unwrap = <T>(response: { data: ApiEnvelope<T> }): T => {
  const envelope = response.data
  if (envelope.code !== '00000' || !envelope.data) {
    throw new Error(envelope.message || '分割服务返回异常')
  }
  return envelope.data
}

export const segmentationApi = {
  async health(): Promise<SegmentationHealth> {
    return unwrap(await client.get<ApiEnvelope<SegmentationHealth>>('/segmentation/health'))
  },

  async analyze(file: File): Promise<SegmentationResult> {
    const contentType = file.name.toLowerCase().endsWith('.dcm')
      ? 'application/dicom'
      : file.type
    if (!['image/png', 'image/jpeg', 'application/dicom'].includes(contentType)) {
      throw new Error('仅支持 PNG、JPG/JPEG 或 DICOM 文件')
    }
    return unwrap(
      await client.post<ApiEnvelope<SegmentationResult>>('/segmentation', file, {
        headers: { 'Content-Type': contentType },
      }),
    )
  },
}
