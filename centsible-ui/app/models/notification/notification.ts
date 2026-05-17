export type NotificationType = 'BUDGET_THRESHOLD' | 'BUDGET_EXCEEDED';

export interface Notification {
  id: string;
  type: NotificationType;
  title: string;
  body: string;
  data: Record<string, string>;
  readAt: string | null;
  createdAt: string;
}
