<script lang="ts" setup>
import adze from 'adze'
import type {Loan, RepaymentForm as RepaymentFormModel} from "~/models/loan/loan";
import RepaymentForm from "~/components/_molecules/loans/repayment-form.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useLoansStore} from "~/stores/loansStore";
import {todayIsoDate} from "~/utils/date";

const props = defineProps<{
  loan: Loan | undefined;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const emit = defineEmits<{
  (e: 'recorded'): void;
}>();

const loansStore = useLoansStore();
const {t} = useI18n();

const form = ref<RepaymentFormModel>(makeBlankForm());
const formRef = ref<InstanceType<typeof RepaymentForm>>();
const loading = ref(false);

const description = computed(() => {
  if (!props.loan) return '';
  return t('contacts.loans.repayment.descriptionWithContact', {name: props.loan.contact.name});
});

function makeBlankForm(): RepaymentFormModel {
  return {
    accountId: props.loan?.accountId,
    affectBalance: props.loan?.affectsBalance ?? true,
    amount: Number(props.loan?.outstanding ?? 0),
    description: '',
    repaidAt: todayIsoDate(),
  };
}

watch(isOpen, (open) => {
  if (open) form.value = makeBlankForm();
});

async function handleSave() {
  if (!props.loan?.id || !props.loan.contact.id) return;
  if (!formRef.value?.validate()) return;

  loading.value = true;
  try {
    await loansStore.recordRepayment(props.loan.id, props.loan.contact.id, form.value);
    emit('recorded');
    isOpen.value = false;
  } catch (error) {
    adze.ns('loans').error('Record repayment failed', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="description"
          :title="t('contacts.loans.repayment.title')">
    <template #body>
      <RepaymentForm v-if="loan" ref="formRef" v-model="form" :max-amount="Number(loan.outstanding)" :currency="loan.currency"/>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('contacts.loans.repayment.submit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
