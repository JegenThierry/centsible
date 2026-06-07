<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import {useUserStore} from '~/stores/userStore';
import ProfileForm from '~/components/_organisms/user/profile-form.vue';
import ProfileAvatarSection from '~/components/_organisms/user/profile-avatar-section.vue';
import ProfileLanguageSection from '~/components/_organisms/user/profile-language-section.vue';
import ProfileThemeSection from '~/components/_organisms/user/profile-theme-section.vue';
import TotpSection from '~/components/_organisms/user/totp-section.vue';
import DefaultCurrencySection from '~/components/_organisms/user/default-currency-section.vue';
import DataExportSection from '~/components/_organisms/user/data-export-section.vue';
import PasswordChangeSection from '~/components/_organisms/user/password-change-section.vue';
import SessionsSection from '~/components/_organisms/user/sessions-section.vue';
import DangerZoneSection from '~/components/_organisms/user/danger-zone-section.vue';
import {useUserNotifications} from "~/components/_organisms/user/notifications";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import NotificationSettingsCard from "~/components/_organisms/user/notification-settings-card.vue";
import type {UserProfileForm} from "~/models/user/user-profile-form";

const userStore = useUserStore();
const {t} = useI18n();
const localeSwitcher = useLocaleSwitcher();
const route = useRoute();
const router = useRouter();

const {
  onAvatarUpdateSuccess,
  onAvatarUpdateError,
  onProfileUpdateSuccess,
  onProfileUpdateError,
  onValidationError
} = useUserNotifications();

const TAB_VALUES = ['profile', 'security', 'notifications', 'appearance'] as const;
type TabValue = typeof TAB_VALUES[number];

const tabs = computed(() => [
  {label: t('profile.tabs.profile'), icon: 'i-lucide-user', slot: 'profile', value: 'profile'},
  {label: t('profile.tabs.security'), icon: 'i-lucide-shield', slot: 'security', value: 'security'},
  {label: t('profile.tabs.notifications'), icon: 'i-lucide-bell', slot: 'notifications', value: 'notifications'},
  {label: t('profile.tabs.appearance'), icon: 'i-lucide-palette', slot: 'appearance', value: 'appearance'},
]);

function isTab(value: unknown): value is TabValue {
  return typeof value === 'string' && (TAB_VALUES as readonly string[]).includes(value);
}

// Deep-link the active tab via ?tab= so other screens can link straight to e.g. Security.
const activeTab = ref<TabValue>(isTab(route.query.tab) ? route.query.tab : 'profile');

watch(activeTab, (tab) => {
  router.replace({query: {...route.query, tab}});
});

const initialFormValues = computed<UserProfileForm | undefined>(() => {
  if (!userStore.user) return undefined;
  const {firstName, lastName, email} = userStore.user;
  return {firstName, lastName, email};
});

async function onSaveProfile(data: UserProfileForm) {
  try {
    await userStore.updateProfile(data);
    onProfileUpdateSuccess();
  } catch (error: any) {
    onProfileUpdateError(error.message);
  }
}

async function onAvatarChange(file: File) {
  try {
    await userStore.updateProfilePicture(file);
    onAvatarUpdateSuccess();
  } catch (error: any) {
    onAvatarUpdateError(error.message);
  }
}

async function onLanguageChange(code: string) {
  await localeSwitcher.apply(code);
}
</script>

<template>
  <UContainer class="py-6 sm:py-10">
    <PageHeader :description="t('profile.page.description')"
                :title="t('profile.page.title')"/>

    <div v-if="userStore.pending && !userStore.user" class="flex justify-center py-8">
      <LoadingAnimation/>
    </div>

    <div v-else-if="userStore.user" class="max-w-2xl mx-auto">
      <UTabs v-model="activeTab" :items="tabs" class="w-full">
        <template #profile>
          <UCard class="mt-4">
            <div class="space-y-10">
              <ProfileAvatarSection :name="userStore.user.name"
                                    :username="userStore.user.username"
                                    :profile-picture="userStore.user.profilePicture"
                                    @change="onAvatarChange"/>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <ProfileForm :initial-values="initialFormValues"
                             :loading="userStore.pending"
                             @save="onSaveProfile"
                             @validation-failed="onValidationError"/>
              </div>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <ProfileLanguageSection @change="onLanguageChange"/>
              </div>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <DefaultCurrencySection/>
              </div>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <DataExportSection/>
              </div>
            </div>
          </UCard>
        </template>

        <template #security>
          <UCard class="mt-4">
            <div class="space-y-10">
              <PasswordChangeSection/>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <TotpSection/>
              </div>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <SessionsSection/>
              </div>

              <div class="border-t border-gray-100 dark:border-gray-800 pt-6">
                <DangerZoneSection/>
              </div>
            </div>
          </UCard>
        </template>

        <template #notifications>
          <UCard class="mt-4">
            <NotificationSettingsCard/>
          </UCard>
        </template>

        <template #appearance>
          <UCard class="mt-4">
            <ProfileThemeSection/>
          </UCard>
        </template>
      </UTabs>
    </div>
  </UContainer>
</template>
