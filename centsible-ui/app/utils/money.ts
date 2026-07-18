import {useMemoize} from '@vueuse/core';

export const AMOUNT_INPUT = {
  min: 0.01,
  max: 999_999_999_999.99,
} as const;

export const BALANCE_INPUT = {
  min: -9_999_999_999_999.99,
  max: 9_999_999_999_999.99,
} as const;

export const MONEY_FIELD_MAX = 9_999_999.99;

const currencyFormatter = useMemoize(
  (currency: string, locale: string) => new Intl.NumberFormat(locale, {style: 'currency', currency}),
);

/**
 * Formats an amount as a localized currency string, e.g. "€12.34". Falls back to a plain 2-decimal
 * string when the currency is unknown. Single home for the Intl currency options that were otherwise
 * hand-rolled across the atoms/molecules.
 */
export function formatCurrency(amount: number, currency: string | undefined, locale: string): string {
  return currency
    ? currencyFormatter(currency, locale).format(amount)
    : amount.toFixed(2);
}
