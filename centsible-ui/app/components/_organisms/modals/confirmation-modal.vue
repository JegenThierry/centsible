<script lang="ts" setup>
import {useToasts} from "~/services/toasts/toast-service";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import DeleteButton from "~/components/_molecules/buttons/delete-button.vue";

const props = defineProps<{
  entity: string;
  deleteCallback: () => Promise<void>;
}>();

const toasts = useToasts();
const {t} = useI18n();
const isOpen = defineModel<boolean>('open', {required: true});

const loading = ref(false);

async function onDelete() {
  loading.value = true;
  try {
    await props.deleteCallback();
    isOpen.value = false;
    toasts.success(t('common.confirmDelete.successTitle', {entity: props.entity}), t('common.confirmDelete.successBody', {entity: props.entity}))
  } catch (error) {
    toasts.error(t('common.confirmDelete.errorTitle', {entity: props.entity}), t('common.confirmDelete.errorBody', {entity: props.entity}))
    console.error(`Failed to delete ${props.entity}:`, error);
  } finally {
    loading.value = false;
  }
}

function onCancel() {
  isOpen.value = false;
}
</script>

<template>
  <UModal v-model:open="isOpen" :title="t('common.confirmDelete.title')">
    <template #body>
      <p>{{ t('common.confirmDelete.question', {entity}) }}</p>
      <p class="text-sm text-neutral-500 mt-2">
        {{ t('common.confirmDelete.irreversible') }}
      </p>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="onCancel"/>
        <DeleteButton :loading="loading" @click="onDelete"/>
      </div>
    </template>
  </UModal>
</template>

<style scoped>
</style>
