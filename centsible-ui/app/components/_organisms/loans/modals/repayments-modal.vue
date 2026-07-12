<script lang="ts" setup>
import type {Loan, Repayment} from "~/models/loan/loan";
import {Currency} from "~/models/budget-account/currency";
import {useLoansStore} from "~/stores/loansStore";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import FormattedDate from "~/components/_atoms/labels/formatted-date.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";

const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const props = defineProps<{
  loan: Loan | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const loansStore = useLoansStore();
const {t} = useI18n();

const loading = ref(false);
const repaymentToDelete = ref<Repayment | null>(null);
const isDeleteOpen = ref(false);

const repayments = computed<Repayment[]>(() =>
  props.loan ? (loansStore.repaymentsByLoan[props.loan.id] ?? []) : [],
);

watch(isOpen, async (open) => {
  if (!open || !props.loan) return;
  loading.value = true;
  try {
    await loansStore.refreshRepayments(props.loan.id);
  } catch {
  } finally {
    loading.value = false;
  }
});

function askDelete(repayment: Repayment) {
  repaymentToDelete.value = repayment;
  isDeleteOpen.value = true;
}

async function confirmDelete() {
  const repayment = repaymentToDelete.value;
  if (!repayment || !props.loan) return;
  await loansStore.deleteRepayment(props.loan.id, repayment.id, props.loan.contact.id);
}
</script>

<template>
  <UModal v-model:open="isOpen" :title="t('contacts.loans.repayments.title')">
    <template #body>
      <div v-if="loading" class="flex justify-center py-8">
        <LoadingAnimation/>
      </div>
      <p v-else-if="repayments.length === 0" class="text-sm text-muted py-6 text-center">
        {{ t('contacts.loans.repayments.empty') }}
      </p>
      <ul v-else class="divide-y divide-default">
        <li v-for="r in repayments" :key="r.id" class="flex items-center justify-between gap-3 py-2.5">
          <div class="min-w-0">
            <p class="text-sm font-medium tabular-nums">
              <BalanceNumberFormat :balance="Number(r.amount)" :currency="r.currency ?? loan?.currency ?? Currency.EUR"/>
            </p>
            <p class="text-xs text-muted flex items-center gap-2">
              <FormattedDate v-if="r.repaidAt" :date="r.repaidAt" format="date"/>
              <span v-if="!r.affectsBalance" class="text-dimmed">· {{ t('contacts.loans.table.trackingOnly') }}</span>
            </p>
          </div>
          <AppButton :aria-label="t('contacts.loans.repayments.deleteAria')"
                     color="error"
                     icon="i-lucide-trash"
                     size="sm"
                     variant="ghost"
                     @click="askDelete(r)"/>
        </li>
      </ul>
    </template>

    <template #footer>
      <div class="flex justify-end">
        <AppButton color="neutral" variant="soft" @click="isOpen = false">
          {{ t('common.actions.close') }}
        </AppButton>
      </div>
    </template>
  </UModal>

  <ConfirmationModal v-if="isDeleteOpen"
                     v-model:open="isDeleteOpen"
                     :title="t('common.confirmDelete.title')"
                     :body="t('contacts.loans.repayments.deleteConfirm')"
                     :confirm-label="t('common.actions.delete')"
                     :delete-callback="confirmDelete"
                     :manage-toasts="false"/>
</template>
