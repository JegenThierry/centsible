export const AMOUNT_INPUT = {
  min: 0.01,
  max: 999_999_999_999.99,
} as const;

export const BALANCE_INPUT = {
  min: -9_999_999_999_999.99,
  max: 9_999_999_999_999.99,
} as const;

/**
 * Upper bound for a single loan/budget/repayment money field (≈10M). Deliberately smaller than
 * {@link AMOUNT_INPUT}.max; single-sourced here because the same literal was repeated across every
 * loan/budget form input and its zod `.max(...)` rule.
 */
export const MONEY_FIELD_MAX = 9_999_999.99;

/**
 * Formats an amount as a localized currency string, e.g. "€12.34". Falls back to a plain 2-decimal
 * string when the currency is unknown. Single home for the Intl currency options that were otherwise
 * hand-rolled across the atoms/molecules.
 */
export function formatCurrency(amount: number, currency: string | undefined, locale: string): string {
  return currency
    ? new Intl.NumberFormat(locale, {style: 'currency', currency}).format(amount)
    : amount.toFixed(2);
}
