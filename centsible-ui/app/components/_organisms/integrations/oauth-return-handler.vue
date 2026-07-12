<script lang="ts" setup>
import {useToasts} from "~/services/toasts/toast-service";
import {useProvidersStore} from "~/stores/providersStore";

const route = useRoute();
const router = useRouter();
const toasts = useToasts();
const providersStore = useProvidersStore();
const {t, te} = useI18n();

function translateErrorCode(code: string | undefined): string {
  if (!code) return t('integrations.oauthReturn.errorBody');
  const key = `integrations.oauthReturn.errors.${code}`;
  return te(key) ? t(key) : t('integrations.oauthReturn.errorBody');
}

onMounted(async () => {
  const status = (route.query.status as string | undefined) ?? 'error';
  const code = route.query.code as string | undefined;
  try {
    if (status === 'ok') {
      toasts.success(
        t('integrations.oauthReturn.successTitle'),
        t('integrations.oauthReturn.successBody'),
      );
    } else {
      toasts.error(
        t('integrations.oauthReturn.errorTitle'),
        translateErrorCode(code),
      );
    }
    await providersStore.refresh();
  } finally {
    router.replace('/integrations');
  }
});
</script>

<template>
  <UContainer class="py-10">
    <div class="flex flex-col items-center gap-4">
      <UIcon class="w-8 h-8 animate-spin" name="i-lucide-loader-2"/>
      <p class="text-sm text-neutral-500">{{ t('integrations.oauthReturn.redirecting') }}</p>
    </div>
  </UContainer>
</template>
