export const AMOUNT_INPUT = {
  min: 0.01,
  max: 999_999_999_999.99,
} as const;

export const BALANCE_INPUT = {
  min: -9_999_999_999_999.99,
  max: 9_999_999_999_999.99,
} as const;
