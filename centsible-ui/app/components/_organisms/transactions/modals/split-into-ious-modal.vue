<script lang="ts" setup>
import type {Transaction} from "~/models/transactions/transaction";
import type {Currency} from "~/models/budget-account/currency";
import type {Contact} from "~/models/contact/contact";
import type {SplitToLoansRequest} from "~/models/loan/loan";
import {useContactsStore} from "~/stores/contactsStore";
import {useLoansStore} from "~/stores/loansStore";
import BalanceNumberFormat from "~/components/_atoms/labels/balance-number-format.vue";
import ModalFooterActions from "~/components/_molecules/modals/modal-footer-actions.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppInput from "~/components/_atoms/ui/app-input.vue";

const props = defineProps<{
  transaction: Transaction;
  // The table's display currency (transactions have no currency of their own).
  currency: Currency;
}>();
const isOpen = defineModel<boolean>('open', {required: true});
const emit = defineEmits<{ (e: 'created'): void }>();

const {t} = useI18n();
const contactsStore = useContactsStore();
const loansStore = useLoansStore();

interface ShareRow {
  mode: 'existing' | 'new';
  contact: Contact | undefined;
  newName: string;
  amount: number | null;
}

function blankRow(): ShareRow {
  return {mode: 'existing', contact: undefined, newName: '', amount: null};
}

// v-if in the parent remounts this on every open, so a fresh single row is the natural default.
const shares = ref<ShareRow[]>([blankRow()]);

onMounted(() => {
  if (contactsStore.contacts.length === 0) contactsStore.updateContacts();
});

const total = computed(() => props.transaction.amount ?? 0);
const owedSum = computed(() => shares.value.reduce((sum, s) => sum + (Number(s.amount) || 0), 0));
const yourShare = computed(() => Math.round((total.value - owedSum.value) * 100) / 100);
const overAllocated = computed(() => owedSum.value - total.value > 0.005);

function hasRecipient(s: ShareRow): boolean {
  return s.mode === 'existing' ? !!s.contact : s.newName.trim().length > 0;
}

const validShares = computed(() => shares.value.filter((s) => hasRecipient(s) && Number(s.amount) > 0));
const canSubmit = computed(() =>
  owedSum.value > 0
  && !overAllocated.value
  && shares.value.every((s) => hasRecipient(s) && Number(s.amount) > 0),
);

function addPerson() {
  shares.value.push(blankRow());
}

function removePerson(index: number) {
  shares.value.splice(index, 1);
  if (shares.value.length === 0) shares.value.push(blankRow());
}

/** Flip a row between choosing an existing contact and typing a brand-new person's name. */
function toggleMode(share: ShareRow) {
  share.mode = share.mode === 'existing' ? 'new' : 'existing';
  share.contact = undefined;
  share.newName = '';
}

function splitEvenly() {
  // Split the bill evenly across everyone at the table — the people listed plus you.
  const perPerson = Math.round((total.value / (shares.value.length + 1)) * 100) / 100;
  shares.value = shares.value.map((s) => ({...s, amount: perPerson}));
}

async function submit() {
  if (!canSubmit.value) return;
  const request: SplitToLoansRequest = {
    currency: props.currency,
    shares: validShares.value.map((s) => {
      const amount = Number(s.amount);
      if (s.mode === 'existing') return {contactId: s.contact!.id, amount};
      const parts = s.newName.trim().split(/\s+/);
      return {newContactFirstName: parts[0], newContactLastName: parts.slice(1).join(' ') || undefined, amount};
    }),
  };
  try {
    await loansStore.splitIntoIous(props.transaction.id, request);
    emit('created');
    isOpen.value = false;
  } catch {
    // Toast is surfaced by the store.
  }
}
</script>

<template>
  <UModal :open="isOpen"
          :title="t('transactions.splitIous.title')"
          :description="t('transactions.splitIous.description')"
          @update:open="isOpen = $event">
    <template #body>
      <div class="space-y-4">
        <div class="flex items-center justify-between gap-3 rounded-lg bg-elevated/50 px-3 py-2">
          <span class="text-sm text-muted truncate">{{ transaction.description }}</span>
          <span class="text-sm font-semibold tabular-nums text-highlighted shrink-0">
            <BalanceNumberFormat :balance="total" :currency="currency"/>
          </span>
        </div>

        <div>
          <p class="text-xs font-medium text-muted mb-2">{{ t('transactions.splitIous.owedBackTo') }}</p>
          <div class="space-y-2">
            <div v-for="(share, i) in shares" :key="i" class="flex items-center gap-2">
              <AppButton :icon="share.mode === 'existing' ? 'i-lucide-user-round-plus' : 'i-lucide-user-round-search'"
                       color="neutral"
                       variant="ghost"
                       size="xs"
                       :aria-label="share.mode === 'existing' ? t('transactions.splitIous.addNewPerson') : t('transactions.splitIous.pickExisting')"
                       @click="toggleMode(share)"/>
              <USelectMenu v-if="share.mode === 'existing'"
                           v-model="share.contact"
                           :items="contactsStore.contacts"
                           class="flex-1 min-w-0"
                           label-key="name"
                           :placeholder="t('transactions.selects.selectContact')"
                           searchable>
                <template #item-leading="{ item }">
                  <UAvatar v-if="item.picture" :src="item.picture" size="2xs"/>
                  <UIcon v-else class="w-4 h-4" name="i-lucide-user"/>
                </template>
              </USelectMenu>
              <AppInput v-else
                      v-model="share.newName"
                      class="flex-1 min-w-0"
                      :placeholder="t('transactions.splitIous.newNamePlaceholder')"/>
              <AppInput v-model.number="share.amount"
                      type="number"
                      step="0.01"
                      min="0"
                      class="w-24 shrink-0"
                      :placeholder="t('transactions.splitIous.amountPlaceholder')"/>
              <AppButton icon="i-lucide-x"
                       color="neutral"
                       variant="ghost"
                       size="xs"
                       :aria-label="t('transactions.splitIous.remove')"
                       @click="removePerson(i)"/>
            </div>
          </div>

          <div class="flex flex-wrap items-center gap-2 mt-3">
            <AppButton color="neutral" variant="soft" size="xs" icon="i-lucide-plus" @click="addPerson">
              {{ t('transactions.splitIous.addPerson') }}
            </AppButton>
            <AppButton color="neutral" variant="ghost" size="xs" icon="i-lucide-equal" @click="splitEvenly">
              {{ t('transactions.splitIous.splitEvenly') }}
            </AppButton>
          </div>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-x-3 gap-y-1 border-t border-default pt-3 text-sm">
          <span class="text-muted">
            {{ t('transactions.splitIous.yourShare') }}
            <span class="font-medium" :class="overAllocated ? 'text-error' : 'text-highlighted'">
              <BalanceNumberFormat :balance="yourShare" :currency="currency"/>
            </span>
          </span>
          <span class="tabular-nums" :class="overAllocated ? 'text-error' : 'text-muted'">
            <BalanceNumberFormat :balance="owedSum" :currency="currency"/> {{ t('transactions.splitIous.owedBack') }}
          </span>
        </div>
        <p v-if="overAllocated" class="text-xs text-error">{{ t('transactions.splitIous.overAllocated') }}</p>
      </div>
    </template>

    <template #footer>
      <ModalFooterActions :loading="loansStore.pending"
                          :disabled="!canSubmit"
                          :submit-label="t('transactions.splitIous.submit')"
                          @cancel="isOpen = false"
                          @submit="submit"/>
    </template>
  </UModal>
</template>
