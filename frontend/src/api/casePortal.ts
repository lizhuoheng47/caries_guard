import request from './request';
import type { ApiResponse } from './dto/base';

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

export const casePortalApi = {
  createPatient(payload: CreatePatientPayload): Promise<ApiResponse<CreatePatientResponse>> {
    return request.post('/patients', payload);
  },

  createVisit(payload: CreateVisitPayload): Promise<ApiResponse<CreateVisitResponse>> {
    return request.post('/visits', payload);
  },

  createCase(payload: CreateCasePayload): Promise<ApiResponse<CreateCaseResponse>> {
    return request.post('/cases', payload);
  },

  uploadCaseFile(file: File, caseId: string, imageTypeCode = 'PANORAMIC'): Promise<ApiResponse<UploadAttachmentResponse>> {
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
    return request.post(`/cases/${caseId}/images`, payload);
  },

  saveImageQualityCheck(imageId: string, payload: SaveImageQualityCheckPayload) {
    return request.post(`/images/${imageId}/quality-checks`, payload);
  },

  createAnalysis(caseId: string, payload: CreateAnalysisPayload): Promise<ApiResponse<CreateAnalysisResponse>> {
    return request.post(`/cases/${caseId}/analysis`, payload);
  },
};
