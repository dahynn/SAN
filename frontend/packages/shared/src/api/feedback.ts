import type { AxiosInstance } from 'axios';
import { unwrapApiResponse, type ApiResponse } from './client';

export type FeedbackType = 'BUG' | 'INCONVENIENCE' | 'FEATURE_REQUEST' | 'ETC';

export interface FeedbackCreateRequest {
  type: FeedbackType;
  content: string;
  contact?: string | null;
  pageUrl?: string | null;
}

export interface FeedbackCreateResponse {
  feedbackId: string;
}

export function createFeedbackApi(apiClient: AxiosInstance) {
  return {
    create: (payload: FeedbackCreateRequest): Promise<FeedbackCreateResponse> =>
      apiClient
        .post<ApiResponse<FeedbackCreateResponse>>('/feedback', payload)
        .then((response) => unwrapApiResponse(response.data)),
  };
}

export type FeedbackApi = ReturnType<typeof createFeedbackApi>;
