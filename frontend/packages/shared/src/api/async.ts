import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';
import type { AsyncJobStatusResponse } from '../types';

export function createAsyncJobsApi(apiClient: AxiosInstance) {
  return {
    getStatus: (jobId: string): Promise<AsyncJobStatusResponse> =>
      apiClient
        .get<ApiResponse<AsyncJobStatusResponse>>(`/async-jobs/${jobId}`)
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type AsyncJobsApi = ReturnType<typeof createAsyncJobsApi>;

