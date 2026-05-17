<script lang="ts" setup>
import {computed, ref} from 'vue';
import {Currency, currencyOptions} from "~/models/budget-account/currency";
import {CategoryType} from "~/models/category/category";
import {useBudgetAccountService} from "~/services/budget-account/budget-account-service";
import {useCategoryService} from "~/services/category/category-service";
import {useToasts} from "~/services/toasts/toast-service";
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useCategoriesStore} from "~/stores/categoriesStore";
import {useApiErrors} from "~/composables/use-api-errors";

definePageMeta({middleware: ['auth-guard']});

const {t} = useI18n();
const accountService = useBudgetAccountService(useApi());
const categoryService = useCategoryService(useApi());
const accountsStore = useBudgetAccountsStore();
const categoriesStore = useCategoriesStore();
const toasts = useToasts();
const apiErrors = useApiErrors();

useHead({title: () => t('onboarding.pageTitle')});

const step = ref<1 | 2 | 3>(1);
const loading = ref(false);

const accountForm = ref({
  name: '',
  initialBalance: 0,
  currency: Currency.EUR,
});

const createdAccountId = ref<string | null>(null);

const expenseSuggestions = [
  {name: 'Groceries', icon: 'i-lucide-shopping-cart', color: '#10b981'},
  {name: 'Rent', icon: 'i-lucide-home', color: '#3b82f6'},
  {name: 'Transport', icon: 'i-lucide-car', color: '#f59e0b'},
  {name: 'Dining', icon: 'i-lucide-utensils', color: '#ef4444'},
  {name: 'Entertainment', icon: 'i-lucide-film', color: '#a855f7'},
  {name: 'Utilities', icon: 'i-lucide-zap', color: '#06b6d4'},
];

const incomeSuggestions = [
  {name: 'Salary', icon: 'i-lucide-briefcase', color: '#10b981'},
  {name: 'Freelance', icon: 'i-lucide-laptop', color: '#3b82f6'},
];

const selectedCategories = ref<Set<string>>(new Set(['Groceries', 'Rent', 'Salary']));

function toggleSuggestion(name: string) {
  const next = new Set(selectedCategories.value);
  if (next.has(name)) next.delete(name); else next.add(name);
  selectedCategories.value = next;
}

const activeIcon = computed(() =>
  currencyOptions.find((item) => item.value === accountForm.value.currency)?.icon
);

async function submitAccount() {
  if (!accountForm.value.name.trim()) {
    toasts.error(t('onboarding.account.errorTitle'), t('onboarding.account.errorBody'));
    return;
  }
  loading.value = true;
  try {
    const created = await accountService.createAccount({
      name: accountForm.value.name.trim(),
      initialBalance: Number(accountForm.value.initialBalance) || 0,
      currency: accountForm.value.currency,
    });
    createdAccountId.value = created.id;
    await accountsStore.updateAvailableAccounts();
    step.value = 2;
  } catch (e) {
    apiErrors.toastError(e, t('onboarding.account.errorTitle'), t('onboarding.account.errorBody'));
  } finally {
    loading.value = false;
  }
}

async function submitCategories() {
  if (selectedCategories.value.size === 0) {
    step.value = 3;
    return;
  }
  loading.value = true;
  try {
    const all = [...expenseSuggestions.map((s) => ({...s, type: CategoryType.EXPENSE})),
                 ...incomeSuggestions.map((s) => ({...s, type: CategoryType.INCOME}))];
    const toCreate = all.filter((s) => selectedCategories.value.has(s.name));
    for (const c of toCreate) {
      try {
        await categoryService.createCategory(c);
      } catch (e) {
        // skip if already exists
      }
    }
    await categoriesStore.updateCategories();
    step.value = 3;
  } catch (e) {
    apiErrors.toastError(e, t('onboarding.categories.errorTitle'), t('onboarding.categories.errorBody'));
  } finally {
    loading.value = false;
  }
}

function finish() {
  localStorage.setItem('centsible.onboarded', '1');
  if (createdAccountId.value) {
    navigateTo(`/${createdAccountId.value}/dashboard`);
  } else {
    navigateTo('/accounts');
  }
}

function skipAll() {
  localStorage.setItem('centsible.onboarded', '1');
  navigateTo('/accounts');
}
</script>

<template>
  <UContainer class="py-10 max-w-2xl">
    <div class="mb-8 text-center">
      <h1 class="text-3xl font-bold tracking-tight">{{ t('onboarding.welcome') }}</h1>
      <p class="text-muted mt-2">{{ t('onboarding.tagline') }}</p>
    </div>

    <div class="flex items-center justify-center gap-2 mb-8">
      <div v-for="i in 3"
           :key="i"
           :class="['h-2 w-12 rounded-full transition-colors', step >= i ? 'bg-primary-500' : 'bg-neutral-300 dark:bg-neutral-700']"/>
    </div>

    <UCard>
      <div v-if="step === 1" class="space-y-4">
        <h2 class="text-xl font-semibold">{{ t('onboarding.account.title') }}</h2>
        <p class="text-sm text-muted">{{ t('onboarding.account.description') }}</p>

        <div class="space-y-3">
          <UInput v-model="accountForm.name"
                  :placeholder="t('onboarding.account.namePlaceholder')"
                  size="lg"/>
          <UInput v-model.number="accountForm.initialBalance"
                  :placeholder="t('onboarding.account.balancePlaceholder')"
                  size="lg"
                  type="number"/>
          <USelect v-model="accountForm.currency"
                   :icon="activeIcon"
                   :items="currencyOptions"
                   class="w-full"
                   size="lg"/>
        </div>

        <div class="flex justify-between pt-2">
          <UButton color="neutral" variant="ghost" @click="skipAll">
            {{ t('onboarding.skip') }}
          </UButton>
          <UButton :loading="loading" color="primary" trailing-icon="i-lucide-arrow-right" @click="submitAccount">
            {{ t('onboarding.continue') }}
          </UButton>
        </div>
      </div>

      <div v-else-if="step === 2" class="space-y-4">
        <h2 class="text-xl font-semibold">{{ t('onboarding.categories.title') }}</h2>
        <p class="text-sm text-muted">{{ t('onboarding.categories.description') }}</p>

        <div>
          <p class="text-xs font-medium uppercase text-muted mb-2">{{ t('onboarding.categories.expenses') }}</p>
          <div class="flex flex-wrap gap-2">
            <button v-for="c in expenseSuggestions"
                    :key="c.name"
                    :class="['flex items-center gap-2 px-3 py-1.5 rounded-full border text-sm transition-colors',
                             selectedCategories.has(c.name)
                               ? 'bg-primary-500 border-primary-500 text-white'
                               : 'border-neutral-300 dark:border-neutral-700 hover:border-primary-400']"
                    @click="toggleSuggestion(c.name)">
              <UIcon :name="c.icon" class="w-4 h-4"/>
              {{ c.name }}
            </button>
          </div>
        </div>

        <div>
          <p class="text-xs font-medium uppercase text-muted mb-2">{{ t('onboarding.categories.income') }}</p>
          <div class="flex flex-wrap gap-2">
            <button v-for="c in incomeSuggestions"
                    :key="c.name"
                    :class="['flex items-center gap-2 px-3 py-1.5 rounded-full border text-sm transition-colors',
                             selectedCategories.has(c.name)
                               ? 'bg-primary-500 border-primary-500 text-white'
                               : 'border-neutral-300 dark:border-neutral-700 hover:border-primary-400']"
                    @click="toggleSuggestion(c.name)">
              <UIcon :name="c.icon" class="w-4 h-4"/>
              {{ c.name }}
            </button>
          </div>
        </div>

        <div class="flex justify-between pt-2">
          <UButton color="neutral" variant="ghost" @click="step = 1">
            {{ t('onboarding.back') }}
          </UButton>
          <UButton :loading="loading" color="primary" trailing-icon="i-lucide-arrow-right" @click="submitCategories">
            {{ t('onboarding.continue') }}
          </UButton>
        </div>
      </div>

      <div v-else class="space-y-4 text-center">
        <UIcon class="w-12 h-12 mx-auto text-success" name="i-lucide-check-circle"/>
        <h2 class="text-xl font-semibold">{{ t('onboarding.done.title') }}</h2>
        <p class="text-sm text-muted">{{ t('onboarding.done.description') }}</p>
        <UButton color="primary"
                 size="lg"
                 trailing-icon="i-lucide-layout-dashboard"
                 @click="finish">
          {{ t('onboarding.done.cta') }}
        </UButton>
      </div>
    </UCard>
  </UContainer>
</template>
