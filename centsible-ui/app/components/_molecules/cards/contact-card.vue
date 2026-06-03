<script lang="ts" setup>
import type {Contact} from "~/models/contact/contact";
import ContactAvatar from "~/components/_atoms/contacts/contact-avatar.vue";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import EditDeleteActions from "~/components/_molecules/buttons/edit-delete-actions.vue";
import {useActiveCurrency} from "~/composables/use-active-currency";

defineProps<{
  contact: Contact;
}>();

const emit = defineEmits<{
  open: [contact: Contact];
  edit: [contact: Contact];
  delete: [contact: Contact];
}>();

const {t} = useI18n();
const currency = useActiveCurrency();
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
          <p class="text-xs text-muted">
            {{ contact.openLoanCount === 1 ? t('common.category.loanCount', {count: contact.openLoanCount}) : t('common.category.loansCount', {count: contact.openLoanCount}) }}
          </p>
        </div>
      </div>
      <div class="flex items-center gap-3">
        <div class="text-right">
          <p class="text-xs text-muted">{{ t('common.category.outstanding') }}</p>
          <p :class="Number(contact.outstanding) > 0 ? 'text-warning font-semibold' : 'text-muted'">
            <BalanceNumberFormat :balance="Number(contact.outstanding)" :currency="currency"/>
          </p>
        </div>
        <EditDeleteActions hide-on-hover
                           @edit="emit('edit', contact)"
                           @delete="emit('delete', contact)"/>
      </div>
    </div>
  </UCard>
</template>
