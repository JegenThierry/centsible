<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import ContactAvatar from "~/components/_atoms/contacts/contact-avatar.vue";
import BalanceNumberFormat from "~/components/_molecules/labels/balance-number-format.vue";
import {Currency} from "~/models/budget-account/currency";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

defineProps<{
  contact: Contact;
}>();

const emit = defineEmits<{
  open: [contact: Contact];
  edit: [contact: Contact];
  delete: [contact: Contact];
}>();

const budgetAccountsStore = useBudgetAccountsStore();
const currency = computed(() => budgetAccountsStore.activeAccount?.currency ?? Currency.EUR);
</script>

<template>
  <UCard
    class="group transition-all cursor-pointer hover:border-primary-300 dark:hover:border-primary-700"
    @click="emit('open', contact)"
  >
    <div class="flex items-center justify-between">
      <div class="flex items-center gap-3">
        <ContactAvatar :alt="contact.name" :src="contact.picture" size="lg"/>
        <div>
          <p class="font-semibold">{{ contact.name }}</p>
          <p class="text-xs text-neutral-500">
            {{ contact.openLoanCount }} {{ contact.openLoanCount === 1 ? 'loan' : 'loans' }}
          </p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <div class="text-right">
          <p class="text-xs text-neutral-500">Outstanding</p>
          <p :class="Number(contact.outstanding) > 0 ? 'text-amber-600 dark:text-amber-400 font-semibold' : 'text-neutral-500'">
            <BalanceNumberFormat :balance="Number(contact.outstanding)" :currency="currency" format="de-De"/>
          </p>
        </div>
        <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
          <UButton
            color="neutral"
            icon="i-lucide-pencil"
            size="sm"
            variant="ghost"
            @click.stop="emit('edit', contact)"
          />
          <UButton
            color="error"
            icon="i-lucide-trash"
            size="sm"
            variant="ghost"
            @click.stop="emit('delete', contact)"
          />
        </div>
      </div>
    </div>
  </UCard>
</template>
