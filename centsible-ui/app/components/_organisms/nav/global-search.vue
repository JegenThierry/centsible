<script lang="ts" setup>
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {currencyIcon} from "~/models/budget-account/currency";
import AppButton from "~/components/_atoms/ui/app-button.vue";

const accountStore = useBudgetAccountsStore();
const {t} = useI18n();

const open = ref(false);

defineShortcuts({
  meta_k: () => {
    open.value = !open.value;
  },
});

function go(to: string): void {
  open.value = false;
  navigateTo(to);
}

const activeId = computed(() => accountStore.activeAccount?.id);

const groups = computed(() => {
  const id = activeId.value;

  const navItems: any[] = [
    {label: t('nav.sidebar.accounts'), icon: 'i-lucide-wallet', onSelect: () => go('/accounts')},
  ];
  if (id) {
    navItems.push(
      {label: t('nav.sidebar.dashboard'), icon: 'i-lucide-layout-dashboard', onSelect: () => go(`/${id}/dashboard`)},
      {label: t('nav.sidebar.transactions'), icon: 'i-lucide-arrow-right-left', onSelect: () => go(`/${id}/transactions`)},
      {label: t('nav.sidebar.recurring'), icon: 'i-lucide-repeat', onSelect: () => go(`/${id}/recurring`)},
    );
  }
  navItems.push(
    {label: t('nav.sidebar.budgets'), icon: 'i-lucide-piggy-bank', onSelect: () => go('/budgets')},
    {label: t('nav.sidebar.loans'), icon: 'i-lucide-hand-coins', onSelect: () => go('/loans')},
    {label: t('nav.sidebar.reports'), icon: 'i-lucide-trending-up', onSelect: () => go('/reports')},
    {label: t('nav.sidebar.categories'), icon: 'i-lucide-tag', onSelect: () => go('/categories')},
    {label: t('nav.sidebar.rules'), icon: 'i-lucide-wand-sparkles', onSelect: () => go('/rules')},
    {label: t('nav.sidebar.tags'), icon: 'i-lucide-hash', onSelect: () => go('/tags')},
    {label: t('nav.sidebar.contacts'), icon: 'i-lucide-users', onSelect: () => go('/contacts')},
    {label: t('nav.sidebar.exports'), icon: 'i-lucide-file-text', onSelect: () => go('/exports')},
    {label: t('nav.sidebar.documents'), icon: 'i-lucide-paperclip', onSelect: () => go('/attachments')},
    {label: t('nav.sidebar.integrations'), icon: 'i-lucide-plug', onSelect: () => go('/integrations')},
  );

  const accountItems = accountStore.availableAccounts.map(a => ({
    label: a.name,
    suffix: a.currency,
    icon: currencyIcon(a.currency),
    onSelect: () => go(`/${a.id}/dashboard`),
  }));

  return [
    {id: 'navigation', label: t('nav.search.goTo'), items: navItems},
    {id: 'accounts', label: t('nav.search.accounts'), items: accountItems},
  ];
});
</script>

<template>
  <div class="contents">
    <button
      :aria-label="t('nav.search.placeholder')"
      class="hidden sm:flex items-center gap-2 h-8 ps-2.5 pe-1.5 w-44 lg:w-56 rounded-md bg-elevated ring ring-default text-muted hover:bg-accented hover:text-default transition-colors"
      type="button"
      @click="open = true"
    >
      <UIcon class="size-4 shrink-0" name="i-lucide-search"/>
      <span class="flex-1 text-left text-sm truncate">{{ t('nav.search.placeholder') }}</span>
      <span class="hidden lg:flex items-center gap-0.5">
        <UKbd value="meta"/>
        <UKbd value="k"/>
      </span>
    </button>

    <AppButton
      :aria-label="t('nav.search.placeholder')"
      class="sm:hidden"
      color="neutral"
      icon="i-lucide-search"
      variant="ghost"
      @click="open = true"
    />

    <UModal v-model:open="open" :ui="{content: 'sm:max-w-xl'}">
      <template #content>
        <UCommandPalette
          :close="true"
          :groups="groups"
          :placeholder="t('nav.search.placeholder')"
          class="h-96"
          @update:open="open = $event"
        />
      </template>
    </UModal>
  </div>
</template>
