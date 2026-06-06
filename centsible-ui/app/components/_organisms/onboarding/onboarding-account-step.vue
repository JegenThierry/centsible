<script lang="ts" setup>
import type {SelectItem} from "@nuxt/ui/components/Select.vue";
import {Currency} from "~/models/budget-account/currency";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

interface Props {
  loading?: boolean;
  activeIcon?: string;
  currencyOptions: SelectItem[];
}

defineProps<Props>();
const emit = defineEmits<{
  (e: 'skip'): void;
  (e: 'submit'): void;
}>();

const {t} = useI18n();

const name = defineModel<string>('name', {required: true});
const initialBalance = defineModel<number>('initialBalance', {required: true});
const currency = defineModel<Currency>('currency', {required: true});
</script>

<template>
  <div class="space-y-4">
    <h2 class="text-xl font-semibold">{{ t('onboarding.account.title') }}</h2>
    <p class="text-sm text-muted">{{ t('onboarding.account.description') }}</p>

    <div class="space-y-3">
      <div class="grid grid-cols-2 gap-3">
        <AppInput v-model="name"
                  :placeholder="t('onboarding.account.namePlaceholder')"
                  class="w-full"
                  size="lg"/>
        <AppInput v-model.number="initialBalance"
                  :placeholder="t('onboarding.account.balancePlaceholder')"
                  class="w-full"
                  size="lg"
                  type="number"/>
      </div>
      <AppSelect v-model="currency"
                 :icon="activeIcon"
                 :items="currencyOptions"
                 class="w-full"
                 size="lg"/>
    </div>

    <div class="flex justify-between pt-2">
      <AppButton color="neutral" variant="ghost" @click="emit('skip')">
        {{ t('onboarding.skip') }}
      </AppButton>
      <AppButton :loading="loading" color="primary" trailing-icon="i-lucide-arrow-right" @click="emit('submit')">
        {{ t('onboarding.continue') }}
      </AppButton>
    </div>
  </div>
</template>
