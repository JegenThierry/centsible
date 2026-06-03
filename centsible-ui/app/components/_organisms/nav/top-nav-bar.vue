<script lang="ts" setup>

import AppButton from "~/components/_atoms/ui/app-button.vue";
import Profile from "~/components/_organisms/nav/profile.vue";
import ProfileSkeleton from "~/components/_molecules/skeletons/profile-skeleton.vue";
import ThemePicker from "~/components/_organisms/theme/theme-picker.vue";
import LanguagePicker from "~/components/_atoms/language/language-picker.vue";
import BrandMark from "~/components/_atoms/brand/brand-mark.vue";
import NotificationBell from "~/components/_organisms/notifications/notification-bell.vue";
import {useUserStore} from "~/stores/userStore";
import {useAuthStore} from "~/stores/authStore";
import {useSidebar} from "~/composables/use-sidebar";

const userStore = useUserStore();
const authStore = useAuthStore();
const {open} = useSidebar();
const {t, setLocale, locale} = useI18n();

const currentPanelIcon = computed(() => open.value ? 'i-lucide-panel-left' : 'i-lucide-panel-right');

function onToggleOpen() {
  open.value = !open.value;
}

onMounted(async () => {
  if (!authStore.isAuthenticated) return;
  await userStore.fetchMyself();
  const stored = userStore.user?.locale;
  if (stored && stored !== locale.value) await setLocale(stored as 'en' | 'fr' | 'de');
});
</script>

<template>
  <div class="h-(--ui-header-height) shrink-0 flex items-center px-4 border-b border-default">
    <div class="flex-1 flex items-center gap-2">
      <ClientOnly>
        <AppButton v-if="authStore.isAuthenticated"
                 :aria-label="t('nav.toggleSidebar')"
                 :icon="currentPanelIcon"
                 color="neutral"
                 variant="ghost"
                 @click="onToggleOpen()"/>
        <NuxtLink v-else
                  aria-label="Centsible home"
                  class="flex items-center transition-opacity hover:opacity-90"
                  to="/">
          <BrandMark/>
        </NuxtLink>
      </ClientOnly>
    </div>

    <ClientOnly>
      <div class="flex items-center gap-2">
        <LanguagePicker/>
        <ThemePicker/>
        <template v-if="authStore.isAuthenticated">
          <NotificationBell/>
          <Profile v-if="userStore.user" :user="userStore.user"/>
          <ProfileSkeleton v-else/>
        </template>
        <AppButton v-else
                 color="primary"
                 icon="i-lucide-log-in"
                 to="/auth">
          {{ t('auth.login.signIn') }}
        </AppButton>
      </div>
    </ClientOnly>
  </div>
</template>
