<script lang="ts" setup>
import {Currency, currencyOptions} from "~/models/budget-account/currency";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import {useValidator} from "~/composables/use-validator";
import {useApiErrors} from "~/composables/use-api-errors";

const emit = defineEmits<{
  (e: 'created'): void;
}>();
const api = useApi();
const accountService = useBudgetAccountService(api);
const accountStore = useBudgetAccountsStore();

const toast = useToasts();

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
    toast.error("Validation errors", "Please correct the highlighted fields.");
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
    toast.success("BudgetAccount created successfully.", `Your account: ${createdAccount.name} has been created`);

    isOpen.value = false;
    navigateTo(`/${createdAccount.id}/dashboard`);
    emit('created');
  } catch (error) {
    useApiErrors().toastError(error, "BudgetAccount not created.", "Account could not be created, please try again.");
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
    description="An account allows you to manage your budget."
    title="Create Budget Account"
  >
    <template #body>
      <UForm id="account-form" :state="state" class="space-y-4 py-2 flex flex-col" @submit="onSubmit">
        <BaseInput ref="nameInput"
                   v-model="state.name"
                   :max-length="100"
                   autofocus
                   label="Budget Account Name"
                   placeholder="Budget Account Name"
                   required
                   type="text"/>

        <BaseInput ref="balanceInput"
                   v-model="state.initialBalance"
                   label="Balance"
                   placeholder="Balance"
                   required
                   type="number"/>

        <UFormField label="Currency" name="currency" required>
          <USelect v-model="state.currency"
                   :icon="activeIcon"
                   :items="currencyOptions"
                   class="w-full"
                   placeholder="Currency"
                   required/>
        </UFormField>
      </UForm>
    </template>
    <template #footer>
      <CancelButton class="ml-auto" type="button" variant="subtle" @click="onCloseModal()"/>
      <UButton :loading="loading" form="account-form" type="submit">
        Create BudgetAccount
      </UButton>
    </template>
  </UModal>
</template>

<style scoped>

</style>
