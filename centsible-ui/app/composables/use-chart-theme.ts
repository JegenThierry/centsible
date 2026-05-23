import type {Currency} from "~/models/budget-account/currency";

export function useChartTheme(currencyRef: MaybeRefOrGetter<Currency>) {
  const colorMode = useColorMode();
  const localeTag = useLocaleTag();

  const isDark = computed(() => colorMode.value === 'dark');
  const tickColor = computed(() => isDark.value ? '#a3a3a3' : '#737373');
  const gridColor = computed(() => isDark.value ? '#262626' : '#e5e5e5');

  const fmtCache = new Map<string, Intl.NumberFormat>();

  /**
   * Memoised by (locale, currency, fractionDigits): chartOptions recomputes often (theme + tick
   * scale changes) and rebuilding Intl.NumberFormat each pass is the expensive bit.
   */
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

  /**
   * Chart.js exposes the original DOM event as `event.native`; mutating its target's cursor is
   * the supported way to signal that a hovered slice/point is clickable.
   */
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
