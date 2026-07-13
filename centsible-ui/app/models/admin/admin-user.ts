/** Mirrors the backend AdminUserDTO: account metadata and coarse counts, never financial data. */
export interface AdminUser {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  registered: boolean;
  totpEnabled: boolean;
  createdAt: string;
  lastLoginAt: string | null;
  lastSeenAt: string | null;
  /** When the user last recorded a transaction (creation time, not the booking date). */
  lastTransactionAt: string | null;
  accountCount: number;
  transactionCount: number;
}
