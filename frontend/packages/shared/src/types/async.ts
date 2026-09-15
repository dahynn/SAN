export type AsyncJobType =
  | 'CARD_ANALYSIS'
  | 'TIL_GENERATION'
  | 'RECALL_GENERATION'
  | 'TIL_GITHUB_COMMIT'
  | 'STAR_RECOMMENDATION';
export type AsyncJobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AsyncJobStatusResponse {
  jobId: string;
  jobType: AsyncJobType | string;
  status: AsyncJobStatus | string;
  errorMessage: string | null;
  failureCategory: 'TIMEOUT' | 'EXTERNAL_SERVICE' | 'PROCESSING_ERROR' | null;
  attemptNumber: number;
  retryable: boolean;
  createdAt: string;
  startedAt: string | null;
  completedAt: string | null;
}
