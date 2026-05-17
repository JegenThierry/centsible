<script lang="ts" setup>
import PageHeader from "~/components/_molecules/page/page-header.vue";
import {useUserStore} from '~/stores/userStore';
import UserAvatar from '~/components/_atoms/user/user-avatar.vue';
import ProfileForm from '~/components/_organisms/user/profile-form.vue';
import {useUserNotifications} from "~/components/_organisms/user/notifications";
import LoadingAnimation from "~/components/_atoms/animations/loading-animation.vue";
import type {UserProfileForm} from "~/models/user/user-profile-form";

const userStore = useUserStore();
const {t, locale, locales} = useI18n();
const localeSwitcher = useLocaleSwitcher();

const {
  onAvatarUpdateSuccess,
  onAvatarUpdateError,
  onProfileUpdateSuccess,
  onProfileUpdateError,
  onValidationError
} = useUserNotifications();

const fileInput = ref<HTMLInputElement | null>(null);

const initialFormValues = computed<UserProfileForm | undefined>(() => {
  if (!userStore.user) return undefined;
  const {firstName, lastName, email} = userStore.user;
  return {firstName, lastName, email};
});

const languageOptions = computed(() =>
  (locales.value as Array<{code: string}>).map((l) => ({
    label: t(`profile.language.${l.code}`),
    value: l.code,
  })),
);

async function onSaveProfile(data: UserProfileForm) {
  try {
    await userStore.updateProfile(data);
    onProfileUpdateSuccess();
  } catch (error: any) {
    onProfileUpdateError(error.message);
  }
}

function onEditAvatar() {
  fileInput.value?.click();
}

async function onFileChange(event: Event) {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

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
        <div class="flex flex-col items-center gap-6">
          <UserAvatar :alt="userStore.user.name"
                      :src="userStore.user.profilePicture"
                      editable
                      size="3xl"
                      @edit="onEditAvatar"/>

          <div class="flex flex-col items-center gap-2">
            <div class="text-center">
              <h2 class="text-xl font-semibold">{{ userStore.user.name }}</h2>
              <p class="text-gray-500">@{{ userStore.user.username }}</p>
            </div>

            <UButton icon="i-lucide-camera"
                     :label="t('profile.avatar.change')"
                     variant="soft"
                     @click="onEditAvatar"/>
          </div>

          <input ref="fileInput"
                 accept="image/*"
                 class="hidden"
                 type="file"
                 @change="onFileChange"/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-2">
          <ProfileForm :initial-values="initialFormValues"
                       :loading="userStore.pending"
                       @save="onSaveProfile"
                       @validation-failed="onValidationError"/>
        </div>

        <div class="max-w-2xl mx-auto border-t border-gray-100 dark:border-gray-800 pt-6">
          <h3 class="text-lg font-semibold">{{ t('profile.language.title') }}</h3>
          <p class="text-sm text-muted mt-1">{{ t('profile.language.description') }}</p>
          <USelectMenu
            class="mt-4 w-full"
            :model-value="locale"
            :items="languageOptions"
            label-key="label"
            value-key="value"
            @update:model-value="onLanguageChange"
          />
        </div>
      </div>
    </UCard>
  </UContainer>
</template>
