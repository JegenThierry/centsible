import type {Currency} from "~/models/budget-account/currency";

export function useChartTheme(currencyRef: MaybeRefOrGetter<Currency>) {
  const colorMode = useColorMode();
  const localeTag = useLocaleTag();

  const isDark = computed(() => colorMode.value === 'dark');
  const tickColor = computed(() => isDark.value ? '#a3a3a3' : '#737373');
  const gridColor = computed(() => isDark.value ? '#262626' : '#e5e5e5');

  // Memoize Intl.NumberFormat by (locale, currency, fractionDigits). Building the formatter
  // is the expensive part; we want chartOptions recomputes (driven by theme/data ticks) to
  // reuse the same instance instead of allocating one each pass.
  const fmtCache = new Map<string, Intl.NumberFormat>();

  function currencyFmt(maximumFractionDigits = 0): Intl.NumberFormat {
    const locale = localeTag.value;
    const currency = toValue(currencyRef);
    const key = `${locale}|${currency}|${maximumFractionDigits}`;
    let fmt = fmtCache.get(key);
    if (!fmt) {
      fmt = new Intl.NumberFormat(locale, {
        style: 'currency',
        currency,
        maximumFractionDigits,
      });
      fmtCache.set(key, fmt);
    }
    return fmt;
  }

  // Chart.js hands the native event in `event.native`. Reading `.target.style` lets us swap
  // the cursor when hovering a clickable datapoint, which is what tells the user the slice is interactive.
  const pointerCursorOnHover = (_evt: unknown, elements: ArrayLike<unknown>) => {
    const target = ((_evt as {native?: Event} | undefined)?.native?.target ?? null) as HTMLElement | null;
    if (target?.style) {
      target.style.cursor = (elements?.length ?? 0) > 0 ? 'pointer' : 'default';
    }
  };

  return {
    isDark,
    tickColor,
    gridColor,
    currencyFmt,
    pointerCursorOnHover,
  };
}
