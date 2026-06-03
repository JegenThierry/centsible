<script lang="ts" setup>
import {computed} from 'vue';
import {DASHBOARD_PERIODS, type DashboardPeriod, useDashboardPeriod} from "~/composables/use-dashboard-period";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const {period} = useDashboardPeriod();
const {t} = useI18n();

const items = computed(() =>
  DASHBOARD_PERIODS.map((value) => ({value, label: t(`accounts.dashboard.periods.${value}`)})),
);

function select(value: DashboardPeriod) {
  period.value = value;
}
</script>

<template>
  <div class="inline-flex flex-wrap items-center gap-1 rounded-lg border border-neutral-200 dark:border-neutral-800 p-1">
    <AppButton
      v-for="item in items"
      :key="item.value"
      :color="period === item.value ? 'primary' : 'neutral'"
      :variant="period === item.value ? 'solid' : 'ghost'"
      size="xs"
      @click="select(item.value)"
    >
      {{ item.label }}
    </AppButton>
  </div>
</template>
