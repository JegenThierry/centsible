<script lang="ts" setup>
import {z} from 'zod'
import {Currency, currencyIcon, currencyOptions} from "~/models/budget-account/currency";
import {AccountType, ACCOUNT_TYPES} from "~/models/budget-account/account-type";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import FormModal from "~/components/_molecules/modals/form-modal.vue";
import {useModalSubmit} from "~/composables/use-modal-submit";

const emit = defineEmits<{
  (e: 'created'): void;
}>();
const accountService = useBudgetAccountService(useApi());
const {t} = useI18n();

const isOpen = defineModel<boolean>({required: true})

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

const activeIcon = computed(() => currencyIcon(state.currency))
const typeOptions = computed(() => ACCOUNT_TYPES.map((value) => ({label: t(`accounts.types.${value}`), value})))

const {pending, submit} = useModalSubmit({
  open: isOpen,
  action: () => accountService.createAccount({
    name: state.name,
    initialBalance: state.initialBalance as number,
    currency: state.currency,
    type: state.type,
  }),
  successTitle: t('accounts.modals.create.toastSuccessTitle'),
  successBody: (account) => t('accounts.modals.create.toastSuccessBody', {name: account.name}),
  errorTitle: t('accounts.modals.create.toastErrorTitle'),
  errorBody: t('accounts.modals.create.toastErrorBody'),
  onSuccess: (account) => {
    navigateTo(`/${account.id}/dashboard`);
    emit('created');
  },
});
</script>

<template>
  <FormModal v-model="isOpen"
             :description="t('accounts.modals.create.description')"
             :title="t('accounts.modals.create.title')"
             :schema="schema"
             :state="state"
             :loading="pending"
             :get-snapshot="() => ({...state})"
             :on-reset-on-open="resetState"
             :submit-label="t('accounts.modals.create.submit')"
             :validation-error-title="t('accounts.modals.create.validationErrorTitle')"
             :validation-error-body="t('accounts.modals.create.validationErrorBody')"
             @submit="submit">
    <template #fields>
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
    </template>
  </FormModal>
</template>
