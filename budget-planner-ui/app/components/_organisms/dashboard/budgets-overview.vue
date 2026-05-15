<script lang="ts" setup>
import {useBudgetsStore} from "~/stores/budgetsStore";
import {Currency} from "~/models/budget-account/currency";
import BudgetProgressBar from "~/components/_molecules/budgets/budget-progress-bar.vue";

const props = defineProps<{
  currency: Currency;
}>();

const store = useBudgetsStore();

const top = computed(() => {
  return [...store.items]
    .sort((a, b) => {
      const ratioA = a.amountLimit > 0 ? a.amountSpent / a.amountLimit : 0;
      const ratioB = b.amountLimit > 0 ? b.amountSpent / b.amountLimit : 0;
      return ratioB - ratioA;
    })
    .slice(0, 5);
});

onMounted(() => {
  if (store.items.length === 0) store.fetchCurrentMonth();
});
</script>

<template>
  <UCard v-if="store.items.length > 0" variant="outline">
    <template #header>
      <div class="flex items-center justify-between">
        <h3 class="font-semibold">Budgets this month</h3>
        <NuxtLink class="text-sm text-primary-500 hover:underline" to="/budgets">View all</NuxtLink>
      </div>
    </template>
    <div class="space-y-4">
      <BudgetProgressBar v-for="budget in top"
                         :key="budget.id"
                         :budget="budget"
                         :currency="props.currency"/>
    </div>
  </UCard>
</template>
