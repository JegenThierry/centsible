<script lang="ts" setup>
import CurrencySelect from "~/components/_atoms/inputs/currency-select.vue";
import {useUserStore} from "~/stores/userStore";
import {useToasts} from "~/services/toasts/toast-service";
import {Currency} from "~/models/budget-account/currency";

const {t} = useI18n();
const userStore = useUserStore();
const {success, error} = useToasts();

const selected = ref<Currency>(userStore.user?.defaultCurrency ?? Currency.EUR);
const saving = ref(false);

watch(() => userStore.user?.defaultCurrency, (currency) => {
  if (currency) selected.value = currency;
});

async function onChange(currency: Currency) {
  if (currency === userStore.user?.defaultCurrency) return;
  saving.value = true;
  try {
    await userStore.updateDefaultCurrency(currency);
    success(t('profile.currency.toasts.savedTitle'), t('profile.currency.toasts.savedBody'));
  } catch {
    selected.value = userStore.user?.defaultCurrency ?? Currency.EUR;
    error(t('profile.currency.toasts.errorTitle'), t('profile.currency.toasts.errorBody'));
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.currency.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.currency.description') }}</p>
    <CurrencySelect
      class="mt-4"
      :model-value="selected"
      :disabled="saving"
      @update:model-value="onChange"/>
  </div>
</template>
