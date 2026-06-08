<script lang="ts" setup>
import {useReportsStore} from "~/stores/reportsStore";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";

const reportsStore = useReportsStore();
const {t} = useI18n();

const s = computed(() => reportsStore.safeToSpend);
const positive = computed(() => (s.value?.safeToSpend ?? 0) >= 0);
</script>

<template>
  <CardSkeleton v-if="!s && reportsStore.pending"/>
  <UCard v-else-if="s">
    <div class="flex items-start justify-between gap-4">
      <div class="min-w-0">
        <p class="text-xs text-muted uppercase tracking-widest font-medium">
          {{ t('accounts.dashboard.safeToSpend.title') }}
        </p>
        <p :class="['text-2xl sm:text-3xl font-bold mt-1 truncate', positive ? 'text-success' : 'text-error']">
          <BalanceNumberFormat :balance="s.safeToSpend" :currency="s.currency"/>
        </p>
        <p class="text-xs text-muted mt-1">
          <BalanceNumberFormat :balance="s.dailyAllowance" :currency="s.currency"/>{{ t('accounts.dashboard.safeToSpend.perDay', {days: s.daysRemaining}) }}
        </p>
      </div>
      <div class="p-3 rounded-full shrink-0 bg-primary-100 dark:bg-primary-900/30 text-primary">
        <UIcon name="i-lucide-wallet" class="w-6 h-6 flex my-auto"/>
      </div>
    </div>

    <div class="grid grid-cols-3 gap-2 mt-4 pt-4 border-t border-default text-center">
      <div class="min-w-0">
        <p class="text-xs text-muted truncate">{{ t('accounts.dashboard.safeToSpend.expectedIncome') }}</p>
        <p class="text-sm font-medium text-success truncate">
          <BalanceNumberFormat :balance="s.expectedIncome" :currency="s.currency"/>
        </p>
      </div>
      <div class="min-w-0">
        <p class="text-xs text-muted truncate">{{ t('accounts.dashboard.safeToSpend.alreadySpent') }}</p>
        <p class="text-sm font-medium text-error truncate">
          <BalanceNumberFormat :balance="s.alreadySpent" :currency="s.currency"/>
        </p>
      </div>
      <div class="min-w-0">
        <p class="text-xs text-muted truncate">{{ t('accounts.dashboard.safeToSpend.upcomingExpenses') }}</p>
        <p class="text-sm font-medium text-error truncate">
          <BalanceNumberFormat :balance="s.upcomingExpenses" :currency="s.currency"/>
        </p>
      </div>
    </div>

    <p class="text-xs text-dimmed mt-3">{{ t('accounts.dashboard.safeToSpend.subtitle') }}</p>
  </UCard>
</template>
