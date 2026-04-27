import request from './request'
import { createMockAnalysisTask } from './mock/analysis'
import type { ApiResponse } from './dto/base'

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true'
const MOCK_STORAGE_PREFIX = 'dentai.mock.case-portal'

type MockPatientRecord = {
  patientId: string
  patientNo: string
  patientName: string
  genderCode?: 'MALE' | 'FEMALE'
  birthDate?: string
}

type MockVisitRecord = {
  visitId: string
  visitNo: string
  patientId: string
  complaint?: string
  visitDate: string
}

type MockCaseRecord = {
  caseId: string
  caseNo: string
  patientId: string
  visitId: string
  caseTitle?: string
  chiefComplaint?: string
}

type MockAttachmentRecord = {
  attachmentId: string
  fileName: string
  objectKey: string
  md5: string
  bucketName: string
  dataUrl?: string
}

type MockImageRecord = {
  imageId: string
  caseId: string
  patientId: string
  attachmentId: string
  imageTypeCode?: string
  imageUrl?: string
}

const storageKey = (name: string) => `${MOCK_STORAGE_PREFIX}:${name}`
const canUseStorage = () => typeof window !== 'undefined' && typeof localStorage !== 'undefined'

const readRecords = <T>(name: string): Record<string, T> => {
  if (!canUseStorage()) return {}
  try {
    const raw = localStorage.getItem(storageKey(name))
    return raw ? (JSON.parse(raw) as Record<string, T>) : {}
  } catch {
    return {}
  }
}

const writeRecords = <T>(name: string, value: Record<string, T>) => {
  if (!canUseStorage()) return
  localStorage.setItem(storageKey(name), JSON.stringify(value))
}

const nextMockId = (scope: string) => {
  const key = storageKey(`counter:${scope}`)
  if (!canUseStorage()) return `${Date.now()}`
  const next = Number(localStorage.getItem(key) || '5000') + 1
  localStorage.setItem(key, String(next))
  return String(next)
}

const pad = (value: string) => value.padStart(5, '0')

const fileToDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

export interface CreatePatientPayload {
  patientName: string;
  genderCode?: 'MALE' | 'FEMALE';
  birthDate?: string;
  sourceCode?: string;
  privacyLevelCode?: string;
  remark?: string;
}

export interface CreatePatientResponse {
  patientId: string;
  patientNo: string;
}

export interface CreateVisitPayload {
  patientId: string;
  doctorUserId?: number;
  visitTypeCode?: string;
  visitDate: string;
  complaint?: string;
  triageLevelCode?: string;
  sourceChannelCode?: string;
  remark?: string;
}

export interface CreateVisitResponse {
  visitId: string;
  visitNo: string;
}

export interface CreateCasePayload {
  visitId: string;
  patientId: string;
  caseTypeCode?: string;
  caseTitle?: string;
  chiefComplaint?: string;
  priorityCode?: string;
  clinicalNotes?: string;
  remark?: string;
}

export interface CreateCaseResponse {
  caseId: string;
  caseNo: string;
  caseStatusCode: string;
}

export interface UploadAttachmentResponse {
  attachmentId: string;
  fileName: string;
  bucketName: string;
  objectKey: string;
  md5: string;
}

export interface CreateCaseImagePayload {
  attachmentId: string;
  visitId: string;
  patientId: string;
  imageTypeCode?: string;
  imageSourceCode?: string;
  shootingTime?: string;
  bodyPositionCode?: string;
  primaryFlag?: string;
  remark?: string;
}

export interface CreateCaseImageResponse {
  imageId: string;
  qualityStatusCode: string;
}

export interface SaveImageQualityCheckPayload {
  checkTypeCode?: string;
  checkResultCode?: string;
  qualityScore?: number;
  blurScore?: number;
  exposureScore?: number;
  integrityScore?: number;
  occlusionScore?: number;
  issueCodes?: string[];
  suggestionText?: string;
  remark?: string;
}

export interface CreateAnalysisPayload {
  caseId: string;
  patientId: string;
  forceRetryFlag?: boolean;
  taskTypeCode?: string;
  remark?: string;
}

export interface CreateAnalysisResponse {
  taskId: string;
  taskNo: string;
  taskStatusCode: string;
}

const mockCreatePatient = async (payload: CreatePatientPayload): Promise<ApiResponse<CreatePatientResponse>> => {
  const patientId = nextMockId('patient')
  const patientNo = `PAT-${pad(patientId)}`
  const patients = readRecords<MockPatientRecord>('patients')
  patients[patientId] = {
    patientId,
    patientNo,
    patientName: payload.patientName,
    genderCode: payload.genderCode,
    birthDate: payload.birthDate,
  }
  writeRecords('patients', patients)
  return { code: '00000', message: 'success', data: { patientId, patientNo } }
}

const mockCreateVisit = async (payload: CreateVisitPayload): Promise<ApiResponse<CreateVisitResponse>> => {
  const visitId = nextMockId('visit')
  const visitNo = `VIS-${pad(visitId)}`
  const visits = readRecords<MockVisitRecord>('visits')
  visits[visitId] = {
    visitId,
    visitNo,
    patientId: payload.patientId,
    complaint: payload.complaint,
    visitDate: payload.visitDate,
  }
  writeRecords('visits', visits)
  return { code: '00000', message: 'success', data: { visitId, visitNo } }
}

const mockCreateCase = async (payload: CreateCasePayload): Promise<ApiResponse<CreateCaseResponse>> => {
  const caseId = nextMockId('case')
  const caseNo = `CASE-${new Date().getFullYear()}-${pad(caseId)}`
  const cases = readRecords<MockCaseRecord>('cases')
  cases[caseId] = {
    caseId,
    caseNo,
    patientId: payload.patientId,
    visitId: payload.visitId,
    caseTitle: payload.caseTitle,
    chiefComplaint: payload.chiefComplaint,
  }
  writeRecords('cases', cases)
  return { code: '00000', message: 'success', data: { caseId, caseNo, caseStatusCode: 'CREATED' } }
}

const mockUploadCaseFile = async (file: File): Promise<ApiResponse<UploadAttachmentResponse>> => {
  const attachmentId = nextMockId('attachment')
  const dataUrl = await fileToDataUrl(file)
  const attachments = readRecords<MockAttachmentRecord>('attachments')
  attachments[attachmentId] = {
    attachmentId,
    fileName: file.name,
    objectKey: `mock/${attachmentId}/${file.name}`,
    md5: `${attachmentId}-${file.size}`,
    bucketName: 'dentai-mock-bucket',
    dataUrl,
  }
  writeRecords('attachments', attachments)
  return {
    code: '00000',
    message: 'success',
    data: {
      attachmentId,
      fileName: file.name,
      bucketName: 'dentai-mock-bucket',
      objectKey: `mock/${attachmentId}/${file.name}`,
      md5: `${attachmentId}-${file.size}`,
    },
  }
}

const mockCreateCaseImage = async (caseId: string, payload: CreateCaseImagePayload): Promise<ApiResponse<CreateCaseImageResponse>> => {
  const imageId = nextMockId('image')
  const attachments = readRecords<MockAttachmentRecord>('attachments')
  const attachment = attachments[payload.attachmentId]
  const images = readRecords<MockImageRecord>('images')
  images[imageId] = {
    imageId,
    caseId,
    patientId: payload.patientId,
    attachmentId: payload.attachmentId,
    imageTypeCode: payload.imageTypeCode,
    imageUrl: attachment?.dataUrl,
  }
  writeRecords('images', images)
  return { code: '00000', message: 'success', data: { imageId, qualityStatusCode: 'PENDING' } }
}

const mockSaveImageQualityCheck = async (imageId: string) => {
  return {
    code: '00000',
    message: 'success',
    data: {
      imageId,
      qualityStatusCode: 'PASS',
    },
  }
}

const mockCreateAnalysis = async (caseId: string, payload: CreateAnalysisPayload): Promise<ApiResponse<CreateAnalysisResponse>> => {
  const cases = readRecords<MockCaseRecord>('cases')
  const patients = readRecords<MockPatientRecord>('patients')
  const images = readRecords<MockImageRecord>('images')
  const currentCase = cases[caseId]
  const patient = patients[payload.patientId]
  const image = Object.values(images).find((item) => item.caseId === caseId)

  const task = createMockAnalysisTask({
    caseNo: currentCase?.caseNo,
    patientId: patient?.patientNo || payload.patientId,
    patientName: patient?.patientName,
    chiefComplaint: currentCase?.chiefComplaint,
    imageUrl: image?.imageUrl,
    sourceDevice: image?.imageTypeCode || 'PANORAMIC',
    taskStatusCode: 'REVIEW',
    gradingLabel: 'C2',
    uncertaintyScore: 0.24,
    needsReview: true,
  })

  return {
    code: '00000',
    message: 'success',
    data: {
      taskId: String(task.taskId),
      taskNo: task.taskNo,
      taskStatusCode: task.taskStatusCode,
    },
  }
}

export const casePortalApi = {
  createPatient(payload: CreatePatientPayload): Promise<ApiResponse<CreatePatientResponse>> {
    if (USE_MOCK) return mockCreatePatient(payload)
    return request.post('/patients', payload)
  },

  createVisit(payload: CreateVisitPayload): Promise<ApiResponse<CreateVisitResponse>> {
    if (USE_MOCK) return mockCreateVisit(payload)
    return request.post('/visits', payload)
  },

  createCase(payload: CreateCasePayload): Promise<ApiResponse<CreateCaseResponse>> {
    if (USE_MOCK) return mockCreateCase(payload)
    return request.post('/cases', payload)
  },

  uploadCaseFile(file: File, caseId: string, imageTypeCode = 'PANORAMIC'): Promise<ApiResponse<UploadAttachmentResponse>> {
    if (USE_MOCK) return mockUploadCaseFile(file)
    const formData = new FormData();
    formData.append('file', file);
    formData.append('caseId', String(caseId));
    formData.append('imageTypeCode', imageTypeCode);
    return request.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
  },

  createCaseImage(caseId: string, payload: CreateCaseImagePayload): Promise<ApiResponse<CreateCaseImageResponse>> {
    if (USE_MOCK) return mockCreateCaseImage(caseId, payload)
    return request.post(`/cases/${caseId}/images`, payload);
  },

  saveImageQualityCheck(imageId: string, payload: SaveImageQualityCheckPayload) {
    if (USE_MOCK) return mockSaveImageQualityCheck(imageId)
    return request.post(`/images/${imageId}/quality-checks`, payload);
  },

  createAnalysis(caseId: string, payload: CreateAnalysisPayload): Promise<ApiResponse<CreateAnalysisResponse>> {
    if (USE_MOCK) return mockCreateAnalysis(caseId, payload)
    return request.post(`/cases/${caseId}/analysis`, payload);
  },
};
