<script setup lang="ts">
import {useToasts} from "~/services/toasts/toast-service";
import CancelButton from "~/components/_molecules/buttons/cancel-button.vue";
import DeleteButton from "~/components/_molecules/buttons/delete-button.vue";

const props = defineProps<{
  entity: string;
  deleteCallback: () => Promise<void>;
}>();

const toasts = useToasts();
const isOpen = defineModel<boolean>('open', {required: true});

const loading = ref(false);

async function onDelete() {
  loading.value = true;
  try {
    await props.deleteCallback();
    isOpen.value = false;
    toasts.success(`Deleted ${props.entity}.`, `Your ${props.entity} has been deleted.`)
  } catch (error) {
    toasts.error(`Error ${props.entity}.`, `Your ${props.entity} could not be deleted, please try again.`)
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
  <UModal v-model:open="isOpen" title="Confirm Delete">
    <template #body>
      <p>Are you sure you want to delete this <strong>{{ entity }}</strong>?</p>
      <p class="text-sm text-neutral-500 mt-2">
        This action cannot be undone.
      </p>
    </template>

    <template #footer>
      <div class="flex justify-end gap-2">
        <CancelButton @click="onCancel" />
        <DeleteButton :loading="loading" @click="onDelete" />
      </div>
    </template>
  </UModal>
</template>

<style scoped>
</style>
