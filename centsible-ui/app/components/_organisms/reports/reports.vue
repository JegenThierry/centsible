<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import NetWorthChart from "~/components/_organisms/reports/net-worth-chart.vue";
import ChartCardSkeleton from "~/components/_molecules/skeletons/chart-card-skeleton.vue";
import {useReportsStore} from "~/stores/reportsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {Currency} from "~/models/budget-account/currency";

const reportsStore = useReportsStore();
const accountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const ranges = [
  {key: '3m', months: 3},
  {key: '6m', months: 6},
  {key: '12m', months: 12},
  {key: '24m', months: 24},
] as const;

const selectedRange = ref<typeof ranges[number]['key']>('6m');

const displayCurrency = computed<Currency>(
  () => accountsStore.availableAccounts[0]?.currency ?? Currency.EUR
);

const rangeItems = computed(() =>
  ranges.map(r => ({label: t(`reports.ranges.${r.key}`), value: r.key}))
);

async function refresh() {
  const range = ranges.find(r => r.key === selectedRange.value) ?? ranges[1]!;
  await reportsStore.fetchNetWorth(range.months);
}

watch(selectedRange, () => refresh());

onMounted(async () => {
  const loadAccounts = accountsStore.availableAccounts.length === 0
    ? accountsStore.updateAvailableAccounts()
    : Promise.resolve();
  await Promise.all([loadAccounts, refresh()]);
});
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader
      :description="t('reports.page.description')"
      :title="t('reports.page.title')"
    >
      <template #actions>
        <USelect v-model="selectedRange" :items="rangeItems" class="w-40" value-key="value"/>
      </template>
    </PageHeader>

    <ChartCardSkeleton v-if="reportsStore.pending"/>
    <NetWorthChart
      v-else
      :currency="displayCurrency"
      :points="reportsStore.netWorth"
    />
  </UContainer>
</template>
