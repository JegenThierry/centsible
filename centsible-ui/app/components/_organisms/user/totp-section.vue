<script lang="ts" setup>
import adze from 'adze'
import QrcodeVue from 'qrcode.vue';
import {useApi} from "~/composables/use-api";
import {useTotpService} from "~/services/auth/totp-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import AppCheckbox from "~/components/_atoms/ui/app-checkbox.vue";
import {z} from "zod";
import type {FormSubmitEvent} from "@nuxt/ui";
import {useClipboard} from "@vueuse/core";
import {triggerBrowserDownload} from "~/utils/blob-download";
import type {TotpEnrollment} from "~/models/auth/totp";

type Step = 'loading' | 'disabled' | 'enrolling' | 'recovery' | 'regenerating' | 'enabled';

const {t} = useI18n();
const totpService = useTotpService(useApi());
const {success, error} = useToasts();
const {copy: copyToClipboard, isSupported: clipboardSupported} = useClipboard({legacy: true});

const codeSchema = z.object({
  code: z.string().trim().min(1, t('common.validation.required', {field: t('profile.twoFactor.enroll.codeLabel')})),
});
type CodeSchema = z.output<typeof codeSchema>;

const step = ref<Step>('loading');
const busy = ref<boolean>(false);
const enrollment = ref<TotpEnrollment | null>(null);
const recoveryCodes = ref<string[]>([]);
const recoveryRemaining = ref(0);
const recoverySaved = ref(false);

const confirmState = reactive({code: ''});
const disableState = reactive({code: ''});
const regenState = reactive({code: ''});

onMounted(refreshStatus);

async function refreshStatus() {
  step.value = 'loading';
  try {
    const status = await totpService.status();
    recoveryRemaining.value = status.recoveryCodesRemaining ?? 0;
    step.value = status.enabled ? 'enabled' : 'disabled';
  } catch (err: any) {
    adze.ns('totp').error('Failed to load 2FA status', err);
    step.value = 'disabled';
  }
}

async function onStartEnroll() {
  if (busy.value) return;
  busy.value = true;
  try {
    enrollment.value = await totpService.enroll();
    confirmState.code = '';
    step.value = 'enrolling';
  } catch (err: any) {
    error(t('profile.twoFactor.toasts.enrollErrorTitle'), t('profile.twoFactor.toasts.enrollErrorBody'));
  } finally {
    busy.value = false;
  }
}

/** Busy-guarded runner for the code-submit actions: any failure surfaces the invalid-code toast. */
async function runWithCode(action: () => Promise<void>) {
  if (busy.value) return;
  busy.value = true;
  try {
    await action();
  } catch (err: any) {
    error(t('profile.twoFactor.toasts.invalidCodeTitle'), t('profile.twoFactor.toasts.invalidCodeBody'));
  } finally {
    busy.value = false;
  }
}

async function onConfirm(_e: FormSubmitEvent<CodeSchema>) {
  await runWithCode(async () => {
    const result = await totpService.confirm(confirmState.code.trim());
    recoveryCodes.value = result.recoveryCodes;
    recoverySaved.value = false;
    step.value = 'recovery';
    success(t('profile.twoFactor.toasts.enabledTitle'), t('profile.twoFactor.toasts.enabledBody'));
  });
}

function onCancelEnroll() {
  totpService.cancelEnroll().catch((err) => adze.ns('totp').warn('Failed to cancel enrollment', err));
  enrollment.value = null;
  confirmState.code = '';
  step.value = 'disabled';
}

async function onDone() {
  recoveryCodes.value = [];
  enrollment.value = null;
  await refreshStatus();
}

async function onDisable(_e: FormSubmitEvent<CodeSchema>) {
  await runWithCode(async () => {
    await totpService.disable(disableState.code.trim());
    disableState.code = '';
    success(t('profile.twoFactor.toasts.disabledTitle'), t('profile.twoFactor.toasts.disabledBody'));
    await refreshStatus();
  });
}

function onStartRegenerate() {
  regenState.code = '';
  step.value = 'regenerating';
}

function onCancelRegenerate() {
  regenState.code = '';
  step.value = 'enabled';
}

async function onRegenerate(_e: FormSubmitEvent<CodeSchema>) {
  await runWithCode(async () => {
    const result = await totpService.regenerateRecoveryCodes(regenState.code.trim());
    recoveryCodes.value = result.recoveryCodes;
    recoverySaved.value = false;
    step.value = 'recovery';
    success(t('profile.twoFactor.recovery.regeneratedTitle'), t('profile.twoFactor.recovery.regeneratedBody'));
  });
}

async function copyRecoveryCodes() {
  if (!clipboardSupported.value) {
    error(t('profile.twoFactor.recovery.copyFailedTitle'), t('profile.twoFactor.recovery.copyFailedBody'));
    return;
  }
  try {
    await copyToClipboard(recoveryCodes.value.join('\n'));
    success(t('profile.twoFactor.recovery.copiedTitle'), t('profile.twoFactor.recovery.copiedBody'));
  } catch {
    error(t('profile.twoFactor.recovery.copyFailedTitle'), t('profile.twoFactor.recovery.copyFailedBody'));
  }
}

function downloadRecoveryCodes() {
  const blob = new Blob([recoveryCodes.value.join('\n') + '\n'], {type: 'text/plain'});
  triggerBrowserDownload(blob, 'centsible-recovery-codes.txt');
}
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.twoFactor.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.twoFactor.description') }}</p>

    <div v-if="step === 'loading'" class="mt-4 text-sm text-muted">
      {{ t('common.actions.loading') }}
    </div>

    <div v-else-if="step === 'disabled'" class="mt-4">
      <UAlert
        :title="t('profile.twoFactor.status.offTitle')"
        :description="t('profile.twoFactor.status.offDescription')"
        icon="i-lucide-shield-off"
        color="neutral"
        variant="subtle"/>
      <AppButton class="mt-4" icon="i-lucide-shield-plus" :loading="busy" @click="onStartEnroll">
        {{ t('profile.twoFactor.enable') }}
      </AppButton>
    </div>

    <div v-else-if="step === 'enrolling' && enrollment" class="mt-4 space-y-4">
      <p class="text-sm">{{ t('profile.twoFactor.enroll.scanInstruction') }}</p>
      <div class="flex justify-center bg-white p-4 rounded-lg w-fit mx-auto">
        <QrcodeVue :value="enrollment.otpauthUri" :size="200" level="M"/>
      </div>
      <div>
        <p class="text-sm text-muted">{{ t('profile.twoFactor.enroll.manualInstruction') }}</p>
        <code class="block mt-1 font-mono text-sm break-all select-all bg-elevated rounded px-2 py-1">
          {{ enrollment.secret }}
        </code>
      </div>
      <UForm :schema="codeSchema" :state="confirmState" class="space-y-4" @submit="onConfirm">
        <BaseInput name="code"
                   v-model="confirmState.code"
                   :disabled="busy"
                   :label="t('profile.twoFactor.enroll.codeLabel')"
                   :hint="t('profile.twoFactor.enroll.codeHint')"
                   required
                   type="text"/>
        <div class="flex gap-3 justify-end">
          <AppButton color="neutral" variant="outline" :disabled="busy" @click="onCancelEnroll">
            {{ t('common.actions.cancel') }}
          </AppButton>
          <AppButton :loading="busy" type="submit">
            {{ t('profile.twoFactor.enroll.verify') }}
          </AppButton>
        </div>
      </UForm>
    </div>

    <div v-else-if="step === 'recovery'" class="mt-4 space-y-4">
      <UAlert
        :title="t('profile.twoFactor.recovery.title')"
        :description="t('profile.twoFactor.recovery.description')"
        icon="i-lucide-key-round"
        color="warning"
        variant="subtle"/>
      <ul class="grid grid-cols-2 gap-2 font-mono text-sm">
        <li v-for="code in recoveryCodes" :key="code" class="bg-elevated rounded px-2 py-1 select-all text-center">
          {{ code }}
        </li>
      </ul>
      <div class="flex gap-3">
        <AppButton color="neutral" variant="outline" icon="i-lucide-copy" @click="copyRecoveryCodes">
          {{ t('profile.twoFactor.recovery.copy') }}
        </AppButton>
        <AppButton color="neutral" variant="outline" icon="i-lucide-download" @click="downloadRecoveryCodes">
          {{ t('profile.twoFactor.recovery.download') }}
        </AppButton>
      </div>
      <AppCheckbox v-model="recoverySaved" :label="t('profile.twoFactor.recovery.savedConfirm')"/>
      <div class="flex justify-end">
        <AppButton icon="i-lucide-check" :disabled="!recoverySaved" @click="onDone">
          {{ t('profile.twoFactor.recovery.done') }}
        </AppButton>
      </div>
    </div>

    <div v-else-if="step === 'regenerating'" class="mt-4 space-y-4">
      <UAlert
        :title="t('profile.twoFactor.recovery.regenerateTitle')"
        :description="t('profile.twoFactor.recovery.regenerateDescription')"
        icon="i-lucide-refresh-cw"
        color="warning"
        variant="subtle"/>
      <UForm :schema="codeSchema" :state="regenState" class="space-y-4" @submit="onRegenerate">
        <BaseInput name="code"
                   v-model="regenState.code"
                   :disabled="busy"
                   :label="t('profile.twoFactor.disable.codeLabel')"
                   :hint="t('profile.twoFactor.disable.codeHint')"
                   required
                   type="text"/>
        <div class="flex gap-3 justify-end">
          <AppButton color="neutral" variant="outline" :disabled="busy" @click="onCancelRegenerate">
            {{ t('common.actions.cancel') }}
          </AppButton>
          <AppButton :loading="busy" type="submit">
            {{ t('profile.twoFactor.recovery.regenerateSubmit') }}
          </AppButton>
        </div>
      </UForm>
    </div>

    <div v-else-if="step === 'enabled'" class="mt-4 space-y-4">
      <UAlert
        :title="t('profile.twoFactor.status.onTitle')"
        :description="t('profile.twoFactor.status.onDescription')"
        icon="i-lucide-shield-check"
        color="success"
        variant="subtle"/>

      <div class="flex items-center justify-between gap-3 rounded-md border border-default p-3">
        <div>
          <p class="text-sm font-medium">{{ t('profile.twoFactor.recovery.remainingTitle') }}</p>
          <p class="text-xs"
             :class="recoveryRemaining <= 3 ? 'text-warning' : 'text-muted'">
            {{ t('profile.twoFactor.recovery.remainingCount', {count: recoveryRemaining}, recoveryRemaining) }}
          </p>
        </div>
        <AppButton color="neutral" variant="outline" size="sm" icon="i-lucide-refresh-cw" @click="onStartRegenerate">
          {{ t('profile.twoFactor.recovery.regenerate') }}
        </AppButton>
      </div>

      <UForm :schema="codeSchema" :state="disableState" class="space-y-4" @submit="onDisable">
        <BaseInput name="code"
                   v-model="disableState.code"
                   :disabled="busy"
                   :label="t('profile.twoFactor.disable.codeLabel')"
                   :hint="t('profile.twoFactor.disable.codeHint')"
                   required
                   type="text"/>
        <AppButton class="ml-auto" color="error" icon="i-lucide-shield-off" :loading="busy" type="submit">
          {{ t('profile.twoFactor.disable.submit') }}
        </AppButton>
      </UForm>
    </div>
  </div>
</template>
