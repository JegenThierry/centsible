<script lang="ts" setup>
import {useBudgetAccountsStore} from "~/stores/budgetAccountsStore";
import {useSidebar} from "~/composables/use-sidebar";
import {currencyIcon} from "~/models/budget-account/currency";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";

const accountStore = useBudgetAccountsStore();
const {open} = useSidebar();
const {t} = useI18n();

const active = computed(() => accountStore.activeAccount);

const items = computed(() => [
  accountStore.availableAccounts.map(a => ({
    label: a.name,
    icon: a.id === active.value?.id ? 'i-lucide-check' : currencyIcon(a.currency),
    onSelect: () => navigateTo(`/${a.id}/dashboard`),
  })),
  [{
    label: t('nav.switcher.allAccounts'),
    icon: 'i-lucide-wallet',
    onSelect: () => navigateTo('/accounts'),
  }],
]);
</script>

<template>
  <UDropdownMenu v-if="active" :content="{align: 'start'}" :items="items" :ui="{content: 'min-w-56'}">
    <button
      :aria-label="t('nav.switcher.label')"
      :class="[
        'w-full flex items-center gap-1.5 p-1.5 rounded-md ring ring-default hover:bg-elevated transition-colors cursor-pointer',
        open ? '' : 'justify-center',
      ]"
      type="button"
    >
      <UIcon :name="currencyIcon(active.currency)" class="shrink-0 size-5 text-primary"/>
      <template v-if="open">
        <span class="flex-1 min-w-0 text-left">
          <span class="block text-sm font-medium truncate text-highlighted leading-tight">{{ active.name }}</span>
          <span class="block text-xs text-muted truncate tabular-nums leading-tight mt-0.5">
            <BalanceNumberFormat :balance="active.balance" :currency="active.currency"/>
          </span>
        </span>
        <UIcon class="size-4 text-dimmed shrink-0" name="i-lucide-chevrons-up-down"/>
      </template>
    </button>
  </UDropdownMenu>
</template>
