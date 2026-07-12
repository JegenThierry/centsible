<script lang="ts" setup>
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import type {BudgetAccount} from "~/models/budget-account/budget-account";
import {AccountType, ACCOUNT_TYPES} from "~/models/budget-account/account-type";
import {currencyOptions} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useApiErrors} from "~/composables/use-api-errors";

const props = defineProps<{
  account: BudgetAccount | undefined;
}>();
const emit = defineEmits<{
  (e: 'updated'): void;
}>();

const accountStore = useBudgetAccountsStore();
const toast = useToasts();
const {t} = useI18n();

const isOpen = defineModel<boolean>({required: true})
const loading = ref(false);

const state = reactive<{
  name: string;
  type: AccountType;
}>({
  name: props.account?.name ?? '',
  type: props.account?.type ?? AccountType.CHECKING,
})

// Re-seed the form whenever the modal is (re)opened for a given account.
watch(isOpen, (open) => {
  if (open && props.account) {
    state.name = props.account.name;
    state.type = props.account.type;
  }
})

const nameLabel = t('accounts.modals.edit.fieldNameLabel');

const schema = z.object({
  name: z.string().trim()
    .min(1, t('common.validation.required', {field: nameLabel}))
    .max(100, t('common.validation.maxLength', {field: nameLabel, max: 100})),
})
type Schema = z.output<typeof schema>

const currencyIcon = computed(() => currencyOptions.find(item => item.value === props.account?.currency)?.icon)
const typeOptions = computed(() => ACCOUNT_TYPES.map((value) => ({label: t(`accounts.types.${value}`), value})))

function onValidationError() {
  toast.error(t('accounts.modals.edit.validationErrorTitle'), t('accounts.modals.edit.validationErrorBody'));
}

async function onSubmit(_event: FormSubmitEvent<Schema>) {
  const account = props.account;
  if (!account) return;
  const {name, type} = state;
  loading.value = true;
  try {
    const updated = await accountStore.updateAccount(account.id, {name, type});
    toast.success(t('accounts.modals.edit.toastSuccessTitle'), t('accounts.modals.edit.toastSuccessBody', {name: updated.name}));
    isOpen.value = false;
    emit('updated');
  } catch (error) {
    useApiErrors().toastError(error, t('accounts.modals.edit.toastErrorTitle'), t('accounts.modals.edit.toastErrorBody'));
  } finally {
    loading.value = false;
  }
}

function onCloseModal() {
  isOpen.value = false
}
</script>

<template>
  <UModal
    v-model:open="isOpen"
    :close="{
        color: 'primary',
        variant: 'outline',
        class: 'rounded-full',
        onClick: onCloseModal,
      }"
    :description="t('accounts.modals.edit.description')"
    :title="t('accounts.modals.edit.title')"
  >
    <template #body>
      <UForm id="edit-account-form" :schema="schema" :state="state" class="space-y-4 py-2 flex flex-col" @submit="onSubmit" @error="onValidationError">
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
                   :icon="currencyIcon"
                   :items="currencyOptions"
                   class="w-full"
                   disabled/>
          <template #help>{{ t('accounts.modals.edit.currencyLocked') }}</template>
        </UFormField>
      </UForm>
    </template>
    <template #footer>
      <ModalFooterActions form="edit-account-form"
                          :loading="loading"
                          :submit-label="t('accounts.modals.edit.submit')"
                          @cancel="onCloseModal"/>
    </template>
  </UModal>
</template>
