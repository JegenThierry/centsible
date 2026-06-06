// Mirrors the backend validation bounds for amount fields.
// Backend: TransactionForm.amount uses Digits(12, 2) with @DecimalMin("0.01") / @DecimalMax("999999999999.99")
// — widened so high-magnitude currencies (IDR, KRW) entered as the original amount aren't blocked.
// Backend: SetBalanceForm.newBalance uses Digits(13, 2) — the wider bound applies to account balances.
export const AMOUNT_INPUT = {
  min: 0.01,
  max: 999_999_999_999.99,
} as const;

export const BALANCE_INPUT = {
  min: -9_999_999_999_999.99,
  max: 9_999_999_999_999.99,
} as const;
