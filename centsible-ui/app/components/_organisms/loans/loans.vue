<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import AllLoansTable from "~/components/_organisms/loans/all-loans-table.vue";
const CreateLoanModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/create-loan-modal.vue"));
const RecordRepaymentModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/record-repayment-modal.vue"));
const DeleteLoanModal = defineAsyncComponent(() => import("~/components/_organisms/loans/modals/delete-loan-modal.vue"));
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import {useLoansStore} from "~/stores/loansStore";
import {useActiveCurrency} from "~/composables/use-active-currency";
import {type Loan, type LoanStatus, loanStatus} from "~/models/loan/loan";

const loansStore = useLoansStore();
const currency = useActiveCurrency();
const {t} = useI18n();

type Filter = 'all' | LoanStatus;
const filter = ref<Filter>('all');

const filterItems = computed(() => [
  {label: t('contacts.loansPage.filterAll'), value: 'all' as Filter},
  {label: t('contacts.loansPage.filterOpen'), value: 'open' as Filter},
  {label: t('contacts.loansPage.filterPartial'), value: 'partial' as Filter},
  {label: t('contacts.loansPage.filterSettled'), value: 'settled' as Filter},
]);

const filteredLoans = computed<Loan[]>(() => {
  const loans = loansStore.allLoans ?? [];
  if (filter.value === 'all') return loans;
  return loans.filter(l => loanStatus(l) === filter.value);
});

const stats = computed(() => {
  const all = loansStore.allLoans ?? [];
  const outstanding = all.reduce((sum, l) => sum + Number(l.outstanding ?? 0), 0);
  const open = all.filter(l => Number(l.outstanding) > 0).length;
  const contacts = new Set(all.filter(l => Number(l.outstanding) > 0).map(l => l.contact?.id)).size;
  return {outstanding, open, contacts};
});

const isCreateOpen = ref(false);
const isRepayOpen = ref(false);
const isDeleteOpen = ref(false);
const selected = ref<Loan>();

function openRepay(loan: Loan) {
  selected.value = loan;
  isRepayOpen.value = true;
}

function openDelete(loan: Loan) {
  selected.value = loan;
  isDeleteOpen.value = true;
}

function openContact(loan: Loan) {
  const id = loan.contact?.id;
  if (id) navigateTo(`/contacts/${id}`);
}

async function refresh() {
  await Promise.all([
    loansStore.refreshAllLoans(),
    loansStore.refreshOutstanding(),
  ]);
}

onMounted(() => refresh());
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader
      :description="t('contacts.loansPage.description')"
      :title="t('contacts.loansPage.title')"
    >
      <template #actions>
        <USelect v-model="filter" :items="filterItems" class="w-48" value-key="value"/>
        <UButton color="primary"
                 icon="i-lucide-hand-coins"
                 @click="isCreateOpen = true">
          {{ t('contacts.page.recordLending') }}
        </UButton>
      </template>
    </PageHeader>

    <div v-if="loansStore.pending && (loansStore.allLoans?.length ?? 0) === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <template v-else>
      <div class="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <UCard>
          <p class="text-xs text-muted">{{ t('contacts.loansPage.stats.totalOutstanding') }}</p>
          <p class="text-lg font-bold text-warning">
            <BalanceNumberFormat :balance="stats.outstanding" :currency="currency"/>
          </p>
        </UCard>
        <UCard>
          <p class="text-xs text-muted">{{ t('contacts.loansPage.stats.openLoans') }}</p>
          <p class="text-lg font-bold">{{ stats.open }}</p>
        </UCard>
        <UCard>
          <p class="text-xs text-muted">{{ t('contacts.loansPage.stats.contactsWithLoans') }}</p>
          <p class="text-lg font-bold">{{ stats.contacts }}</p>
        </UCard>
      </div>

      <AllLoansTable :loans="filteredLoans"
                     :loading="loansStore.pending"
                     @delete="openDelete"
                     @open-contact="openContact"
                     @repay="openRepay"/>
    </template>

    <CreateLoanModal v-if="isCreateOpen"
                     v-model:open="isCreateOpen"
                     @created="refresh"/>
    <RecordRepaymentModal v-if="isRepayOpen"
                          v-model:open="isRepayOpen"
                          :loan="selected"
                          @recorded="refresh"/>
    <DeleteLoanModal v-if="isDeleteOpen"
                     v-model:open="isDeleteOpen"
                     :loan="selected"
                     @deleted="refresh"/>
  </UContainer>
</template>
