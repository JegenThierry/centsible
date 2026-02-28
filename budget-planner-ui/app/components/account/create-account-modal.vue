<script setup lang="ts">
import {Currency, currencyOptions} from "~/models/account/currency";
import {useAccountService} from "~/services/account/account-service";
import {useToasts} from "~/services/toasts/toast-service";

const api = useApi();
const accountService = useAccountService(api);
const toast = useToasts();

const isOpen = defineModel<boolean>({required: true})
const loading = ref(false);

const state = reactive({
  name: undefined,
  initialBalance: undefined,
  currency: Currency.EUR,
})

const activeIcon = computed(() => currencyOptions.find(item => item.value === state.currency)?.icon)

function onSubmit() {
  const { name, initialBalance, currency } = state;

  if (initialBalance == undefined || name == null) {
    toast.error("Validation errors", "Not all fields are set");
    return;
  }

  accountService.createAccount({name, initialBalance, currency})
      .then((result) => toast.success("Account created successfully.", `Your account: ${result.name} has been created`))
      .catch(() => toast.error("Account not created.", `Account could not be created, please try again.`));
}

function onCloseModal() {
  isOpen.value = false
}
</script>

<template>
  <UModal
      v-model:open="isOpen"
      title="Create Account"
      description="An account allows you to manage your budget."
      :close="{
        color: 'primary',
        variant: 'outline',
        class: 'rounded-full',
        onClick: onCloseModal,
      }"
  >
    <template #body>
      <UForm id="account-form" :state="state" class="space-y-4 py-2 flex flex-col" @submit="onSubmit">
        <UFormField label="Account name" name="name">
          <UInput v-model="state.name"
                  required
                  placeholder="Account name"
                  class="w-full"/>
        </UFormField>

        <UFormField label="Balance" name="initialBalance">
          <UInputNumber v-model="state.initialBalance"
                        required
                        placeholder="Balance"
                        class="w-full"/>
        </UFormField>

        <UFormField label="Currency" name="currency">
          <USelect v-model="state.currency"
                   :items="currencyOptions"
                   :icon="activeIcon"
                   required
                   placeholder="Currency"
                   class="w-full"/>
        </UFormField>
      </UForm>
    </template>
    <template #footer>
      <UButton class="ml-auto" color="neutral" variant="subtle" type="button" @click="onCloseModal()">
        Cancel
      </UButton>
      <UButton :loading="loading" type="submit" form="account-form">
        Create Account
      </UButton>
    </template>
  </UModal>
</template>

<style scoped>

</style>