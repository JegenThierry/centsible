<script lang="ts" setup>
import {computed} from 'vue';
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useUserStore} from "~/stores/userStore";
import {useSidebar} from "~/composables/use-sidebar";
import BrandMark from "~/components/_atoms/brand/brand-mark.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AccountSwitcher from "~/components/_organisms/nav/account-switcher.vue";
import SidebarUserMenu from "~/components/_organisms/nav/sidebar-user-menu.vue";

const accountStore = useBudgetAccountsStore();
const userStore = useUserStore();
const {open} = useSidebar();
const {t} = useI18n();

const items = computed(() => {
  const accountId = accountStore.activeAccount?.id;

  const primary: any[] = [
    {label: t('nav.sidebar.accounts'), to: '/accounts', icon: 'i-lucide-wallet', target: '_self'},
  ];

  if (accountId) {
    primary.push(
      {label: t('nav.sidebar.dashboard'), to: `/${accountId}/dashboard`, icon: 'i-lucide-layout-dashboard', target: '_self'},
      {label: t('nav.sidebar.transactions'), to: `/${accountId}/transactions`, icon: 'i-lucide-arrow-right-left', target: '_self'},
    );
  }

  // Budgets and loans are ledger-wide (no account FK), so they stay visible without an
  // active account; only recurring is account-scoped and appears when one is selected.
  const planChildren: any[] = [
    {label: t('nav.sidebar.budgets'), to: '/budgets', icon: 'i-lucide-piggy-bank', target: '_self'},
  ];
  if (accountId) {
    planChildren.push({label: t('nav.sidebar.recurring'), to: `/${accountId}/recurring`, icon: 'i-lucide-repeat', target: '_self'});
  }
  planChildren.push({label: t('nav.sidebar.loans'), to: '/loans', icon: 'i-lucide-hand-coins', target: '_self'});

  primary.push({
    label: t('nav.sidebar.plan'),
    icon: 'i-lucide-target',
    defaultOpen: true,
    children: planChildren,
  });

  const manage: any[] = [
    {label: t('nav.sidebar.sections.manage'), type: 'label'},
    {label: t('nav.sidebar.reports'), to: '/reports', icon: 'i-lucide-trending-up', target: '_self'},
    {label: t('nav.sidebar.categories'), to: '/categories', icon: 'i-lucide-tag', target: '_self'},
    {label: t('nav.sidebar.rules'), to: '/rules', icon: 'i-lucide-wand-sparkles', target: '_self'},
    {label: t('nav.sidebar.tags'), to: '/tags', icon: 'i-lucide-hash', target: '_self'},
    {label: t('nav.sidebar.contacts'), to: '/contacts', icon: 'i-lucide-users', target: '_self'},
  ];

  const data: any[] = [
    {label: t('nav.sidebar.sections.data'), type: 'label'},
    {label: t('nav.sidebar.exports'), to: '/exports', icon: 'i-lucide-file-text', target: '_self'},
    {label: t('nav.sidebar.documents'), to: '/attachments', icon: 'i-lucide-paperclip', target: '_self'},
    {label: t('nav.sidebar.integrations'), to: '/integrations', icon: 'i-lucide-plug', target: '_self'},
  ];

  if (!userStore.user?.admin) return [primary, manage, data];

  const admin: any[] = [
    {label: t('nav.sidebar.sections.admin'), type: 'label'},
    {label: t('nav.sidebar.admin'), to: '/admin', icon: 'i-lucide-shield', target: '_self'},
  ];

  return [primary, manage, data, admin];
})
</script>

<template>
  <USidebar
    v-model:open="open"
    :ui="{
      container: 'h-full m-0',
      content: 'rounded-none sm:rounded-xl',
      footer: 'border-t border-default'
    }"
    collapsible="icon"
    side="left"
    variant="inset"
  >
    <template #header>
      <div class="flex items-center justify-between w-full">
        <NuxtLink :aria-label="t('nav.brandHome')"
                  class="flex items-center transition-opacity hover:opacity-90"
                  to="/accounts">
          <BrandMark :show-wordmark="open"/>
        </NuxtLink>
        <AppButton
          :aria-label="t('nav.closeSidebar')"
          class="lg:hidden"
          color="neutral"
          icon="i-lucide-x"
          variant="ghost"
          @click="open = false"
        />
      </div>
    </template>

    <AccountSwitcher/>

    <UNavigationMenu
      :items="items"
      :ui="{ link: 'p-1.5 overflow-hidden' }"
      orientation="vertical"
    />

    <template #footer>
      <SidebarUserMenu/>
    </template>
  </USidebar>
</template>
