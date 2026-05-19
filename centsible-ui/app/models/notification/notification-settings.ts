export interface NotificationSettings {
  largeTransactionThreshold: number | null;
  lowBalanceThreshold: number | null;
  loanDueDaysAhead: number;
  recurringDueDaysAhead: number;
}

export const DEFAULT_NOTIFICATION_SETTINGS: NotificationSettings = {
  largeTransactionThreshold: null,
  lowBalanceThreshold: null,
  loanDueDaysAhead: 3,
  recurringDueDaysAhead: 2,
};
