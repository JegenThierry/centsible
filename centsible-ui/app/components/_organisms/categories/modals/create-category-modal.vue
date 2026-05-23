<script lang="ts" setup>
import adze from 'adze'
import {useCategoriesStore} from "~/stores/categoriesStore";
import {type CategoryForm, CategoryType} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import IconInput from "~/components/_molecules/inputs/icon-input.vue";
import ColorSelect from "~/components/_atoms/inputs/color-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useValidator} from "~/composables/use-validator";

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();
const {t} = useI18n();

const form = ref<CategoryForm>({
  name: '',
  icon: 'i-lucide-tag',
  color: '#3b82f6',
  type: CategoryType.EXPENSE,
});

const nameInput = ref<InstanceType<typeof BaseInput>>();
const iconInput = ref<InstanceType<typeof IconInput>>();

const loading = ref(false);

const typeOptions = computed(() => [
  {label: t('categories.type.expense'), value: CategoryType.EXPENSE},
  {label: t('categories.type.income'), value: CategoryType.INCOME},
]);

function resetForm() {
  form.value = {
    name: '',
    icon: 'i-lucide-tag',
    color: '#3b82f6',
    type: CategoryType.EXPENSE,
  };
}

watch(isOpen, (newValue) => {
  if (newValue) {
    resetForm();
  }
});

async function handleSave() {
  const inputs = [nameInput, iconInput];
  if (!useValidator().validateInputs(inputs)) {
    return;
  }

  loading.value = true;
  try {
    await categoriesStore.createCategory(form.value);
    isOpen.value = false;
  } catch (error) {
    adze.ns('categories').error('Failed to create category', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('categories.create.description')"
          :title="t('categories.create.title')">
    <template #body>
      <div class="space-y-4">
        <URadioGroup v-model="form.type"
                     :items="typeOptions"
                     :legend="t('categories.type.legend')"
                     orientation="horizontal"/>

        <BaseInput ref="nameInput"
                   v-model="form.name"
                   :max-length="50"
                   :label="t('categories.form.nameLabel')"
                   :placeholder="t('categories.form.namePlaceholder')"
                   required
                   type="text"/>

        <IconInput ref="iconInput"
                   v-model="form.icon"
                   required/>

        <ColorSelect v-model="form.color"
                     :label="t('categories.form.colorLabel')"
                     required/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="t('categories.create.submit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
