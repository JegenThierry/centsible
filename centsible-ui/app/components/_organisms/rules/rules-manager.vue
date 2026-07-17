<script lang="ts" setup>
import {useRulesStore} from "~/stores/rulesStore";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useToasts} from "~/services/toasts/toast-service";
import type {Rule, RuleCondition} from "~/models/rule/rule";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";
import PageHeader from "~/components/_molecules/page/page-header.vue";

const RuleModal = defineAsyncComponent(() => import("~/components/_organisms/rules/modals/rule-modal.vue"));
const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const store = useRulesStore();
const budgetAccountsStore = useBudgetAccountsStore();
const toasts = useToasts();
const {t} = useI18n();

const isRuleModalOpen = ref(false);
const isDeleteOpen = ref(false);
const selected = ref<Rule | null>(null);

function openCreate() {
  selected.value = null;
  isRuleModalOpen.value = true;
}

function openEdit(rule: Rule) {
  selected.value = rule;
  isRuleModalOpen.value = true;
}

function openDelete(rule: Rule) {
  selected.value = rule;
  isDeleteOpen.value = true;
}

async function apply(rule: Rule) {
  try {
    const count = await store.applyRule(rule.id);
    toasts.success(t('rules.appliedTitle'), t('rules.appliedBody', {count}));
  } catch {
  }
}

function accountName(id: string): string {
  return budgetAccountsStore.availableAccounts.find(a => a.id === id)?.name ?? id;
}

function conditionText(c: RuleCondition): string {
  const field = t(`rules.field.${c.field}`);
  const operator = t(`rules.operator.${c.operator}`);
  const value = c.field === 'DIRECTION'
    ? t(`rules.direction.${c.value}`)
    : c.field === 'ACCOUNT'
      ? accountName(c.value)
      : c.value;
  return `${field} ${operator} ${value}`;
}

onMounted(() => {
  store.updateRules();
  if (budgetAccountsStore.availableAccounts.length === 0) budgetAccountsStore.updateAvailableAccounts();
});
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('rules.page.description')"
      :title="t('rules.page.title')"
    >
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center"
                   icon="i-lucide-plus"
                   @click="openCreate">
          {{ t('rules.newRule') }}
        </AppButton>
      </template>
    </PageHeader>

    <div class="space-y-4">
      <div v-if="store.pending && store.rules.length === 0" class="space-y-3">
        <CardSkeleton v-for="i in 3" :key="i"/>
      </div>

      <AppEmptyState v-else-if="store.rules.length === 0"
                     :description="t('rules.emptyDescription')"
                     :title="t('rules.emptyTitle')"
                     icon="i-lucide-wand-sparkles">
        <template #actions>
          <AppButton class="w-full sm:w-auto justify-center" @click="openCreate">
            {{ t('rules.newRule') }}
          </AppButton>
        </template>
      </AppEmptyState>

      <div v-else class="space-y-3">
        <UCard v-for="rule in store.rules" :key="rule.id" :ui="{body: 'p-3 sm:p-4'}" variant="outline">
          <div class="space-y-2.5">
            <div class="flex items-center gap-2 flex-wrap">
              <span class="font-medium truncate">{{ rule.name }}</span>
              <UBadge v-if="!rule.enabled" color="neutral" variant="subtle">{{ t('rules.disabledBadge') }}</UBadge>
              <div class="ml-auto flex items-center gap-1">
                <AppButton :aria-label="t('rules.applyAria')"
                           color="neutral" icon="i-lucide-wand-sparkles" size="xs" variant="ghost"
                           @click="apply(rule)"/>
                <AppButton :aria-label="t('rules.editAria')"
                           color="neutral" icon="i-lucide-pencil" size="xs" variant="ghost"
                           @click="openEdit(rule)"/>
                <AppButton :aria-label="t('rules.deleteAria')"
                           color="neutral" icon="i-lucide-trash" size="xs" variant="ghost"
                           @click="openDelete(rule)"/>
              </div>
            </div>

            <div class="flex items-center gap-1.5 flex-wrap">
              <span class="text-[0.65rem] font-semibold uppercase tracking-wide text-muted">
                {{ rule.matchAll ? t('rules.matchAll') : t('rules.matchAny') }}
              </span>
              <UBadge v-for="(c, i) in rule.conditions" :key="`c-${i}`" color="neutral" variant="subtle">
                {{ conditionText(c) }}
              </UBadge>
            </div>

            <div class="flex items-center gap-1.5 flex-wrap">
              <UIcon class="w-4 h-4 shrink-0 text-muted" name="i-lucide-corner-down-right"/>
              <template v-for="(a, i) in rule.actions" :key="`a-${i}`">
                <CategoryBadge v-if="a.type === 'SET_CATEGORY' && a.category"
                               :color="a.category.color"
                               :icon="a.category.icon"
                               :name="a.category.name"/>
                <span v-else-if="a.type === 'ADD_TAG' && a.tag"
                      :style="{backgroundColor: `${a.tag.color}1a`, color: a.tag.color}"
                      class="inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium">
                  #{{ a.tag.name }}
                </span>
              </template>
            </div>
          </div>
        </UCard>
      </div>

      <RuleModal v-if="isRuleModalOpen" v-model:open="isRuleModalOpen" :rule="selected"/>

      <ConfirmationModal v-if="isDeleteOpen && selected"
                         v-model:open="isDeleteOpen"
                         :delete-callback="() => store.deleteRule(selected?.id ?? '')"
                         :entity="t('rules.entity')"/>
    </div>
  </UContainer>
</template>
