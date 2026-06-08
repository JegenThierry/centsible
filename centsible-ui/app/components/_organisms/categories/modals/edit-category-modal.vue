<script lang="ts" setup>
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {useCategoriesStore} from "~/stores/categoriesStore";
import {type Category, type CategoryForm, CategoryType} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import IconInput from "~/components/_molecules/inputs/icon-input.vue";
import ColorSelect from "~/components/_atoms/inputs/color-select.vue";
import AppRadioGroup from "~/components/_atoms/ui/app-radio-group.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {categorySchema} from "~/utils/form-schemas";

const props = defineProps<{
  category?: Category;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();
const {t} = useI18n();

const form = ref<CategoryForm>({
  name: '',
  icon: 'i-lucide-tag',
  color: '#3b82f6',
  type: CategoryType.EXPENSE,
});

const loading = ref(false);
const formId = useId();

const schema = categorySchema(t);
type Schema = z.output<typeof schema>;

const typeOptions = computed(() => [
  {label: t('categories.type.expense'), value: CategoryType.EXPENSE},
  {label: t('categories.type.income'), value: CategoryType.INCOME},
]);

watch(() => props.category, (newCategory) => {
  if (newCategory) {
    form.value = {
      name: newCategory.name,
      icon: newCategory.icon,
      color: newCategory.color,
      type: newCategory.type,
    };
  }
}, {immediate: true});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!props.category?.id) return;

  loading.value = true;
  try {
    await categoriesStore.updateCategory(props.category.id, form.value);
    isOpen.value = false;
  } catch (error) {
    adze.ns('categories').error('Failed to update category', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('categories.edit.description')"
          :title="t('categories.edit.title')">
    <template #body>
      <UForm :id="formId" :schema="schema" :state="form" class="space-y-4" @submit="handleSave">
        <AppRadioGroup v-model="form.type"
                     :items="typeOptions"
                     :legend="t('categories.type.legend')"
                     orientation="horizontal"/>

        <BaseInput name="name"
                   v-model="form.name"
                   :max-length="50"
                   :label="t('categories.form.nameLabel')"
                   :placeholder="t('categories.form.namePlaceholder')"
                   required
                   type="text"/>

        <IconInput name="icon"
                   v-model="form.icon"
                   required/>

        <ColorSelect v-model="form.color"
                     :label="t('categories.form.colorLabel')"
                     required/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="t('categories.edit.submit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
