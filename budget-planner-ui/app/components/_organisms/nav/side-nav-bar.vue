<script lang="ts" setup>
import {computed} from 'vue';
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useSidebar} from "~/composables/use-sidebar";
import BrandMark from "~/components/_atoms/brand/brand-mark.vue";

const accountStore = useBudgetAccountsStore();
const {open} = useSidebar();
const {t} = useI18n();

const items = computed(() => {
  const accountId = accountStore.activeAccount?.id;
  const menuItems: any[] = [
    {
      label: t('nav.sidebar.accounts'),
      to: '/accounts',
      icon: 'i-lucide-wallet',
      target: '_self'
    },
  ];

  if (accountId) {
    menuItems.push(
      {
        label: t('nav.sidebar.dashboard'),
        to: `/${accountId}/dashboard`,
        icon: 'i-lucide-layout-dashboard',
        target: '_self'
      },
      {
        label: t('nav.sidebar.transactions'),
        to: `/${accountId}/transactions`,
        icon: 'i-lucide-arrow-right-left',
        target: '_self'
      },
      {
        label: t('nav.sidebar.plan'),
        icon: 'i-lucide-target',
        defaultOpen: true,
        children: [
          {
            label: t('nav.sidebar.budgets'),
            to: '/budgets',
            icon: 'i-lucide-piggy-bank',
            target: '_self'
          },
          {
            label: t('nav.sidebar.recurring'),
            to: `/${accountId}/recurring`,
            icon: 'i-lucide-repeat',
            target: '_self'
          }
        ]
      }
    );
  }

  menuItems.push(
    {
      label: t('nav.sidebar.categories'),
      to: '/categories',
      icon: 'i-lucide-tag',
      target: '_self'
    },
    {
      label: t('nav.sidebar.contacts'),
      to: '/contacts',
      icon: 'i-lucide-users',
      target: '_self'
    },
    {
      label: t('nav.sidebar.myDocuments'),
      to: '/exports',
      icon: 'i-lucide-file-text',
      target: '_self'
    },
    {
      label: t('nav.sidebar.integrations'),
      to: '/integrations',
      icon: 'i-lucide-plug',
      target: '_self'
    },
    {
      label: t('nav.sidebar.profile'),
      to: '/profile',
      icon: 'i-lucide-user',
      target: '_self'
    }
  );

  return menuItems;
})
</script>

<template>
  <USidebar
    v-model:open="open"
    :ui="{
      container: 'h-full m-0',
      content: 'rounded-none sm:rounded-xl'
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
        <UButton
          :aria-label="t('nav.closeSidebar')"
          class="lg:hidden"
          color="neutral"
          icon="i-lucide-x"
          variant="ghost"
          @click="open = false"
        />
      </div>
    </template>

    <UNavigationMenu
      :items="items"
      :ui="{ link: 'p-1.5 overflow-hidden' }"
      orientation="vertical"
    />
  </USidebar>
</template>
