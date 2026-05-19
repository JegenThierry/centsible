<script lang="ts" setup>
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import {useLoansStore} from "~/stores/loansStore";
import {useActiveCurrency} from "~/composables/use-active-currency";

const loansStore = useLoansStore();
const currency = useActiveCurrency();
const {t} = useI18n();

const openLoans = computed(() => (loansStore.allLoans ?? []).filter(l => Number(l.outstanding) > 0));
const openCount = computed(() => openLoans.value.length);

const topLoans = computed(() =>
  [...openLoans.value]
    .sort((a, b) => Number(b.outstanding) - Number(a.outstanding))
    .slice(0, 4),
);

onMounted(async () => {
  const tasks: Promise<unknown>[] = [];
  if ((loansStore.allLoans ?? []).length === 0) {
    tasks.push(loansStore.refreshAllLoans().catch(e => console.error('Failed to load loans glance', e)));
  }
  tasks.push(loansStore.refreshOutstanding().catch(e => console.error('Failed to load outstanding total', e)));
  await Promise.all(tasks);
});
</script>

<template>
  <UCard v-if="loansStore.totalOutstanding > 0 || openCount > 0">
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="text-base font-semibold text-highlighted">
          {{ t('contacts.loansGlance.title') }}
        </h3>
        <UButton color="neutral" size="xs" variant="ghost" to="/loans">
          {{ t('contacts.loansGlance.viewAll') }}
        </UButton>
      </div>
    </template>

    <div class="space-y-3">
      <div class="flex items-baseline justify-between">
        <div>
          <p class="text-xs text-muted">{{ t('contacts.loansGlance.outstanding') }}</p>
          <p class="text-xl font-bold text-warning">
            <BalanceNumberFormat :balance="loansStore.totalOutstanding" :currency="currency"/>
          </p>
        </div>
        <p class="text-sm text-muted">
          {{ t('contacts.loansGlance.openCount', {count: openCount}, openCount) }}
        </p>
      </div>

      <div v-if="topLoans.length === 0" class="text-sm text-muted">
        {{ t('contacts.loansGlance.empty') }}
      </div>

      <ul v-else class="space-y-1.5">
        <li v-for="loan in topLoans"
            :key="loan.id"
            class="flex items-center justify-between text-sm">
          <NuxtLink :to="`/contacts/${loan.contact?.id}`"
                    class="truncate text-default hover:text-primary">
            {{ loan.contact?.name }}
          </NuxtLink>
          <span class="font-medium text-warning">
            <BalanceNumberFormat :balance="Number(loan.outstanding)" :currency="currency"/>
          </span>
        </li>
      </ul>
    </div>
  </UCard>
</template>
