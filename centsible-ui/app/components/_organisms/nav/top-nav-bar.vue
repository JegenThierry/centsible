<script lang="ts" setup>

import AppButton from "~/components/_atoms/ui/app-button.vue";
import NavBreadcrumb from "~/components/_molecules/nav/nav-breadcrumb.vue";
import GlobalSearch from "~/components/_organisms/nav/global-search.vue";
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
  // Best effort: this runs on every authenticated page, and a transient failure here must not
  // become an unhandled rejection — a genuine 401 is handled by the axios interceptor.
  await userStore.fetchMyselfBestEffort();
  const stored = userStore.user?.locale;
  if (stored && stored !== locale.value) await setLocale(stored as 'en' | 'fr' | 'de');
});
</script>

<template>
  <div class="h-(--ui-header-height) shrink-0 flex items-center gap-2 px-4 border-b border-default">
    <ClientOnly>
      <template v-if="authStore.isAuthenticated">
        <AppButton
          :aria-label="t('nav.toggleSidebar')"
          :icon="currentPanelIcon"
          color="neutral"
          variant="ghost"
          @click="onToggleOpen()"/>
        <USeparator class="hidden md:flex h-6" orientation="vertical"/>
        <NavBreadcrumb class="hidden md:flex min-w-0"/>
      </template>
      <NuxtLink v-else
                aria-label="Centsible home"
                class="flex items-center transition-opacity hover:opacity-90"
                to="/">
        <BrandMark/>
      </NuxtLink>
    </ClientOnly>

    <div class="flex-1"/>

    <ClientOnly>
      <div class="flex items-center gap-2">
        <GlobalSearch v-if="authStore.isAuthenticated"/>
        <div class="flex items-center gap-1">
          <LanguagePicker/>
          <ThemePicker/>
          <template v-if="authStore.isAuthenticated">
            <NotificationBell/>
          </template>
          <AppButton v-else
                   class="ms-1"
                   color="primary"
                   icon="i-lucide-log-in"
                   to="/auth">
            {{ t('auth.login.signIn') }}
          </AppButton>
        </div>
      </div>
    </ClientOnly>
  </div>
</template>
