<script lang="ts" setup>
import {z} from 'zod'
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {AccountType, ACCOUNT_TYPES} from "~/models/budget-account/account-type";
import {currencyIcon, currencyOptions} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import FormModal from "~/components/_molecules/modals/form-modal.vue";
import {useModalSubmit} from "~/composables/use-modal-submit";

const props = defineProps<{
  account: BudgetAccount | undefined;
}>();
const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const accountStore = useBudgetAccountsStore();
const {t} = useI18n();

const isOpen = defineModel<boolean>({required: true})

const state = reactive<{
  name: string;
  type: AccountType;
}>({
  name: props.account?.name ?? '',
  type: props.account?.type ?? AccountType.CHECKING,
})

// Re-seed the form whenever the modal is (re)opened for a given account.
function resetState() {
  if (!props.account) return;
  state.name = props.account.name;
  state.type = props.account.type;
}

const nameLabel = t('accounts.modals.edit.fieldNameLabel');

const schema = z.object({
  name: z.string().trim()
    .min(1, t('common.validation.required', {field: nameLabel}))
    .max(100, t('common.validation.maxLength', {field: nameLabel, max: 100})),
})

const activeIcon = computed(() => currencyIcon(props.account?.currency))
const typeOptions = computed(() => ACCOUNT_TYPES.map((value) => ({label: t(`accounts.types.${value}`), value})))

const {pending, submit} = useModalSubmit({
  open: isOpen,
  action: () => accountStore.updateAccount(props.account!.id, {name: state.name, type: state.type}),
  successTitle: t('accounts.modals.edit.toastSuccessTitle'),
  successBody: (updated) => t('accounts.modals.edit.toastSuccessBody', {name: updated.name}),
  errorTitle: t('accounts.modals.edit.toastErrorTitle'),
  errorBody: t('accounts.modals.edit.toastErrorBody'),
  onSuccess: () => emit('updated'),
});
</script>

<template>
  <FormModal v-model="isOpen"
             :description="t('accounts.modals.edit.description')"
             :title="t('accounts.modals.edit.title')"
             :schema="schema"
             :state="state"
             :loading="pending"
             :get-snapshot="() => ({...state})"
             :on-reset-on-open="resetState"
             :submit-label="t('accounts.modals.edit.submit')"
             :validation-error-title="t('accounts.modals.edit.validationErrorTitle')"
             :validation-error-body="t('accounts.modals.edit.validationErrorBody')"
             @submit="submit">
    <template #fields>
      <BaseInput name="name"
                 v-model="state.name"
                 :max-length="100"
                 autofocus
                 :label="t('accounts.modals.edit.fieldNameLabel')"
                 :placeholder="t('accounts.modals.edit.fieldNamePlaceholder')"
                 required
                 type="text"/>

      <UFormField :label="t('accounts.modals.edit.fieldTypeLabel')" name="type" required>
        <AppSelect v-model="state.type"
                 :items="typeOptions"
                 class="w-full"
                 :placeholder="t('accounts.modals.edit.fieldTypePlaceholder')"
                 required/>
      </UFormField>

      <UFormField :label="t('accounts.modals.edit.fieldCurrencyLabel')" name="currency">
        <AppSelect :model-value="props.account?.currency"
                 :icon="activeIcon"
                 :items="currencyOptions"
                 class="w-full"
                 disabled/>
        <template #help>{{ t('accounts.modals.edit.currencyLocked') }}</template>
      </UFormField>
    </template>
  </FormModal>
</template>
