<script lang="ts" setup>
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";

const accountStore = useBudgetAccountsStore();
const route = useRoute();
const {t} = useI18n();

const account = computed(() => accountStore.activeAccount);

const accountScoped = computed(() => {
  const id = account.value?.id;
  return !!id && route.path.startsWith(`/${id}`);
});

const sectionKey = computed<string | null>(() => {
  const p = route.path;
  if (p.includes('/dashboard')) return 'nav.sidebar.dashboard';
  if (p.includes('/transactions')) return 'nav.sidebar.transactions';
  if (p.includes('/recurring')) return 'nav.sidebar.recurring';
  if (p.startsWith('/budgets')) return 'nav.sidebar.budgets';
  if (p.startsWith('/loans')) return 'nav.sidebar.loans';
  if (p.startsWith('/reports')) return 'nav.sidebar.reports';
  if (p.startsWith('/categories')) return 'nav.sidebar.categories';
  if (p.startsWith('/rules')) return 'nav.sidebar.rules';
  if (p.startsWith('/tags')) return 'nav.sidebar.tags';
  if (p.startsWith('/contacts')) return 'nav.sidebar.contacts';
  if (p.startsWith('/exports')) return 'nav.sidebar.exports';
  if (p.startsWith('/attachments')) return 'nav.sidebar.documents';
  if (p.startsWith('/integrations')) return 'nav.sidebar.integrations';
  if (p.startsWith('/profile')) return 'nav.sidebar.profile';
  if (p.startsWith('/about')) return 'nav.footer.about';
  return null;
});

const items = computed(() => {
  const crumbs: Array<{ label: string; icon?: string; to?: string }> = [];

  if (accountScoped.value && account.value) {
    crumbs.push({label: account.value.name, to: `/${account.value.id}/dashboard`});
  } else {
    crumbs.push({label: t('nav.sidebar.accounts'), icon: 'i-lucide-wallet', to: '/accounts'});
  }

  if (sectionKey.value) crumbs.push({label: t(sectionKey.value)});

  return crumbs;
});
</script>

<template>
  <UBreadcrumb
    v-if="items.length > 1"
    :items="items"
    :ui="{root: 'min-w-0', linkLabel: 'truncate'}"
  />
</template>
