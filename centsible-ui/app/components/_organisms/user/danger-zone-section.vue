<script lang="ts" setup>
import AppButton from "~/components/_atoms/ui/app-button.vue";
import DeleteAccountModal from "~/components/_organisms/user/delete-account-modal.vue";

const {t} = useI18n();
const isModalOpen = ref(false);
</script>

<template>
  <div>
    <div class="flex items-center gap-2">
      <UIcon name="i-lucide-triangle-alert" class="text-error"/>
      <h3 class="text-lg font-semibold text-error">{{ t('profile.dangerZone.title') }}</h3>
    </div>
    <p class="text-sm text-muted mt-1">{{ t('profile.dangerZone.description') }}</p>

    <UAlert
      class="mt-4"
      :title="t('profile.dangerZone.delete.title')"
      :description="t('profile.dangerZone.delete.warning')"
      icon="i-lucide-trash-2"
      color="error"
      variant="subtle"/>

    <p class="text-sm text-muted mt-3">{{ t('profile.dangerZone.delete.description') }}</p>

    <AppButton class="mt-3" color="error" icon="i-lucide-trash-2" @click="isModalOpen = true">
      {{ t('profile.dangerZone.delete.button') }}
    </AppButton>

    <DeleteAccountModal v-model:open="isModalOpen"/>
  </div>
</template>
