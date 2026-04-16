import type {SelectItem} from "@nuxt/ui/components/Select.vue";

export enum Currency {
  EUR = 'EUR',
  USD = 'USD',
  YEN = 'YEN',
}

export const currencyOptions = [
  {
    label: Currency.EUR.toString(),
    value: Currency.EUR,
    icon: 'i-lucide-badge-euro'
  },
  {
    label: Currency.USD.toString(),
    value: Currency.USD,
    icon: 'i-lucide-badge-dollar-sign'
  },
  {
    label: Currency.YEN.toString(),
    value: Currency.YEN,
    icon: 'i-lucide-badge-japanese-yen'
  },
] satisfies SelectItem[]
