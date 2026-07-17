<script lang="ts" setup>
import type {Currency} from "~/models/budget-account/currency";
import BalanceLineChart from "~/components/_molecules/charts/balance-line-chart.vue";
import UpcomingBillsList from "~/components/_molecules/reports/upcoming-bills-list.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import {useReportsStore} from "~/stores/reportsStore";
import {useChartTheme} from "~/composables/use-chart-theme";

const props = defineProps<{
  // Fallback display currency until the forecast (which is authoritative) has loaded.
  currency: Currency;
}>();

const reportsStore = useReportsStore();
const {t} = useI18n();

const HORIZONS = [3, 6, 12] as const;

const forecastCurrency = computed<Currency>(() => reportsStore.forecast?.currency ?? props.currency);
const points = computed(() => reportsStore.forecast?.points ?? []);
const occurrences = computed(() => reportsStore.forecast?.occurrences ?? []);

// points[0] is today's actual net worth; the last point is the projected horizon balance.
const endBalance = computed(() => points.value[points.value.length - 1]?.balance ?? 0);
const delta = computed(() => endBalance.value - (points.value[0]?.balance ?? 0));

const {primaryColor, withAlpha} = useChartTheme(forecastCurrency);
const lineColor = computed(() => primaryColor.value);
const lineFillColor = computed(() => withAlpha(primaryColor.value, 0.1));

function selectHorizon(months: number) {
  if (months === reportsStore.forecastMonths) return;
  reportsStore.fetchForecast(months);
}

onMounted(() => {
  if (!reportsStore.forecast) reportsStore.fetchForecast(reportsStore.forecastMonths);
});
</script>

<template>
  <UCard>
    <template #header>
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="text-base font-semibold text-highlighted">{{ t('reports.forecast.title') }}</h3>
          <p class="text-xs text-muted">{{ t('reports.forecast.subtitle') }}</p>
        </div>
        <UButtonGroup size="xs">
          <AppButton v-for="h in HORIZONS"
                   :key="h"
                   :color="h === reportsStore.forecastMonths ? 'primary' : 'neutral'"
                   :variant="h === reportsStore.forecastMonths ? 'solid' : 'outline'"
                   :label="t('reports.forecast.horizonShort', {count: h})"
                   @click="selectHorizon(h)"/>
        </UButtonGroup>
      </div>
    </template>

    <AppEmptyState v-if="reportsStore.forecastError"
                   icon="i-lucide-triangle-alert"
                   :title="t('reports.forecast.error')">
      <template #actions>
        <AppButton color="neutral"
                   variant="soft"
                   icon="i-lucide-refresh-cw"
                   @click="reportsStore.fetchForecast(reportsStore.forecastMonths)">
          {{ t('common.actions.retry') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <div v-else-if="!reportsStore.forecast" class="h-72 flex items-center justify-center">
      <UIcon name="i-lucide-loader-circle" class="w-6 h-6 animate-spin text-muted"/>
    </div>

    <div v-else
         class="grid grid-cols-1 lg:grid-cols-3 gap-6 transition-opacity"
         :class="{'opacity-60': reportsStore.forecastLoading}">
      <div class="lg:col-span-2">
        <div class="flex flex-wrap items-baseline gap-x-2 gap-y-1 mb-3">
          <span class="text-sm text-muted">
            {{ t('reports.forecast.projectedBalance', {count: reportsStore.forecastMonths}) }}
          </span>
          <span class="text-xl font-semibold tabular-nums text-highlighted">
            <BalanceNumberFormat :balance="endBalance" :currency="forecastCurrency"/>
          </span>
          <span class="inline-flex items-center gap-1 text-sm tabular-nums"
                :class="delta >= 0 ? 'text-success' : 'text-error'">
            <UIcon :name="delta >= 0 ? 'i-lucide-arrow-up-right' : 'i-lucide-arrow-down-right'"
                   class="w-3.5 h-3.5 shrink-0"/>
            <BalanceNumberFormat :balance="delta" :currency="forecastCurrency"/>
            <span class="text-muted">{{ t('reports.forecast.vsToday') }}</span>
          </span>
        </div>

        <BalanceLineChart :currency="forecastCurrency"
                          :legend-label="t('reports.forecast.legend')"
                          :points="points"
                          :color="lineColor"
                          :fill-color="lineFillColor"
                          dashed
                          height-class="h-72"/>
      </div>

      <div class="lg:col-span-1">
        <UpcomingBillsList :occurrences="occurrences" :currency="forecastCurrency"/>
      </div>
    </div>
  </UCard>
</template>
