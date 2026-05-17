import type {SelectItem} from "@nuxt/ui/components/Select.vue";

export enum Currency {
  EUR = 'EUR',
  USD = 'USD',
  JPY = 'JPY',
  GBP = 'GBP',
  AUD = 'AUD',
  CAD = 'CAD',
  CHF = 'CHF',
  CNY = 'CNY',
  HKD = 'HKD',
  NZD = 'NZD',
  SEK = 'SEK',
  NOK = 'NOK',
  DKK = 'DKK',
  SGD = 'SGD',
  KRW = 'KRW',
  INR = 'INR',
  MXN = 'MXN',
  BRL = 'BRL',
  ZAR = 'ZAR',
  TRY = 'TRY',
  PLN = 'PLN',
  PHP = 'PHP',
  IDR = 'IDR',
}

const FALLBACK_ICON = 'i-lucide-badge-dollar-sign';

const currencyIcons: Partial<Record<Currency, string>> = {
  [Currency.EUR]: 'i-lucide-badge-euro',
  [Currency.USD]: 'i-lucide-badge-dollar-sign',
  [Currency.JPY]: 'i-lucide-badge-japanese-yen',
  [Currency.GBP]: 'i-lucide-badge-pound-sterling',
  [Currency.CHF]: 'i-lucide-badge-swiss-franc',
  [Currency.INR]: 'i-lucide-badge-indian-rupee',
  [Currency.TRY]: 'i-lucide-badge-turkish-lira',
  [Currency.PHP]: 'i-lucide-philippine-peso',
};

export const currencyOptions = (Object.values(Currency) as Currency[]).map(code => ({
  label: code,
  value: code,
  icon: currencyIcons[code] ?? FALLBACK_ICON,
})) satisfies SelectItem[];
