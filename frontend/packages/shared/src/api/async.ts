import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type { AsyncJobStatusResponse } from '../types';

export function createAsyncJobsApi(apiClient: AxiosInstance) {
  return {
    getStatus: (jobId: string): Promise<AsyncJobStatusResponse> =>
      apiClient
        .get<ApiResponse<AsyncJobStatusResponse>>(`/async-jobs/${jobId}`)
        .then((response) => unwrapApiResponse(response.data)),
    getTilGenerationHistory: (summaryId: string): Promise<AsyncJobStatusResponse[]> =>
      apiClient
        .get<ApiResponse<AsyncJobStatusResponse[]>>(`/async-jobs/til-generations/${summaryId}`)
        .then((response) => unwrapApiResponse(response.data)),
    retryTilGeneration: (jobId: string): Promise<AsyncJobStatusResponse> =>
      apiClient
        .post<ApiResponse<AsyncJobStatusResponse>>(`/async-jobs/${jobId}/retry`)
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type AsyncJobsApi = ReturnType<typeof createAsyncJobsApi>;
