<script lang="ts" setup>
import CategorySuggestionChip from "~/components/_atoms/onboarding/category-suggestion-chip.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";

interface Suggestion {
  name: string;
  icon: string;
  color: string;
}

interface Props {
  expenseSuggestions: Suggestion[];
  incomeSuggestions: Suggestion[];
  selectedCategories: Set<string>;
  loading?: boolean;
}

defineProps<Props>();
const emit = defineEmits<{
  (e: 'toggle', name: string): void;
  (e: 'back'): void;
  (e: 'submit'): void;
}>();

const {t} = useI18n();
</script>

<template>
  <div class="space-y-4">
    <h2 class="text-xl font-semibold">{{ t('onboarding.categories.title') }}</h2>
    <p class="text-sm text-muted">{{ t('onboarding.categories.description') }}</p>

    <div>
      <p class="text-xs font-medium uppercase text-muted mb-2">{{ t('onboarding.categories.expenses') }}</p>
      <div class="flex flex-wrap gap-2">
        <CategorySuggestionChip v-for="c in expenseSuggestions"
                                :key="c.name"
                                :label="c.name"
                                :icon="c.icon"
                                :selected="selectedCategories.has(c.name)"
                                @toggle="emit('toggle', c.name)"/>
      </div>
    </div>

    <div>
      <p class="text-xs font-medium uppercase text-muted mb-2">{{ t('onboarding.categories.income') }}</p>
      <div class="flex flex-wrap gap-2">
        <CategorySuggestionChip v-for="c in incomeSuggestions"
                                :key="c.name"
                                :label="c.name"
                                :icon="c.icon"
                                :selected="selectedCategories.has(c.name)"
                                @toggle="emit('toggle', c.name)"/>
      </div>
    </div>

    <div class="flex justify-between pt-2">
      <AppButton color="neutral" variant="ghost" @click="emit('back')">
        {{ t('onboarding.back') }}
      </AppButton>
      <AppButton :loading="loading" color="primary" trailing-icon="i-lucide-arrow-right" @click="emit('submit')">
        {{ t('onboarding.continue') }}
      </AppButton>
    </div>
  </div>
</template>
