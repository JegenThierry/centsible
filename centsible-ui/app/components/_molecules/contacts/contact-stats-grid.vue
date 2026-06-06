<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import type {Currency} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const props = defineProps<{
  contact: Contact;
  currency: Currency;
}>();

const {t} = useI18n();

const stats = computed(() => [
  {labelKey: 'contacts.detail.stats.totalLent', value: Number(props.contact.totalLent)},
  {labelKey: 'contacts.detail.stats.totalOwed', value: Number(props.contact.totalOwed)},
  {labelKey: 'contacts.detail.stats.totalRepaid', value: Number(props.contact.totalRepaid), valueClass: 'text-success'},
  {labelKey: 'contacts.detail.stats.outstanding', value: Number(props.contact.outstanding), valueClass: 'text-warning'},
]);
</script>

<template>
  <div class="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
    <UCard v-for="stat in stats" :key="stat.labelKey">
      <p class="text-xs text-muted">{{ t(stat.labelKey) }}</p>
      <p :class="['text-lg font-bold', stat.valueClass]">
        <BalanceNumberFormat :balance="stat.value" :currency="currency"/>
      </p>
    </UCard>
  </div>
</template>
