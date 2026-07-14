export type NotificationType =
  | 'BUDGET_THRESHOLD'
  | 'BUDGET_EXCEEDED'
  | 'BUDGET_PACE'
  | 'LOAN_DUE'
  | 'RECURRING_UPCOMING'
  | 'LARGE_TRANSACTION'
  | 'LOW_ACCOUNT_BALANCE'
  | 'PROJECTED_SHORTFALL'
  | 'SYNC_FAILED'
  | 'CONSENT_EXPIRING';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  data: Record<string, string>;
  readAt: string | null;
  createdAt: string;
}
