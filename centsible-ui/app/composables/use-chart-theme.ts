import type {Currency} from "~/models/budget-account/currency";

export function useChartTheme(currencyRef: MaybeRefOrGetter<Currency>) {
  const colorMode = useColorMode();
  const appConfig = useAppConfig();
  const localeTag = useLocaleTag();

  const isDark = computed(() => colorMode.value === 'dark');
  const tickColor = computed(() => isDark.value ? '#a3a3a3' : '#737373');
  const gridColor = computed(() => isDark.value ? '#262626' : '#e5e5e5');

  /**
   * Chart.js paints on <canvas>, which can't read CSS variables, so the app's design tokens are
   * resolved to concrete colors at runtime. A throwaway probe element lets the browser resolve the
   * full var() chain (e.g. --ui-color-primary-500 → --color-blush-500 → an rgb() string). Colors are
   * re-resolved whenever the palette (theme switch) or the light/dark mode changes.
   */
  const FALLBACK = {primary: '#ee387e', success: '#22c55e', error: '#ef4444'};

  function resolveToken(varName: string, fallback: string): string {
    if (!import.meta.client) return fallback;
    const probe = document.createElement('span');
    probe.style.cssText = 'position:absolute;visibility:hidden;pointer-events:none';
    probe.style.color = `var(${varName}, ${fallback})`;
    document.body.appendChild(probe);
    const resolved = getComputedStyle(probe).color;
    probe.remove();
    return resolved || fallback;
  }

  const series = ref<{primary: string; success: string; error: string}>({...FALLBACK});

  function resolveSeriesColors() {
    series.value = {
      primary: resolveToken('--ui-color-primary-500', FALLBACK.primary),
      success: resolveToken('--ui-color-success-500', FALLBACK.success),
      error: resolveToken('--ui-color-error-500', FALLBACK.error),
    };
  }

  if (import.meta.client) {
    onMounted(resolveSeriesColors);
    watch(
      [() => colorMode.value, () => appConfig.ui.colors?.primary],
      () => nextTick(resolveSeriesColors),
      {flush: 'post'},
    );
  }

  const primaryColor = computed(() => series.value.primary);
  const successColor = computed(() => series.value.success);
  const errorColor = computed(() => series.value.error);

  /** Add an alpha channel to a resolved rgb()/rgba() colour string (for area-chart fills). */
  function withAlpha(color: string, alpha: number): string {
    const nums = color.match(/[\d.]+/g);
    if (!nums || nums.length < 3) return color;
    return `rgba(${nums[0]}, ${nums[1]}, ${nums[2]}, ${alpha})`;
  }

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

  /**
   * The bottom legend config shared verbatim by every currency chart (point-style swatches, muted
   * tick colour, 11px). A builder so each chart reads the current `tickColor`.
   */
  function chartLegend() {
    return {
      position: 'bottom' as const,
      labels: {color: tickColor.value, usePointStyle: true, font: {size: 11}},
    };
  }

  return {
    isDark,
    tickColor,
    gridColor,
    primaryColor,
    successColor,
    errorColor,
    withAlpha,
    currencyFmt,
    pointerCursorOnHover,
    chartLegend,
  };
}
