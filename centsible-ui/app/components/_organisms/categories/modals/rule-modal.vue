<script lang="ts" setup>
import adze from 'adze'
import {z} from 'zod'
import type {FormSubmitEvent} from '@nuxt/ui'
import {useCategorizationRulesStore} from "~/stores/categorizationRulesStore";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {type CategorizationRule, type CategorizationRuleForm, MATCH_TYPES, type MatchType} from "~/models/categorization/rule";
import type {Category} from "~/models/category/category";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import CategorySelect from "~/components/_atoms/inputs/category-select.vue";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";

const props = defineProps<{
  rule?: CategorizationRule | null;
  presetPattern?: string;
  presetCategoryId?: number;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const store = useCategorizationRulesStore();
const categoriesStore = useCategoriesStore();
const {t} = useI18n();

const loading = ref(false);
const formId = useId();

const state = reactive<{
  matchType: MatchType;
  pattern: string;
  category: Category | undefined;
  priority: number;
}>({
  matchType: 'CONTAINS',
  pattern: '',
  category: undefined,
  priority: 0,
});

const isEdit = computed(() => !!props.rule);

const matchOptions = computed(() => MATCH_TYPES.map((value) => ({value, label: t(`categories.rules.match.${value}`)})));

// Short, plain-language explanation of the selected match type so users know how it behaves.
const matchHelp = computed(() => t(`categories.rules.matchHelp.${state.matchType}`));

const patternLabel = t('categories.rules.patternLabel');
const priorityLabel = t('categories.rules.priorityLabel');

const schema = z.object({
  pattern: z.string().trim()
    .min(1, t('common.validation.required', {field: patternLabel}))
    .max(255, t('common.validation.maxLength', {field: patternLabel, max: 255})),
  category: z.any().refine((v) => !!v, t('common.validation.required', {field: t('categories.rules.categoryLabel')})),
  priority: z.coerce.number({message: t('common.validation.number', {field: priorityLabel})})
    .min(0, t('common.validation.min', {field: priorityLabel, min: 0}))
    .max(1000, t('common.validation.max', {field: priorityLabel, max: 1000})),
});
type Schema = z.output<typeof schema>;

function reset() {
  if (props.rule) {
    state.matchType = props.rule.matchType;
    state.pattern = props.rule.pattern;
    state.category = props.rule.category;
    state.priority = props.rule.priority;
  } else {
    state.matchType = 'CONTAINS';
    state.pattern = props.presetPattern ?? '';
    state.priority = 0;
    state.category = props.presetCategoryId != null
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

async function handleSave(_event: FormSubmitEvent<Schema>) {
  if (!state.category?.id) return;

  const form: CategorizationRuleForm = {
    matchType: state.matchType,
    pattern: state.pattern,
    categoryId: state.category.id,
    priority: Number(state.priority) || 0,
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
      <UForm :id="formId" :schema="schema" :state="state" class="space-y-4" @submit="handleSave">
        <BaseInput name="pattern"
                   v-model="state.pattern"
                   :max-length="255"
                   :label="t('categories.rules.patternLabel')"
                   :placeholder="t('categories.rules.patternPlaceholder')"
                   required
                   type="text"/>

        <div>
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">
            {{ t('categories.rules.matchLabel') }}
          </label>
          <AppSelect v-model="state.matchType" :items="matchOptions" class="w-full mt-1"/>
          <p class="text-xs text-muted mt-1">{{ matchHelp }}</p>
        </div>

        <CategorySelect name="category"
                        v-model="state.category"
                        :options="categoriesStore.categories"
                        :label="t('categories.rules.categoryLabel')"
                        required/>

        <BaseInput name="priority"
                   v-model="state.priority"
                   :label="t('categories.rules.priorityLabel')"
                   :description="t('categories.rules.priorityDescription')"
                   :min="0"
                   :max="1000"
                   type="number"/>
      </UForm>
    </template>

    <template #footer>
      <ModalFooterActions :form="formId"
                          :loading="loading"
                          :submit-label="isEdit ? t('categories.rules.editSubmit') : t('categories.rules.createSubmit')"
                          @cancel="isOpen = false"/>
    </template>
  </UModal>
</template>
