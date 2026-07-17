<script lang="ts" setup>
import type {TableColumn} from '@nuxt/ui';
import PageHeader from "~/components/_molecules/page/page-header.vue";
import BaseTable from "~/components/_molecules/tables/base-table.vue";
import ConfirmationModal from "~/components/_organisms/modals/confirmation-modal.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import type {AdminUser} from "~/models/admin/admin-user";
import {formatRelativeToNow} from "~/utils/date";
import {useAdminStore} from "~/stores/adminStore";
import {useUserStore} from "~/stores/userStore";

const adminStore = useAdminStore();
const userStore = useUserStore();
const {t, locale} = useI18n();

const columns = computed<TableColumn<AdminUser>[]>(() => [
  {accessorKey: 'username', header: t('admin.users.columns.user')},
  {accessorKey: 'registered', header: t('admin.users.columns.status')},
  {accessorKey: 'accountCount', header: t('admin.users.columns.accounts')},
  {accessorKey: 'transactionCount', header: t('admin.users.columns.transactions')},
  {accessorKey: 'createdAt', header: t('admin.users.columns.createdAt')},
  {accessorKey: 'lastSeenAt', header: t('admin.users.columns.lastSeen')},
  {accessorKey: 'lastTransactionAt', header: t('admin.users.columns.lastTransaction')},
  {id: 'actions', header: ''},
]);

const STALE_AFTER_MS = 90 * 24 * 60 * 60 * 1000;

function lastActivityMs(user: AdminUser): number | null {
  const stamps = [user.lastSeenAt, user.lastLoginAt, user.lastTransactionAt]
    .filter((value): value is string => !!value)
    .map((value) => new Date(value).getTime());
  return stamps.length ? Math.max(...stamps) : null;
}

function isStale(user: AdminUser): boolean {
  const reference = lastActivityMs(user) ?? new Date(user.createdAt).getTime();
  return Date.now() - reference > STALE_AFTER_MS;
}

const deleteTarget = ref<AdminUser>();
const deleteOpen = ref(false);
const resendPendingId = ref<string | null>(null);

function formatDate(iso: string): string {
  return new Intl.DateTimeFormat(locale.value, {dateStyle: 'medium'}).format(new Date(iso));
}

function askDelete(user: AdminUser) {
  deleteTarget.value = user;
  deleteOpen.value = true;
}

async function confirmDelete() {
  if (!deleteTarget.value) return;
  await adminStore.deleteUser(deleteTarget.value.id);
}

async function resendVerification(user: AdminUser) {
  resendPendingId.value = user.id;
  try {
    await adminStore.resendVerification(user.id);
  } catch {
  } finally {
    resendPendingId.value = null;
  }
}

function refresh() {
  adminStore.fetchUsers().catch(() => {
  });
}

onMounted(refresh);
</script>

<template>
  <UContainer class="py-6 sm:py-10 space-y-4 sm:space-y-6">
    <PageHeader
      :description="t('admin.page.description')"
      :title="t('admin.page.title')"
    />

    <BaseTable
      :columns="columns"
      :data="adminStore.users"
      :error="adminStore.error"
      :error-title="t('admin.users.errorTitle')"
      :loading="adminStore.pending"
      empty-icon="i-lucide-users"
      :empty-title="t('admin.users.empty')"
      @retry="refresh"
    >
      <template #username-cell="{ row }">
        <div class="flex flex-col">
          <span class="font-medium">{{ row.original.username }}</span>
          <span class="text-xs text-muted">{{ row.original.email }}</span>
        </div>
      </template>

      <template #registered-cell="{ row }">
        <div class="flex flex-wrap items-center gap-1.5">
          <UBadge :color="row.original.registered ? 'success' : 'warning'" size="sm" variant="subtle">
            {{ row.original.registered ? t('admin.users.status.registered') : t('admin.users.status.pending') }}
          </UBadge>
          <UBadge v-if="row.original.totpEnabled" color="info" size="sm" variant="subtle">
            {{ t('admin.users.status.twoFactor') }}
          </UBadge>
          <UBadge v-if="row.original.id === userStore.user?.id" color="primary" size="sm" variant="subtle">
            {{ t('admin.users.status.you') }}
          </UBadge>
          <UBadge v-if="isStale(row.original)" color="neutral" size="sm" variant="subtle">
            {{ t('admin.users.status.inactive') }}
          </UBadge>
        </div>
      </template>

      <template #createdAt-cell="{ row }">
        {{ formatDate(row.original.createdAt) }}
      </template>

      <template #lastSeenAt-cell="{ row }">
        <span v-if="row.original.lastSeenAt ?? row.original.lastLoginAt"
              :title="formatDate(row.original.lastSeenAt ?? row.original.lastLoginAt!)">
          {{ formatRelativeToNow(row.original.lastSeenAt ?? row.original.lastLoginAt!, locale) }}
        </span>
        <span v-else class="text-muted">{{ t('admin.users.never') }}</span>
      </template>

      <template #lastTransactionAt-cell="{ row }">
        <span v-if="row.original.lastTransactionAt" :title="formatDate(row.original.lastTransactionAt)">
          {{ formatRelativeToNow(row.original.lastTransactionAt, locale) }}
        </span>
        <span v-else class="text-muted">{{ t('admin.users.never') }}</span>
      </template>

      <template #actions-cell="{ row }">
        <div class="flex items-center justify-end gap-1">
          <UTooltip v-if="!row.original.registered" :text="t('admin.users.actions.resend')">
            <AppButton
              :aria-label="t('admin.users.actions.resend')"
              :loading="resendPendingId === row.original.id"
              color="neutral"
              icon="i-lucide-mail"
              size="sm"
              variant="ghost"
              @click="resendVerification(row.original)"
            />
          </UTooltip>
          <UTooltip v-if="row.original.id !== userStore.user?.id" :text="t('admin.users.actions.delete')">
            <AppButton
              :aria-label="t('admin.users.actions.delete')"
              color="error"
              icon="i-lucide-trash-2"
              size="sm"
              variant="ghost"
              @click="askDelete(row.original)"
            />
          </UTooltip>
        </div>
      </template>
    </BaseTable>

    <ConfirmationModal
      v-model:open="deleteOpen"
      :body="t('admin.users.delete.body', {username: deleteTarget?.username ?? ''})"
      :confirm-label="t('admin.users.delete.submit')"
      :delete-callback="confirmDelete"
      :manage-toasts="false"
      :title="t('admin.users.delete.title')"
    />
  </UContainer>
</template>
