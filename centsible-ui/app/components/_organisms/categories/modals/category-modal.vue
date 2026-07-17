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
import FormModal from "~/components/_molecules/modals/form-modal.vue";
import {categorySchema} from "~/utils/form-schemas";

const props = defineProps<{
  /** Present → edit that category; absent → create a new one. */
  category?: Category;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const categoriesStore = useCategoriesStore();
const {t} = useI18n();

const form = ref<CategoryForm>(blankForm());
const loading = ref(false);

const schema = categorySchema(t);
type Schema = z.output<typeof schema>;

const isEdit = computed(() => !!props.category);

const typeOptions = computed(() => [
  {label: t('categories.type.expense'), value: CategoryType.EXPENSE},
  {label: t('categories.type.income'), value: CategoryType.INCOME},
]);

function blankForm(): CategoryForm {
  return {name: '', icon: 'i-lucide-tag', color: '#3b82f6', type: CategoryType.EXPENSE};
}

function syncForm() {
  const c = props.category;
  form.value = c
    ? {name: c.name, icon: c.icon, color: c.color, type: c.type}
    : blankForm();
}

// Snapshot for FormModal's dirty guard; defined in script so `form.value` reads the ref, not the
// template-unwrapped value.
function snapshot() {
  return form.value;
}

watch(() => props.category, () => {
  if (isOpen.value) syncForm();
});

async function handleSave(_event: FormSubmitEvent<Schema>) {
  loading.value = true;
  try {
    if (props.category) {
      if (!props.category.id) return;
      await categoriesStore.updateCategory(props.category.id, form.value);
    } else {
      await categoriesStore.createCategory(form.value);
    }
    isOpen.value = false;
  } catch (error) {
    adze.ns('categories').error(`Failed to ${isEdit.value ? 'update' : 'create'} category`, error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <FormModal v-model="isOpen"
             :description="t(isEdit ? 'categories.edit.description' : 'categories.create.description')"
             :title="t(isEdit ? 'categories.edit.title' : 'categories.create.title')"
             :schema="schema"
             :state="form"
             :loading="loading"
             :get-snapshot="snapshot"
             :on-reset-on-open="syncForm"
             :submit-label="t(isEdit ? 'categories.edit.submit' : 'categories.create.submit')"
             @submit="handleSave">
    <template #fields>
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
    </template>
  </FormModal>
</template>
