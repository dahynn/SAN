import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type {
  RecallQuizGenerateRequest,
  RecallQuizGenerationJobResponse,
  RecallQuizListResponse,
  RecallQuizSubmitRequest,
  RecallQuizSubmitResponse,
  RecallQuizType,
} from '../types';

export function createRecallApi(apiClient: AxiosInstance) {
  return {
    requestQuizGeneration: (payload: RecallQuizGenerateRequest): Promise<RecallQuizGenerationJobResponse> =>
      apiClient
        .post<ApiResponse<RecallQuizGenerationJobResponse>>('/recall/quizzes', payload)
        .then((response) => unwrapApiResponse(response.data)),

    getQuizzes: (targetDate: string, quizType: RecallQuizType): Promise<RecallQuizListResponse> =>
      apiClient
        .get<ApiResponse<RecallQuizListResponse>>('/recall/quizzes', { params: { targetDate, quizType } })
        .then((response) => unwrapApiResponse(response.data)),

    submitQuiz: (quizId: string, payload: RecallQuizSubmitRequest): Promise<RecallQuizSubmitResponse> =>
      apiClient
        .post<ApiResponse<RecallQuizSubmitResponse>>(`/recall/quizzes/${quizId}/submissions`, payload)
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type RecallApi = ReturnType<typeof createRecallApi>;
