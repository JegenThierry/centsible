export enum AccountType {
  CHECKING = 'CHECKING',
  SAVINGS = 'SAVINGS',
  CASH = 'CASH',
  CREDIT_CARD = 'CREDIT_CARD',
  INVESTMENT = 'INVESTMENT',
  ASSET = 'ASSET',
  LOAN = 'LOAN',
  MORTGAGE = 'MORTGAGE',
  OTHER = 'OTHER',
}

export const ACCOUNT_TYPES: AccountType[] = Object.values(AccountType);

/** Liability-style accounts subtract from net worth — their balance magnitude is debt, not wealth. */
export const LIABILITY_TYPES: ReadonlySet<AccountType> = new Set([
  AccountType.CREDIT_CARD,
  AccountType.LOAN,
  AccountType.MORTGAGE,
]);

export function isLiabilityType(type: AccountType): boolean {
  return LIABILITY_TYPES.has(type);
}

/** Net-worth contribution: assets add their balance, liabilities subtract their magnitude. */
export function accountNetContribution(account: {type: AccountType; balance: number}): number {
  return isLiabilityType(account.type) ? -Math.abs(account.balance) : account.balance;
}
