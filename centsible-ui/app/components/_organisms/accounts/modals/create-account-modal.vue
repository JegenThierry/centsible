<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {Currency, currencyOptions} from "~/models/budget-account/currency";
import {AccountType, ACCOUNT_TYPES} from "~/models/budget-account/account-type";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useApiErrors} from "~/composables/use-api-errors";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";

const emit = defineEmits<{
  (e: 'created'): void;
}>();
const accountService = useBudgetAccountService(useApi());
const toast = useToasts();
const {toastError} = useApiErrors();
const {t} = useI18n();

const isOpen = defineModel<boolean>({required: true})
const loading = ref(false);

const state = reactive<{
  name: string;
  initialBalance: number | undefined;
  currency: Currency;
  type: AccountType;
}>({
  name: '',
  initialBalance: undefined,
  currency: Currency.EUR,
  type: AccountType.CHECKING,
})

function resetState() {
  state.name = '';
  state.initialBalance = undefined;
  state.currency = Currency.EUR;
  state.type = AccountType.CHECKING;
}

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => ({...state}),
  onResetOnOpen: resetState,
});

const nameLabel = t('accounts.modals.create.fieldNameLabel');
const balanceLabel = t('accounts.modals.create.fieldBalanceLabel');

const schema = z.object({
  name: z.string().trim()
    .min(1, t('common.validation.required', {field: nameLabel}))
    .max(100, t('common.validation.maxLength', {field: nameLabel, max: 100})),
  initialBalance: z.preprocess(
    (v) => (v === '' || v === null ? undefined : v),
    z.number({
      error: (issue) => issue.input === undefined
        ? t('common.validation.required', {field: balanceLabel})
        : t('common.validation.number', {field: balanceLabel}),
    }),
  ),
})
type Schema = z.output<typeof schema>

const activeIcon = computed(() => currencyOptions.find(item => item.value === state.currency)?.icon)
const typeOptions = computed(() => ACCOUNT_TYPES.map((value) => ({label: t(`accounts.types.${value}`), value})))

function onValidationError() {
  toast.error(t('accounts.modals.create.validationErrorTitle'), t('accounts.modals.create.validationErrorBody'));
}

async function onSubmit(_event: FormSubmitEvent<Schema>) {
  const {name, initialBalance, currency, type} = state;
  loading.value = true;
  try {
    const createdAccount = await accountService.createAccount({
      name,
      initialBalance: initialBalance as number,
      currency,
      type
    })
    toast.success(t('accounts.modals.create.toastSuccessTitle'), t('accounts.modals.create.toastSuccessBody', {name: createdAccount.name}));

    isOpen.value = false;
    navigateTo(`/${createdAccount.id}/dashboard`);
    emit('created');
  } catch (error) {
    toastError(error, t('accounts.modals.create.toastErrorTitle'), t('accounts.modals.create.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal
    :open="isOpen"
    :close="{
        color: 'primary',
        variant: 'outline',
        class: 'rounded-full',
      }"
    :description="t('accounts.modals.create.description')"
    :title="t('accounts.modals.create.title')"
    @update:open="requestClose"
  >
    <template #body>
      <UForm id="account-form" :schema="schema" :state="state" class="space-y-4 py-2 flex flex-col" @submit="onSubmit" @error="onValidationError">
        <BaseInput name="name"
                   v-model="state.name"
                   :max-length="100"
                   autofocus
                   :label="t('accounts.modals.create.fieldNameLabel')"
                   :placeholder="t('accounts.modals.create.fieldNamePlaceholder')"
                   required
                   type="text"/>

        <BaseInput name="initialBalance"
                   v-model="state.initialBalance"
                   :label="t('accounts.modals.create.fieldBalanceLabel')"
                   :placeholder="t('accounts.modals.create.fieldBalancePlaceholder')"
                   required
                   type="number"/>

        <UFormField :label="t('accounts.modals.create.fieldCurrencyLabel')" name="currency" required>
          <AppSelect v-model="state.currency"
                   :icon="activeIcon"
                   :items="currencyOptions"
                   class="w-full"
                   :placeholder="t('accounts.modals.create.fieldCurrencyPlaceholder')"
                   required/>
        </UFormField>

        <UFormField :label="t('accounts.modals.create.fieldTypeLabel')" name="type" required>
          <AppSelect v-model="state.type"
                   :items="typeOptions"
                   class="w-full"
                   :placeholder="t('accounts.modals.create.fieldTypePlaceholder')"
                   required/>
        </UFormField>
      </UForm>
    </template>
    <template #footer>
      <ModalFooterActions form="account-form"
                          :loading="loading"
                          :submit-label="t('accounts.modals.create.submit')"
                          @cancel="requestClose(false)"/>
    </template>
  </UModal>
</template>
