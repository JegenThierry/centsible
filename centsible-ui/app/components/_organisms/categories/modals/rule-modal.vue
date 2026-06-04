<script lang="ts" setup>
import adze from 'adze'
import {useCategorizationRulesStore} from "~/stores/categorizationRulesStore";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {type CategorizationRule, type CategorizationRuleForm, MATCH_TYPES, type MatchType} from "~/models/categorization/rule";
import type {Category} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import {useValidator} from "~/composables/use-validator";

const props = defineProps<{
  rule?: CategorizationRule | null;
  presetPattern?: string;
  presetCategoryId?: number;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const store = useCategorizationRulesStore();
const categoriesStore = useCategoriesStore();
const {t} = useI18n();

const patternInput = ref<InstanceType<typeof BaseInput>>();
const categoryInput = ref();
const loading = ref(false);

const matchType = ref<MatchType>('CONTAINS');
const pattern = ref('');
const category = ref<Category>();
const priority = ref(0);

const isEdit = computed(() => !!props.rule);

const matchOptions = computed(() => MATCH_TYPES.map((value) => ({value, label: t(`categories.rules.match.${value}`)})));

function reset() {
  if (props.rule) {
    matchType.value = props.rule.matchType;
    pattern.value = props.rule.pattern;
    category.value = props.rule.category;
    priority.value = props.rule.priority;
  } else {
    matchType.value = 'CONTAINS';
    pattern.value = props.presetPattern ?? '';
    priority.value = 0;
    category.value = props.presetCategoryId != null
      ? categoriesStore.categories.find(c => c.id === props.presetCategoryId)
      : undefined;
  }
}

watch(isOpen, async (open) => {
  if (open) {
    if (categoriesStore.categories.length === 0) await categoriesStore.updateCategories();
    reset();
  }
});

async function handleSave() {
  if (!useValidator().validateInputs([patternInput, categoryInput])) return;
  if (!category.value?.id) return;

  const form: CategorizationRuleForm = {
    matchType: matchType.value,
    pattern: pattern.value,
    categoryId: category.value.id,
    priority: Number(priority.value) || 0,
  };

  loading.value = true;
  try {
    if (props.rule) await store.updateRule(props.rule.id, form);
    else await store.createRule(form);
    isOpen.value = false;
  } catch (error) {
    adze.ns('categorization').error('Failed to save rule', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal v-model:open="isOpen"
          :description="t('categories.rules.modalDescription')"
          :title="isEdit ? t('categories.rules.editTitle') : t('categories.rules.createTitle')">
    <template #body>
      <div class="space-y-4">
        <BaseInput ref="patternInput"
                   v-model="pattern"
                   :max-length="255"
                   :label="t('categories.rules.patternLabel')"
                   :placeholder="t('categories.rules.patternPlaceholder')"
                   required
                   type="text"/>

        <div>
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">
            {{ t('categories.rules.matchLabel') }}
          </label>
          <AppSelect v-model="matchType" :items="matchOptions" class="w-full mt-1"/>
        </div>

        <CategorySelect ref="categoryInput"
                        v-model="category"
                        :options="categoriesStore.categories"
                        :label="t('categories.rules.categoryLabel')"
                        required/>

        <BaseInput v-model="priority"
                   :label="t('categories.rules.priorityLabel')"
                   :description="t('categories.rules.priorityDescription')"
                   :min="0"
                   type="number"/>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loading"
                          :submit-label="isEdit ? t('categories.rules.editSubmit') : t('categories.rules.createSubmit')"
                          @cancel="isOpen = false"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
