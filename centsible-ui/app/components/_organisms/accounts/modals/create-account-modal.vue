<script lang="ts" setup>
import {Currency, currencyOptions} from "~/models/budget-account/currency";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useValidator} from "~/composables/use-validator";
import {useApiErrors} from "~/composables/use-api-errors";

const emit = defineEmits<{
  (e: 'created'): void;
}>();
const accountService = useBudgetAccountService(useApi());
const toast = useToasts();
const {t} = useI18n();

const isOpen = defineModel<boolean>({required: true})
const loading = ref(false);

const state = reactive<{
  name: string;
  initialBalance: number | undefined;
  currency: Currency;
}>({
  name: '',
  initialBalance: undefined,
  currency: Currency.EUR,
})

const nameInput = ref<InstanceType<typeof BaseInput>>();
const balanceInput = ref<InstanceType<typeof BaseInput>>();

const activeIcon = computed(() => currencyOptions.find(item => item.value === state.currency)?.icon)

async function onSubmit() {
  if (!useValidator().validateInputs([nameInput, balanceInput])) {
    toast.error(t('accounts.modals.create.validationErrorTitle'), t('accounts.modals.create.validationErrorBody'));
    return;
  }

  const {name, initialBalance, currency} = state;
  loading.value = true;
  try {
    const createdAccount = await accountService.createAccount({
      name,
      initialBalance: initialBalance as number,
      currency
    })
    toast.success(t('accounts.modals.create.toastSuccessTitle'), t('accounts.modals.create.toastSuccessBody', {name: createdAccount.name}));

    isOpen.value = false;
    navigateTo(`/${createdAccount.id}/dashboard`);
    emit('created');
  } catch (error) {
    useApiErrors().toastError(error, t('accounts.modals.create.toastErrorTitle'), t('accounts.modals.create.toastErrorBody'));
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
    :description="t('accounts.modals.create.description')"
    :title="t('accounts.modals.create.title')"
  >
    <template #body>
      <UForm id="account-form" :state="state" class="space-y-4 py-2 flex flex-col" @submit="onSubmit">
        <BaseInput ref="nameInput"
                   v-model="state.name"
                   :max-length="100"
                   autofocus
                   :label="t('accounts.modals.create.fieldNameLabel')"
                   :placeholder="t('accounts.modals.create.fieldNamePlaceholder')"
                   required
                   type="text"/>

        <BaseInput ref="balanceInput"
                   v-model="state.initialBalance"
                   :label="t('accounts.modals.create.fieldBalanceLabel')"
                   :placeholder="t('accounts.modals.create.fieldBalancePlaceholder')"
                   required
                   type="number"/>

        <UFormField :label="t('accounts.modals.create.fieldCurrencyLabel')" name="currency" required>
          <USelect v-model="state.currency"
                   :icon="activeIcon"
                   :items="currencyOptions"
                   class="w-full"
                   :placeholder="t('accounts.modals.create.fieldCurrencyPlaceholder')"
                   required/>
        </UFormField>
      </UForm>
    </template>
    <template #footer>
      <ModalFooterActions form="account-form"
                          :loading="loading"
                          :submit-label="t('accounts.modals.create.submit')"
                          @cancel="onCloseModal"/>
    </template>
  </UModal>
</template>
