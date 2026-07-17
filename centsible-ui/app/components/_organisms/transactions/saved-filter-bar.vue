<script lang="ts" setup>
import {useSavedFiltersStore} from "~/stores/savedFiltersStore";
import type {TransactionFilters} from "~/models/transactions/transaction-filters";
import {useToasts} from "~/services/toasts/toast-service";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";

const props = defineProps<{
  filters: TransactionFilters;
  canSave: boolean;
}>();

const emit = defineEmits<{
  (e: 'apply', filters: TransactionFilters): void;
}>();

const store = useSavedFiltersStore();
const toasts = useToasts();
const {t} = useI18n();

onMounted(() => store.ensureLoaded());

const saveOpen = ref(false);
const newName = ref('');
const saving = ref(false);

/** Canonical string form of a filter set, so a saved view can be matched against the active one. */
function fingerprint(f: TransactionFilters): string {
  return JSON.stringify({
    search: f.search || '',
    categoryIds: [...(f.categoryIds ?? [])].sort((a, b) => a - b),
    tagIds: [...(f.tagIds ?? [])].sort((a, b) => a - b),
    fromDate: f.fromDate || '',
    toDate: f.toDate || '',
    type: f.type || '',
    amountMin: f.amountMin ?? null,
    amountMax: f.amountMax ?? null,
    sort: f.sort || 'DATE_DESC',
  });
}

const activeFingerprint = computed(() => fingerprint(props.filters));

function isActive(f: TransactionFilters): boolean {
  return fingerprint(f) === activeFingerprint.value;
}

function apply(filters: TransactionFilters) {
  emit('apply', {...filters});
}

function openSave() {
  newName.value = '';
  saveOpen.value = true;
}

async function confirmSave() {
  const name = newName.value.trim();
  if (!name) return;
  saving.value = true;
  try {
    await store.create({name, filters: props.filters});
    toasts.success(t('transactions.savedFilters.savedTitle'), t('transactions.savedFilters.savedBody', {name}));
    saveOpen.value = false;
  } catch (e) {
    toasts.error(t('transactions.savedFilters.errorTitle'), t('transactions.savedFilters.errorBody'));
  } finally {
    saving.value = false;
  }
}

async function remove(id: number, name: string) {
  try {
    await store.remove(id);
    toasts.success(t('transactions.savedFilters.deletedTitle'), t('transactions.savedFilters.deletedBody', {name}));
  } catch (e) {
    toasts.error(t('transactions.savedFilters.errorTitle'), t('transactions.savedFilters.errorBody'));
  }
}
</script>

<template>
  <div v-if="store.savedFilters.length > 0 || canSave" class="flex flex-wrap items-center gap-2">
    <UButton v-for="f in store.savedFilters"
             :key="f.id"
             :color="isActive(f.filters) ? 'primary' : 'neutral'"
             :variant="isActive(f.filters) ? 'solid' : 'soft'"
             size="xs"
             @click="apply(f.filters)">
      {{ f.name }}
      <template #trailing>
        <UIcon :aria-label="t('transactions.savedFilters.delete')"
               class="w-3 h-3 opacity-50 hover:opacity-100"
               name="i-lucide-x"
               @click.stop="remove(f.id, f.name)"/>
      </template>
    </UButton>

    <UButton v-if="canSave"
             color="neutral"
             icon="i-lucide-bookmark-plus"
             size="xs"
             variant="ghost"
             @click="openSave">
      {{ t('transactions.savedFilters.save') }}
    </UButton>

    <UModal v-model:open="saveOpen" :title="t('transactions.savedFilters.saveTitle')">
      <template #body>
        <UFormField :label="t('transactions.savedFilters.nameLabel')">
          <UInput v-model="newName"
                  :placeholder="t('transactions.savedFilters.namePlaceholder')"
                  autofocus
                  class="w-full"
                  maxlength="100"
                  @keyup.enter="confirmSave"/>
        </UFormField>
      </template>
      <template #footer>
        <ModalFooterActions :disabled="!newName.trim()"
                            :loading="saving"
                            :submit-label="t('common.actions.save')"
                            @cancel="saveOpen = false"
                            @submit="confirmSave"/>
      </template>
    </UModal>
  </div>
</template>
