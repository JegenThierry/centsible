<script lang="ts" setup>
import adze from 'adze'
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import type {AccountBalanceAtDate} from "~/models/reports/account-balance-at-date";
import {isLiability} from "~/models/budget-account/account-type";
import {useReportsStore} from "~/stores/reportsStore";

const props = defineProps<{
  date: string | null;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const reportsStore = useReportsStore();
const {t} = useI18n();

const rows = ref<AccountBalanceAtDate[]>([]);
const loading = ref(false);

const assets = computed(() => rows.value.reduce((sum, r) =>
  isLiability(r.type) ? sum : sum + Number(r.convertedBalance ?? 0), 0));
const liabilities = computed(() => Math.abs(rows.value.reduce((sum, r) =>
  isLiability(r.type) ? sum + Number(r.convertedBalance ?? 0) : sum, 0)));
const net = computed(() => assets.value - liabilities.value);
const displayCurrency = computed(() => rows.value[0]?.targetCurrency ?? rows.value[0]?.currency);

async function load() {
  if (!props.date) return;
  loading.value = true;
  try {
    rows.value = await reportsStore.fetchNetWorthBreakdown(props.date);
  } catch (error) {
    adze.ns('reports').error('Failed to load breakdown', error);
    rows.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => [isOpen.value, props.date], ([open]) => {
  if (open) load();
});
</script>

<template>
  <USlideover v-model:open="isOpen" :title="t('reports.netWorth.breakdown.title')">
    <template #body>
      <div class="space-y-4">
        <div v-if="date" class="text-sm text-muted">
          <FormattedDate :date="date" format="date"/>
        </div>

        <div v-if="loading" class="py-10 flex justify-center">
          <LoadingAnimation/>
        </div>

        <template v-else>
          <ul class="divide-y divide-default">
            <li v-for="row in rows"
                :key="row.accountId"
                class="flex items-center justify-between py-2.5">
              <span class="text-default">{{ row.accountName }}</span>
              <span class="font-medium tabular-nums">
                <BalanceNumberFormat :balance="Number(row.balance)" :currency="row.currency"/>
              </span>
            </li>
          </ul>

          <div v-if="rows.length === 0" class="text-sm text-muted text-center py-6">
            {{ t('reports.netWorth.breakdown.empty') }}
          </div>

          <div v-else class="border-t border-default pt-3 space-y-2">
            <div class="flex items-center justify-between text-sm">
              <span class="text-muted">{{ t('reports.netWorth.assets') }}</span>
              <span class="font-medium tabular-nums">
                <BalanceNumberFormat v-if="displayCurrency" :balance="assets" :currency="displayCurrency"/>
              </span>
            </div>
            <div class="flex items-center justify-between text-sm">
              <span class="text-muted">{{ t('reports.netWorth.liabilities') }}</span>
              <span class="font-medium tabular-nums">
                <BalanceNumberFormat v-if="displayCurrency" :balance="liabilities" :currency="displayCurrency"/>
              </span>
            </div>
            <div class="flex items-center justify-between border-t border-default pt-2">
              <span class="font-semibold">{{ t('reports.netWorth.net') }}</span>
              <span class="font-bold tabular-nums">
                <BalanceNumberFormat v-if="displayCurrency" :balance="net" :currency="displayCurrency"/>
              </span>
            </div>
          </div>
        </template>
      </div>
    </template>
  </USlideover>
</template>
