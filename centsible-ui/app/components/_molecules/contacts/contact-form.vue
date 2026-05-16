<script lang="ts" setup>
import type {ContactForm} from "~/models/contact/contact";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import {useValidator} from "~/composables/use-validator";

const props = defineProps<{
  modelValue: ContactForm;
}>();

const emit = defineEmits(['update:modelValue']);

const {t} = useI18n();

const firstNameInput = ref<InstanceType<typeof BaseInput>>();
const lastNameInput = ref<InstanceType<typeof BaseInput>>();

const form = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
});

defineExpose({
  validate: () => useValidator().validateInputs([firstNameInput, lastNameInput]),
});
</script>

<template>
  <div class="space-y-4">
    <BaseInput ref="firstNameInput"
               v-model="form.firstName"
               :max-length="100"
               :label="t('contacts.form.firstNameLabel')"
               :placeholder="t('contacts.form.firstNamePlaceholder')"
               required
               type="text"/>

    <BaseInput ref="lastNameInput"
               v-model="form.lastName"
               :max-length="100"
               :label="t('contacts.form.lastNameLabel')"
               :placeholder="t('contacts.form.lastNamePlaceholder')"
               type="text"/>
  </div>
</template>
