<script lang="ts" setup>
import {useCategorizationRulesStore} from "~/stores/categorizationRulesStore";
import {useToasts} from "~/services/toasts/toast-service";
import type {CategorizationRule} from "~/models/categorization/rule";
import CategoryBadge from "~/components/_molecules/badges/category-badge.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";

const RuleModal = defineAsyncComponent(() => import("~/components/_organisms/categories/modals/rule-modal.vue"));
const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const store = useCategorizationRulesStore();
const toasts = useToasts();
const {t} = useI18n();

const isRuleModalOpen = ref(false);
const isDeleteOpen = ref(false);
const selected = ref<CategorizationRule | null>(null);

function openCreate() {
  selected.value = null;
  isRuleModalOpen.value = true;
}

function openEdit(rule: CategorizationRule) {
  selected.value = rule;
  isRuleModalOpen.value = true;
}

function openDelete(rule: CategorizationRule) {
  selected.value = rule;
  isDeleteOpen.value = true;
}

async function apply(rule: CategorizationRule) {
  try {
    const count = await store.applyRule(rule.id);
    toasts.success(t('categories.rules.appliedTitle'), t('categories.rules.appliedBody', {count}));
  } catch {
    // handled by the store toast
  }
}

onMounted(() => store.updateRules());
</script>

<template>
  <div class="space-y-4">
    <div class="flex justify-end">
      <AppButton icon="i-lucide-plus" @click="openCreate">{{ t('categories.rules.newRule') }}</AppButton>
    </div>

    <div v-if="store.pending && store.rules.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.rules.length === 0"
                   :description="t('categories.rules.emptyDescription')"
                   icon="i-lucide-wand-sparkles"
                   :title="t('categories.rules.emptyTitle')">
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center" @click="openCreate">
          {{ t('categories.rules.newRule') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <div v-else class="space-y-3">
      <UCard v-for="rule in store.rules"
             :key="rule.id"
             :ui="{body: 'p-3 sm:p-4'}"
             variant="outline">
        <div class="flex items-center gap-3 flex-wrap">
          <UBadge color="neutral" variant="subtle">{{ t(`categories.rules.match.${rule.matchType}`) }}</UBadge>
          <span class="font-medium truncate">{{ rule.pattern }}</span>
          <UIcon name="i-lucide-arrow-right" class="w-4 h-4 shrink-0 text-muted"/>
          <CategoryBadge :name="rule.category.name" :icon="rule.category.icon" :color="rule.category.color"/>
          <div class="ml-auto flex items-center gap-1">
            <AppButton :aria-label="t('categories.rules.applyAria')"
                       color="neutral" icon="i-lucide-wand-sparkles" size="xs" variant="ghost"
                       @click="apply(rule)"/>
            <AppButton :aria-label="t('categories.rules.editAria')"
                       color="neutral" icon="i-lucide-pencil" size="xs" variant="ghost"
                       @click="openEdit(rule)"/>
            <AppButton :aria-label="t('categories.rules.deleteAria')"
                       color="neutral" icon="i-lucide-trash" size="xs" variant="ghost"
                       @click="openDelete(rule)"/>
          </div>
        </div>
      </UCard>
    </div>

    <RuleModal v-if="isRuleModalOpen" v-model:open="isRuleModalOpen" :rule="selected"/>

    <ConfirmationModal v-if="isDeleteOpen && selected"
                       v-model:open="isDeleteOpen"
                       :entity="t('categories.rules.entity')"
                       :delete-callback="() => store.deleteRule(selected?.id ?? '')"/>
  </div>
</template>
