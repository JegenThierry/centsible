<script lang="ts" setup>
import {computed} from 'vue';
import {DASHBOARD_PERIODS, type DashboardPeriod, useDashboardPeriod} from "~/composables/use-dashboard-period";

const {period} = useDashboardPeriod();
const {t} = useI18n();

const items = computed(() =>
  DASHBOARD_PERIODS.map((value) => ({value, label: t(`accounts.dashboard.periods.${value}`)})),
);
</script>

<template>
  <UTabs :content="false"
         :items="items"
         :model-value="period"
         size="xs"
         @update:model-value="period = $event as DashboardPeriod"/>
</template>
