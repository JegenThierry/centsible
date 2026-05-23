<script lang="ts" setup>
import adze from 'adze'
import {useCategoriesStore} from "~/stores/categoriesStore";
import {type Category, type CategoryForm, CategoryType} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import IconInput from "~/components/_molecules/inputs/icon-input.vue";
import ColorSelect from "~/components/_atoms/inputs/color-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useValidator} from "~/composables/use-validator";

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

const nameInput = ref<InstanceType<typeof BaseInput>>();
const iconInput = ref<InstanceType<typeof IconInput>>();

const loading = ref(false);

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

async function handleSave() {
  if (!props.category?.id) return;

  const inputs = [nameInput, iconInput];
  if (!useValidator().validateInputs(inputs)) {
    return;
  }

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
                          :submit-label="t('categories.edit.submit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
