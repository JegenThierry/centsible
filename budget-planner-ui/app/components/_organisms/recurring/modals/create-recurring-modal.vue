<script lang="ts" setup>
import {Frequency, type RecurringTransactionForm} from "~/models/recurring/recurring-transaction";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import RecurringFormFields from "~/components/_molecules/recurring/recurring-form.vue";
import {useRecurringTransactionService} from "~/services/recurring/recurring-transaction-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {todayIsoDate} from "~/utils/date";

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const api = useApi();
const service = useRecurringTransactionService(api);
const toasts = useToasts();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();

const form = ref<RecurringTransactionForm>(makeBlankForm());
const formRef = ref<InstanceType<typeof RecurringFormFields>>();
const loading = ref(false);
const formId = useId();
const activeCurrency = computed(() => budgetAccountsStore.activeAccount?.currency);

function makeBlankForm(): RecurringTransactionForm {
  return {
    amount: 0,
    description: '',
    category: undefined,
    frequency: Frequency.MONTHLY,
    startDate: todayIsoDate(),
    endDate: undefined,
    active: true,
  };
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => form.value,
  onResetOnOpen: () => { form.value = makeBlankForm(); },
});

async function handleSave() {
  if (loading.value) return;
  if (!formRef.value?.validate()) return;
  if (!budgetAccountsStore.activeAccount?.id) return;
  if (!form.value.category?.id || !form.value.startDate) return;

  loading.value = true;
  try {
    await service.create(budgetAccountsStore.activeAccount.id, {
      amount: form.value.amount,
      description: form.value.description,
      categoryId: form.value.category.id,
      frequency: form.value.frequency,
      startDate: form.value.startDate,
      endDate: form.value.endDate || null,
      active: form.value.active,
    });
    toasts.success(t('transactions.recurring.create.toastSuccessTitle'), t('transactions.recurring.create.toastSuccessBody'));
    emit('created');
    isOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('transactions.recurring.create.toastErrorTitle'), t('transactions.recurring.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t('transactions.recurring.create.description')"
          :title="t('transactions.recurring.create.title')"
          @update:open="requestClose">
    <template #body>
      <UForm :id="formId" :state="form" @submit="handleSave">
        <RecurringFormFields ref="formRef"
                             v-model="form"
                             :currency="activeCurrency"
                             :disabled="loading"/>
      </UForm>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton :disabled="loading" @click="requestClose(false)"/>
        <UButton :form="formId" :loading="loading" type="submit">{{ t('transactions.recurring.create.submit') }}</UButton>
      </div>
    </template>
  </UModal>
</template>
