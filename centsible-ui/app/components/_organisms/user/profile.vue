<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import {useUserStore} from '~/stores/userStore';
import ProfileForm from '~/components/_organisms/user/profile-form.vue';
import ProfileAvatarSection from '~/components/_organisms/user/profile-avatar-section.vue';
import ProfileLanguageSection from '~/components/_organisms/user/profile-language-section.vue';
import ProfileThemeSection from '~/components/_organisms/user/profile-theme-section.vue';
import {useUserNotifications} from "~/components/_organisms/user/notifications";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import NotificationSettingsCard from "~/components/_organisms/user/notification-settings-card.vue";
import type {UserProfileForm} from "~/models/user/user-profile-form";

const userStore = useUserStore();
const {t} = useI18n();
const localeSwitcher = useLocaleSwitcher();

const {
  onAvatarUpdateSuccess,
  onAvatarUpdateError,
  onProfileUpdateSuccess,
  onProfileUpdateError,
  onValidationError
} = useUserNotifications();

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

    <UCard v-else-if="userStore.user" class="max-w-2xl mx-auto">
      <div class="space-y-12 mt-8">
        <ProfileAvatarSection :name="userStore.user.name"
                              :username="userStore.user.username"
                              :profile-picture="userStore.user.profilePicture"
                              @change="onAvatarChange"/>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-2">
          <ProfileForm :initial-values="initialFormValues"
                       :loading="userStore.pending"
                       @save="onSaveProfile"
                       @validation-failed="onValidationError"/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-6">
          <ProfileLanguageSection @change="onLanguageChange"/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-6">
          <NotificationSettingsCard/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-6">
          <ProfileThemeSection/>
        </div>
      </div>
    </UCard>
  </UContainer>
</template>
