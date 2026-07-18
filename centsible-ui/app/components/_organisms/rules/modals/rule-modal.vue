<script lang="ts" setup>
import adze from 'adze'
import {useRulesStore} from "~/stores/rulesStore";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {useTagsStore} from "~/stores/tagsStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {
  DIRECTIONS,
  OPERATORS_BY_FIELD,
  RULE_ACTION_TYPES,
  RULE_FIELDS,
  type Rule,
  type RuleActionForm,
  type RuleActionType,
  type RuleConditionForm,
  type RuleField,
  type RuleForm,
  type RuleOperator,
} from "~/models/rule/rule";
import AppSelect from "~/components/_atoms/ui/app-select.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import {useModalDirtyGuard} from "~/composables/use-unsaved-changes-guard";
import {useRulePreview} from "~/composables/use-rule-preview";
import {useDefaultCurrency} from "~/composables/use-default-currency";
import TransactionAmount from "~/components/_molecules/transactions/transaction-amount.vue";

const props = defineProps<{
  rule?: Rule | null;
  presetPattern?: string;
  presetCategoryId?: number;
}>();

const isOpen = defineModel<boolean>('open', {required: true});

const store = useRulesStore();
const categoriesStore = useCategoriesStore();
const tagsStore = useTagsStore();
const budgetAccountsStore = useBudgetAccountsStore();
const {t} = useI18n();
const defaultCurrency = useDefaultCurrency();

const loading = ref(false);
const isEdit = computed(() => !!props.rule);

interface ConditionRow {
  field: RuleField;
  operator: RuleOperator;
  value: string;
}

interface ActionRow {
  type: RuleActionType;
  categoryId?: number;
  tagId?: number;
}

const state = reactive<{
  name: string;
  matchAll: boolean;
  enabled: boolean;
  priority: number;
  conditions: ConditionRow[];
  actions: ActionRow[];
}>({
  name: '',
  matchAll: true,
  enabled: true,
  priority: 0,
  conditions: [],
  actions: [],
});

const fieldOptions = computed(() => RULE_FIELDS.map(f => ({value: f, label: t(`rules.field.${f}`)})));
const actionTypeOptions = computed(() => RULE_ACTION_TYPES.map(a => ({value: a, label: t(`rules.actionType.${a}`)})));
const directionOptions = computed(() => DIRECTIONS.map(d => ({value: d, label: t(`rules.direction.${d}`)})));
const matchModeOptions = computed(() => [
  {value: true, label: t('rules.matchAll')},
  {value: false, label: t('rules.matchAny')},
]);
const accountOptions = computed(() => budgetAccountsStore.availableAccounts.map(a => ({value: a.id, label: a.name})));
const categoryOptions = computed(() => categoriesStore.categories.map(c => ({value: c.id, label: c.name})));
const tagOptions = computed(() => tagsStore.tags.map(tag => ({value: tag.id, label: tag.name})));

function operatorOptions(field: RuleField) {
  return OPERATORS_BY_FIELD[field].map(o => ({value: o, label: t(`rules.operator.${o}`)}));
}

function defaultCondition(): ConditionRow {
  return {field: 'DESCRIPTION', operator: 'CONTAINS', value: ''};
}

function defaultAction(): ActionRow {
  return {type: 'SET_CATEGORY', categoryId: undefined, tagId: undefined};
}

function changeField(row: ConditionRow, field: RuleField) {
  row.field = field;
  row.operator = OPERATORS_BY_FIELD[field][0] ?? 'IS';
  row.value = '';
}

function addCondition() {
  state.conditions.push(defaultCondition());
}

function removeCondition(index: number) {
  state.conditions.splice(index, 1);
}

function addAction() {
  state.actions.push(defaultAction());
}

function removeAction(index: number) {
  state.actions.splice(index, 1);
}

function reset() {
  if (props.rule) {
    state.name = props.rule.name;
    state.matchAll = props.rule.matchAll;
    state.enabled = props.rule.enabled;
    state.priority = props.rule.priority;
    state.conditions = props.rule.conditions.map(c => ({field: c.field, operator: c.operator, value: c.value}));
    state.actions = props.rule.actions.map(a => ({
      type: a.type,
      categoryId: a.category?.id ?? undefined,
      tagId: a.tag?.id ?? undefined,
    }));
  } else {
    state.name = props.presetPattern ?? '';
    state.matchAll = true;
    state.enabled = true;
    state.priority = 0;
    state.conditions = [
      props.presetPattern
        ? {field: 'DESCRIPTION', operator: 'CONTAINS', value: props.presetPattern}
        : defaultCondition(),
    ];
    state.actions = [
      props.presetCategoryId != null
        ? {type: 'SET_CATEGORY', categoryId: props.presetCategoryId, tagId: undefined}
        : defaultAction(),
    ];
  }
}

const canPreview = computed(() => state.conditions.some(c => String(c.value).trim().length > 0));

const canSubmit = computed(() =>
  state.name.trim().length > 0
  && canPreview.value
  && state.actions.some(a => (a.type === 'SET_CATEGORY' && a.categoryId != null) || (a.type === 'ADD_TAG' && a.tagId != null)),
);

function toRuleForm(): RuleForm {
  const conditions: RuleConditionForm[] = state.conditions
    .filter(c => String(c.value).trim().length > 0)
    .map(c => ({field: c.field, operator: c.operator, value: String(c.value).trim()}));

  const actions: RuleActionForm[] = state.actions
    .filter(a => (a.type === 'SET_CATEGORY' && a.categoryId != null) || (a.type === 'ADD_TAG' && a.tagId != null))
    .map(a => a.type === 'SET_CATEGORY'
      ? {type: a.type, categoryId: a.categoryId}
      : {type: a.type, tagId: a.tagId});

  return {
    name: state.name.trim(),
    matchAll: state.matchAll,
    enabled: state.enabled,
    priority: Number(state.priority) || 0,
    conditions,
    actions,
  };
}

const previewForm = computed<RuleForm>(() => toRuleForm());

const {matchedCount, sample, loading: previewLoading, failed: previewFailed} = useRulePreview({
  form: previewForm,
  enabled: canPreview,
});

const {requestClose} = useModalDirtyGuard({
  isOpen,
  loading,
  getSnapshot: () => ({...state}),
  onResetOnOpen: reset,
});

watch(isOpen, async (open) => {
  if (!open) return;
  if (categoriesStore.categories.length === 0) await categoriesStore.updateCategories();
  if (tagsStore.tags.length === 0) await tagsStore.fetchAll();
  if (budgetAccountsStore.availableAccounts.length === 0) await budgetAccountsStore.updateAvailableAccounts();
});

async function handleSave() {
  const form = toRuleForm();
  if (!form.name || form.conditions.length === 0 || form.actions.length === 0) return;

  loading.value = true;
  try {
    if (props.rule) await store.updateRule(props.rule.id, form);
    else await store.createRule(form);
    isOpen.value = false;
  } catch (error) {
    adze.ns('rules').error('Failed to save rule', error);
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :description="t('rules.modalDescription')"
          :title="isEdit ? t('rules.editTitle') : t('rules.createTitle')"
          :ui="{content: 'sm:max-w-2xl'}"
          @update:open="requestClose">
    <template #body>
      <div class="space-y-5">
        <UFormField :label="t('rules.nameLabel')">
          <AppInput v-model="state.name" :placeholder="t('rules.namePlaceholder')" class="w-full"/>
        </UFormField>

        <UFormField :label="t('rules.matchModeLabel')">
          <AppSelect v-model="state.matchAll" :items="matchModeOptions" class="w-full"/>
        </UFormField>

        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium text-default">{{ t('rules.conditionsLabel') }}</span>
            <AppButton color="neutral" icon="i-lucide-plus" size="xs" variant="ghost" @click="addCondition">
              {{ t('rules.addCondition') }}
            </AppButton>
          </div>
          <p v-if="state.conditions.length === 0" class="text-xs text-muted">{{ t('rules.noConditions') }}</p>
          <div v-for="(row, i) in state.conditions" :key="`c-${i}`" class="flex flex-wrap items-center gap-2">
            <AppSelect :items="fieldOptions"
                       :model-value="row.field"
                       class="w-36"
                       @update:model-value="(v: RuleField) => changeField(row, v)"/>
            <AppSelect v-if="row.field === 'DESCRIPTION' || row.field === 'AMOUNT'"
                       v-model="row.operator"
                       :items="operatorOptions(row.field)"
                       class="w-32"/>
            <AppInput v-if="row.field === 'DESCRIPTION'"
                    v-model="row.value"
                    :placeholder="t('rules.valuePlaceholder')"
                    class="flex-1 min-w-[8rem]"/>
            <AppInput v-else-if="row.field === 'AMOUNT'"
                    v-model="row.value"
                    class="flex-1 min-w-[8rem]"
                    type="number"/>
            <AppSelect v-else-if="row.field === 'DIRECTION'"
                       v-model="row.value"
                       :items="directionOptions"
                       :placeholder="t('rules.selectDirection')"
                       class="flex-1 min-w-[8rem]"/>
            <AppSelect v-else
                       v-model="row.value"
                       :items="accountOptions"
                       :placeholder="t('rules.selectAccount')"
                       class="flex-1 min-w-[8rem]"/>
            <AppButton :aria-label="t('rules.removeAria')"
                       color="neutral" icon="i-lucide-x" size="xs" variant="ghost"
                       @click="removeCondition(i)"/>
          </div>
        </div>

        <div class="space-y-2">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium text-default">{{ t('rules.actionsLabel') }}</span>
            <AppButton color="neutral" icon="i-lucide-plus" size="xs" variant="ghost" @click="addAction">
              {{ t('rules.addAction') }}
            </AppButton>
          </div>
          <p v-if="state.actions.length === 0" class="text-xs text-muted">{{ t('rules.noActions') }}</p>
          <div v-for="(row, i) in state.actions" :key="`a-${i}`" class="flex flex-wrap items-center gap-2">
            <AppSelect v-model="row.type" :items="actionTypeOptions" class="w-40"/>
            <AppSelect v-if="row.type === 'SET_CATEGORY'"
                       v-model="row.categoryId"
                       :items="categoryOptions"
                       :placeholder="t('rules.selectCategory')"
                       class="flex-1 min-w-[8rem]"/>
            <AppSelect v-else
                       v-model="row.tagId"
                       :items="tagOptions"
                       :placeholder="t('rules.selectTag')"
                       class="flex-1 min-w-[8rem]"/>
            <AppButton :aria-label="t('rules.removeAria')"
                       color="neutral" icon="i-lucide-x" size="xs" variant="ghost"
                       @click="removeAction(i)"/>
          </div>
        </div>

        <div v-if="canPreview" class="space-y-2 rounded-lg border border-default p-3">
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium text-default">{{ t('rules.preview.title') }}</span>
            <UIcon v-if="previewLoading" class="w-4 h-4 shrink-0 animate-spin text-muted" name="i-lucide-loader-circle"/>
          </div>
          <p v-if="previewFailed" class="text-xs text-error">{{ t('rules.preview.failed') }}</p>
          <template v-else-if="matchedCount !== null">
            <p v-if="matchedCount === 0" class="text-xs text-muted">{{ t('rules.preview.empty') }}</p>
            <template v-else>
              <p class="text-sm text-muted">{{ t('rules.preview.matchCount', {count: matchedCount}) }}</p>
              <ul class="space-y-1">
                <li v-for="(match, i) in sample"
                    :key="`p-${i}`"
                    class="flex items-center justify-between gap-3">
                  <span class="min-w-0 flex-1 truncate text-sm text-default">{{ match.description }}</span>
                  <TransactionAmount :amount="match.amount" :currency="defaultCurrency" :type="match.type"/>
                </li>
              </ul>
            </template>
          </template>
        </div>

        <div class="flex flex-wrap items-end gap-4">
          <UFormField :description="t('rules.priorityDescription')" :label="t('rules.priorityLabel')">
            <AppInput v-model="state.priority" :max="1000" :min="0" class="w-28" type="number"/>
          </UFormField>
          <UFormField :label="t('rules.enabledLabel')">
            <USwitch v-model="state.enabled"/>
          </UFormField>
        </div>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :disabled="!canSubmit"
                          :loading="loading"
                          :submit-label="isEdit ? t('rules.editSubmit') : t('rules.createSubmit')"
                          @cancel="requestClose(false)"
                          @submit="handleSave"/>
    </template>
  </UModal>
</template>
