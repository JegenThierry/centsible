<script lang="ts" setup>
import adze from 'adze'
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import type {Currency} from "~/models/budget-account/currency";
import type {Transaction} from "~/models/transactions/transaction";
import {useTransactionService} from "~/services/transactions/transaction-service";

const props = defineProps<{
  accountId: string;
  currency: Currency;
  categoryId: number | null;
  categoryName: string | null;
  fromDate: string | null;
  toDate: string | null;
}>();

const isOpen = defineModel<boolean>('open', {required: true});
const {t} = useI18n();
const service = useTransactionService(useApi());

const transactions = ref<Transaction[]>([]);
const loading = ref(false);

async function load() {
  if (!props.categoryId || !props.accountId) return;
  loading.value = true;
  try {
    transactions.value = await service.fetchTransactions(props.accountId, 1, 200, {
      categoryIds: [props.categoryId],
      fromDate: props.fromDate ?? undefined,
      toDate: props.toDate ?? undefined,
    });
  } catch (error) {
    adze.ns('dashboard').error('Failed to load category drill', error);
    transactions.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => [isOpen.value, props.categoryId, props.fromDate, props.toDate], ([open]) => {
  if (open) load();
});

const total = computed(() =>
  transactions.value.reduce((sum, t) => sum + Math.abs(Number(t.amount)), 0),
);
</script>

<template>
  <USlideover v-model:open="isOpen"
              :title="categoryName ?? t('reports.drillDown.title')"
              :description="t('reports.drillDown.title')">
    <template #body>
      <div v-if="loading" class="py-10 flex justify-center">
        <LoadingAnimation/>
      </div>

      <template v-else>
        <div v-if="transactions.length === 0" class="text-sm text-muted text-center py-6">
          {{ t('reports.drillDown.empty') }}
        </div>

        <ul v-else class="divide-y divide-default">
          <li v-for="tx in transactions"
              :key="tx.id"
              class="flex items-center justify-between py-2.5">
            <div class="min-w-0 flex-1">
              <p class="text-default truncate">{{ tx.description || '—' }}</p>
              <p class="text-xs text-muted">
                <FormattedDate :date="tx.transactionDate" format="date"/>
              </p>
            </div>
            <span class="font-medium tabular-nums shrink-0 ml-3">
              <BalanceNumberFormat :balance="Number(tx.amount)" :currency="currency"/>
            </span>
          </li>
        </ul>

        <div v-if="transactions.length > 0" class="flex items-center justify-between border-t border-default pt-3 mt-3">
          <span class="font-semibold">Total</span>
          <span class="font-bold">
            <BalanceNumberFormat :balance="total" :currency="currency"/>
          </span>
        </div>
      </template>
    </template>
  </USlideover>
</template>
