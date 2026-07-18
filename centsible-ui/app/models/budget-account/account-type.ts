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

export const LIABILITY_TYPES: AccountType[] = [
  AccountType.CREDIT_CARD,
  AccountType.LOAN,
  AccountType.MORTGAGE,
];

export function isLiability(type: AccountType | undefined): boolean {
  return type != null && LIABILITY_TYPES.includes(type);
}
