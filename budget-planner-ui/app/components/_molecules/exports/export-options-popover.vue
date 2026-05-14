<script lang="ts" setup>
import {computed, ref} from 'vue';
import {useAuthStore} from "~/stores/authStore";
import type {CreateExportRequest, ExportRequestParams, ExportType} from "~/models/export/export-job";

const props = defineProps<{
  type: ExportType;
  defaultTitle: string;
  paramsBuilder: () => ExportRequestParams;
  pending?: boolean;
}>();

const emit = defineEmits<{
  (e: 'submit', payload: CreateExportRequest): void;
}>();

const authStore = useAuthStore();
const open = ref(false);
const sendEmail = ref(false);
const recipient = ref('');
const title = ref('');

const userEmail = computed(() => authStore.user?.email ?? '');

function reset() {
  sendEmail.value = false;
  recipient.value = '';
  title.value = props.defaultTitle;
}

function onOpen(value: boolean) {
  open.value = value;
  if (value) reset();
}

function submit() {
  const payload: CreateExportRequest = {
    type: props.type,
    title: title.value || props.defaultTitle,
    params: props.paramsBuilder(),
    postProcessing: sendEmail.value
      ? [{type: 'SEND_EMAIL', config: recipient.value ? {recipient: recipient.value} : {}}]
      : [],
  };
  emit('submit', payload);
  open.value = false;
}
</script>

<template>
  <UPopover :open="open" @update:open="onOpen">
    <slot/>

    <template #content>
      <div class="p-4 space-y-3 w-80">
        <div>
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">Title</label>
          <UInput v-model="title" :placeholder="defaultTitle" class="w-full mt-1"/>
        </div>

        <div class="border-t border-neutral-200 dark:border-neutral-800 pt-3">
          <UCheckbox v-model="sendEmail" label="Email me a copy when ready"/>
        </div>

        <div v-if="sendEmail">
          <label class="text-xs font-medium text-neutral-600 dark:text-neutral-300">Recipient</label>
          <UInput v-model="recipient" :placeholder="userEmail" class="w-full mt-1" type="email"/>
          <p class="text-xs text-neutral-500 mt-1">Leave blank to use your account email.</p>
        </div>

        <div class="flex justify-end gap-2 pt-2">
          <UButton color="neutral" variant="ghost" @click="open = false">Cancel</UButton>
          <UButton :loading="pending" color="primary" @click="submit">Start export</UButton>
        </div>
      </div>
    </template>
  </UPopover>
</template>
