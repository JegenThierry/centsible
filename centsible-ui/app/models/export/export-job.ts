export type ExportType =
  | 'TRANSACTIONS'
  | 'LENDINGS_PER_CONTACT'
  | 'LENDINGS_ALL'
  | 'ACCOUNTS_SUMMARY';

export type JobStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';

export type ExportStatus = JobStatus;

export type PostProcessingType = 'SEND_EMAIL';

export type PostProcessingStatus = JobStatus;

export interface ExportPostProcessing {
  id: string;
  exportJobId: string;
  type: PostProcessingType;
  status: PostProcessingStatus;
  config: Record<string, unknown>;
  errorMessage: string | null;
  attemptCount: number;
  completedAt: string | null;
  createdAt: string;
  modifiedAt: string;
}

export interface ExportJob {
  id: string;
  userId: string;
  type: ExportType;
  status: ExportStatus;
  title: string;
  pdfFilename: string | null;
  errorMessage: string | null;
  attemptCount: number;
  completedAt: string | null;
  createdAt: string;
  modifiedAt: string;
  postProcessing: ExportPostProcessing[];
}

export interface PostProcessingRequest {
  type: PostProcessingType;
  config?: Record<string, unknown>;
}

export type ExportRequestParams =
  | ({ kind: 'TRANSACTIONS' } & TransactionsParams)
  | ({ kind: 'LENDINGS_PER_CONTACT' } & LendingsPerContactParams)
  | ({ kind: 'LENDINGS_ALL' } & LendingsAllParams)
  | ({ kind: 'ACCOUNTS_SUMMARY' } & AccountsSummaryParams);

export interface TransactionsParams {
  accountIds?: string[];
  fromDate?: string | null;
  toDate?: string | null;
  categoryIds?: number[];
}

export interface LendingsPerContactParams {
  contactId: string;
}

export interface LendingsAllParams {
  includeSettled?: boolean;
}

export interface AccountsSummaryParams {
  asOfDate?: string | null;
}

export interface CreateExportRequest {
  type: ExportType;
  title: string;
  params: ExportRequestParams;
  postProcessing?: PostProcessingRequest[];
}
