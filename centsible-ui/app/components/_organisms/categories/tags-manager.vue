<script lang="ts" setup>
import {useTagsStore} from "~/stores/tagsStore";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";
import type {Tag} from "~/models/tag/tag";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";
import ColorSelect from "~/components/_atoms/inputs/color-select.vue";
import CardSkeleton from "~/components/_molecules/skeletons/card-skeleton.vue";
import AppEmptyState from "~/components/_molecules/feedback/app-empty-state.vue";

const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const store = useTagsStore();
const toasts = useToasts();
const {t} = useI18n();

const draftOpen = ref(false);
const draftId = ref<number | null>(null);
const draftName = ref('');
const draftColor = ref('#3b82f6');
const saving = ref(false);

const isDeleteOpen = ref(false);
const selected = ref<Tag | null>(null);

function openCreate() {
  draftId.value = null;
  draftName.value = '';
  draftColor.value = '#3b82f6';
  draftOpen.value = true;
}

function openEdit(tag: Tag) {
  draftId.value = tag.id;
  draftName.value = tag.name;
  draftColor.value = tag.color;
  draftOpen.value = true;
}

async function saveDraft() {
  const name = draftName.value.trim();
  if (!name) return;
  saving.value = true;
  try {
    if (draftId.value == null) {
      await store.create({name, color: draftColor.value});
      toasts.success(t('categories.tags.toastCreatedTitle'), t('categories.tags.toastCreatedBody', {name}));
    } else {
      await store.update(draftId.value, {name, color: draftColor.value});
      toasts.success(t('categories.tags.toastUpdatedTitle'), t('categories.tags.toastUpdatedBody', {name}));
    }
    draftOpen.value = false;
  } catch (error) {
    useApiErrors().toastError(error, t('categories.tags.toastSaveFailedTitle'), t('categories.tags.toastSaveFailedBody'));
  } finally {
    saving.value = false;
  }
}

function openDelete(tag: Tag) {
  selected.value = tag;
  isDeleteOpen.value = true;
}

async function deleteSelected(): Promise<void> {
  if (selected.value) await store.remove(selected.value.id);
}

onMounted(() => store.fetchAll());
</script>

<template>
  <div class="space-y-4">
    <div class="flex justify-end">
      <AppButton icon="i-lucide-plus" @click="openCreate">{{ t('categories.tags.newTag') }}</AppButton>
    </div>

    <div v-if="draftOpen" class="rounded-md border border-default p-3 sm:p-4 space-y-3">
      <AppInput v-model="draftName"
                :label="t('categories.tags.nameLabel')"
                :placeholder="t('categories.tags.namePlaceholder')"
                class="w-full"
                @keyup.enter="saveDraft"/>
      <ColorSelect v-model="draftColor" :label="t('categories.tags.colorLabel')"/>
      <div class="flex justify-end gap-2">
        <AppButton color="neutral" variant="ghost" @click="draftOpen = false">
          {{ t('common.actions.cancel') }}
        </AppButton>
        <AppButton :disabled="!draftName.trim()" :loading="saving" @click="saveDraft">
          {{ t('common.actions.save') }}
        </AppButton>
      </div>
    </div>

    <div v-if="store.loading && store.tags.length === 0" class="space-y-3">
      <CardSkeleton v-for="i in 3" :key="i"/>
    </div>

    <AppEmptyState v-else-if="store.tags.length === 0 && !draftOpen"
                   :description="t('categories.tags.emptyDescription')"
                   icon="i-lucide-tags"
                   :title="t('categories.tags.emptyTitle')">
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center" @click="openCreate">
          {{ t('categories.tags.newTag') }}
        </AppButton>
      </template>
    </AppEmptyState>

    <div v-else class="flex flex-wrap gap-2">
      <div v-for="tag in store.tags"
           :key="tag.id"
           class="inline-flex items-center gap-2 rounded-full border border-default pl-3 pr-1 py-1">
        <span class="w-2.5 h-2.5 rounded-full shrink-0" :style="{backgroundColor: tag.color}"/>
        <span class="text-sm">{{ tag.name }}</span>
        <AppButton :aria-label="t('categories.tags.editAria')"
                   color="neutral" icon="i-lucide-pencil" size="xs" variant="ghost"
                   @click="openEdit(tag)"/>
        <AppButton :aria-label="t('categories.tags.deleteAria')"
                   color="neutral" icon="i-lucide-trash" size="xs" variant="ghost"
                   @click="openDelete(tag)"/>
      </div>
    </div>

    <ConfirmationModal v-if="isDeleteOpen && selected"
                       v-model:open="isDeleteOpen"
                       :entity="t('categories.tags.entity')"
                       :delete-callback="deleteSelected"/>
  </div>
</template>
