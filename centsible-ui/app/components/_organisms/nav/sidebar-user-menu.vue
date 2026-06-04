<script lang="ts" setup>
import {useUserStore} from "~/stores/userStore";
import {useAuthStore} from "~/stores/authStore";
import {useSidebar} from "~/composables/use-sidebar";

const userStore = useUserStore();
const authStore = useAuthStore();
const {open} = useSidebar();
const {t} = useI18n();

const user = computed(() => userStore.user);

const items = computed(() => [
  [
    {label: t('profile.dropdown.profile'), icon: 'i-lucide-user', onSelect: () => navigateTo('/profile')},
    {label: t('profile.dropdown.about'), icon: 'i-lucide-info', onSelect: () => navigateTo('/about')},
  ],
  [
    {label: t('profile.dropdown.logout'), icon: 'i-lucide-log-out', color: 'error' as any, onSelect: () => authStore.logout()},
  ],
]);
</script>

<template>
  <UDropdownMenu
    v-if="user"
    :content="{align: 'start', side: 'top', sideOffset: 8}"
    :items="items"
    :ui="{content: 'min-w-56'}"
  >
    <button
      :aria-label="user.name"
      :class="[
        'w-full flex items-center gap-2 p-1.5 rounded-md hover:bg-elevated transition-colors cursor-pointer',
        open ? '' : 'justify-center',
      ]"
      type="button"
    >
      <UAvatar
        :alt="user.name"
        :icon="user.profilePicture ? undefined : 'i-lucide-user'"
        :src="user.profilePicture || undefined"
        class="shrink-0"
        size="xs"
      />
      <template v-if="open">
        <span class="flex-1 min-w-0 text-left">
          <span class="block text-sm font-medium truncate text-highlighted leading-tight">{{ user.name }}</span>
          <span class="block text-xs text-muted truncate leading-tight mt-0.5">{{ user.email }}</span>
        </span>
        <UIcon class="size-4 text-dimmed shrink-0" name="i-lucide-chevrons-up-down"/>
      </template>
    </button>
  </UDropdownMenu>

  <div v-else :class="['flex items-center gap-2 p-1.5', open ? '' : 'justify-center']">
    <div class="size-6 rounded-full bg-elevated animate-pulse shrink-0"/>
    <div v-if="open" class="flex-1 min-w-0 space-y-1.5">
      <div class="h-3 w-20 rounded bg-elevated animate-pulse"/>
      <div class="h-2.5 w-28 rounded bg-elevated animate-pulse"/>
    </div>
  </div>
</template>
