import request from './request';
import { mockRagApi } from './mock/rag';
import type {
  DoctorQaCommandDTO,
  PatientExplanationCommandDTO,
  RagAnswerDTO,
  RagAskCommandDTO,
} from './dto/rag';
import type { ApiResponse } from './dto/base';

const USE_MOCK = import.meta.env.VITE_USE_MOCK === 'true';

const resolveMockQuestion = (
  payload: string | RagAskCommandDTO | DoctorQaCommandDTO | PatientExplanationCommandDTO
) => {
  if (typeof payload === 'string') return payload;
  return payload.question?.trim() || '请解释当前病例';
};

export const ragApi = {
  ask(payload: string | RagAskCommandDTO): Promise<ApiResponse<RagAnswerDTO>> {
    if (USE_MOCK) return mockRagApi.ask(resolveMockQuestion(payload));
    return request.post('/rag/ask', typeof payload === 'string' ? { question: payload } : payload);
  },

  doctorQa(payload: DoctorQaCommandDTO): Promise<ApiResponse<RagAnswerDTO>> {
    if (USE_MOCK) return mockRagApi.ask(resolveMockQuestion(payload));
    return request.post('/rag/doctor-qa', payload);
  },

  patientExplanation(payload: PatientExplanationCommandDTO): Promise<ApiResponse<RagAnswerDTO>> {
    if (USE_MOCK) return mockRagApi.ask(resolveMockQuestion(payload));
    return request.post('/rag/patient-explanation', payload);
  },
};
