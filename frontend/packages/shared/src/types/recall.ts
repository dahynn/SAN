export type RecallQuizType = 'OX' | 'SHORT_ANSWER';

export interface RecallQuizGenerateRequest {
  targetDate: string;
  quizType: RecallQuizType;
}

export interface RecallQuizGenerationJobResponse {
  generationId: string;
  quizJobId: string;
  targetDate: string;
  quizType: RecallQuizType;
}

export interface RecallQuizResponse {
  quizId: string;
  scrapId: string;
  quizType: RecallQuizType;
  question: string;
  solved: boolean;
  correct: boolean | null;
  submittedAnswer: string | null;
  explanation: string | null;
}

export interface RecallQuizListResponse {
  targetDate: string;
  quizType: RecallQuizType;
  quizzes: RecallQuizResponse[];
}

export interface RecallQuizSubmitRequest {
  answer: string;
}

export interface RecallQuizSubmitResponse {
  quizId: string;
  quizType: RecallQuizType;
  question: string;
  solved: boolean;
  correct: boolean | null;
  submittedAnswer: string | null;
  explanation: string | null;
}
