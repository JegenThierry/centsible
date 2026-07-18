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
import PageHeader from "~/components/_molecules/page/page-header.vue";

const ConfirmationModal = defineAsyncComponent(() => import("~/components/_organisms/modals/confirmation-modal.vue"));

const store = useTagsStore();
const toasts = useToasts();
const {toastError} = useApiErrors();
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
      toasts.success(t('tags.toasts.createdTitle'), t('tags.toasts.createdBody', {name}));
    } else {
      await store.update(draftId.value, {name, color: draftColor.value});
      toasts.success(t('tags.toasts.updatedTitle'), t('tags.toasts.updatedBody', {name}));
    }
    draftOpen.value = false;
  } catch (error) {
    toastError(error, t('tags.toasts.saveFailedTitle'), t('tags.toasts.saveFailedBody'));
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
  <UContainer class="py-6 sm:py-10">
    <PageHeader
      :description="t('tags.page.description')"
      :title="t('tags.page.title')"
    >
      <template #actions>
        <AppButton class="w-full sm:w-auto justify-center"
                   icon="i-lucide-plus"
                   @click="openCreate">
          {{ t('tags.newTag') }}
        </AppButton>
      </template>
    </PageHeader>

    <div class="space-y-4">
      <div v-if="draftOpen" class="rounded-md border border-default p-3 sm:p-4 space-y-3">
        <AppInput v-model="draftName"
                  :label="t('tags.nameLabel')"
                  :placeholder="t('tags.namePlaceholder')"
                  class="w-full"
                  @keyup.enter="saveDraft"/>
        <ColorSelect v-model="draftColor" :label="t('tags.colorLabel')"/>
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
                     :description="t('tags.emptyDescription')"
                     icon="i-lucide-tags"
                     :title="t('tags.emptyTitle')">
        <template #actions>
          <AppButton class="w-full sm:w-auto justify-center" @click="openCreate">
            {{ t('tags.newTag') }}
          </AppButton>
        </template>
      </AppEmptyState>

      <div v-else class="flex flex-wrap gap-2">
        <div v-for="tag in store.tags"
             :key="tag.id"
             class="inline-flex items-center gap-2 rounded-full border border-default pl-3 pr-1 py-1">
          <span class="w-2.5 h-2.5 rounded-full shrink-0" :style="{backgroundColor: tag.color}"/>
          <span class="text-sm">{{ tag.name }}</span>
          <AppButton :aria-label="t('tags.editAria')"
                     color="neutral" icon="i-lucide-pencil" size="xs" variant="ghost"
                     @click="openEdit(tag)"/>
          <AppButton :aria-label="t('tags.deleteAria')"
                     color="neutral" icon="i-lucide-trash" size="xs" variant="ghost"
                     @click="openDelete(tag)"/>
        </div>
      </div>

      <ConfirmationModal v-if="isDeleteOpen && selected"
                         v-model:open="isDeleteOpen"
                         :entity="t('tags.entity')"
                         :delete-callback="deleteSelected"/>
    </div>
  </UContainer>
</template>
