<script lang="ts" setup>
import adze from 'adze'
import type {LoanForm as LoanFormModel} from "~/models/loan/loan";
import LoanForm from "~/components/_molecules/loans/loan-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useLoansStore} from "~/stores/loansStore";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  contactId?: string;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const loansStore = useLoansStore();
const {t} = useI18n();

const form = ref<LoanFormModel>(makeBlankForm());
const formRef = ref<InstanceType<typeof LoanForm>>();
const loading = ref(false);

function makeBlankForm(): LoanFormModel {
  return {
    contactId: props.contactId,
    newContactFirstName: undefined,
    newContactLastName: undefined,
    accountId: undefined,
    affectBalance: true,
    lentAmount: 0,
    owedAmount: 0,
    description: '',
    transactionDate: todayIsoDate(),
    dueDate: undefined,
    notes: undefined,
  };
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlankForm();
});

async function handleSave() {
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    await loansStore.createLoan(form.value);
    emit('created');
    isOpen.value = false;
  } catch (error) {
    adze.ns('loans').error('Create loan failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('contacts.loans.create.description')"
          :title="t('contacts.loans.create.title')">
    <template #body>
      <LoanForm ref="formRef" v-model="form" :lock-contact="!!contactId"/>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('contacts.loans.create.submit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
