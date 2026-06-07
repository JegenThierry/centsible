<script lang="ts" setup>
import adze from 'adze'
import QrcodeVue from 'qrcode.vue';
import {useApi} from "~/composables/use-api";
import {useTotpService} from "~/services/auth/totp-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import AppButton from "~/components/_atoms/ui/app-button.vue";
import {useValidator} from "~/composables/use-validator";
import type {TotpEnrollment} from "~/models/auth/totp";

type Step = 'loading' | 'disabled' | 'enrolling' | 'recovery' | 'enabled';

const {t} = useI18n();
const totpService = useTotpService(useApi());
const {success, error} = useToasts();

const step = ref<Step>('loading');
const busy = ref<boolean>(false);
const enrollment = ref<TotpEnrollment | null>(null);
const recoveryCodes = ref<string[]>([]);

const confirmState = reactive({code: ''});
const disableState = reactive({code: ''});
const confirmInput = ref<InstanceType<typeof BaseInput>>();
const disableInput = ref<InstanceType<typeof BaseInput>>();

onMounted(refreshStatus);

async function refreshStatus() {
  step.value = 'loading';
  try {
    const status = await totpService.status();
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

async function onConfirm() {
  if (busy.value) return;
  if (!useValidator().validateInputs([confirmInput])) return;
  busy.value = true;
  try {
    const result = await totpService.confirm(confirmState.code.trim());
    recoveryCodes.value = result.recoveryCodes;
    step.value = 'recovery';
    success(t('profile.twoFactor.toasts.enabledTitle'), t('profile.twoFactor.toasts.enabledBody'));
  } catch (err: any) {
    error(t('profile.twoFactor.toasts.invalidCodeTitle'), t('profile.twoFactor.toasts.invalidCodeBody'));
  } finally {
    busy.value = false;
  }
}

function onCancelEnroll() {
  enrollment.value = null;
  confirmState.code = '';
  step.value = 'disabled';
}

async function onDone() {
  recoveryCodes.value = [];
  enrollment.value = null;
  await refreshStatus();
}

async function onDisable() {
  if (busy.value) return;
  if (!useValidator().validateInputs([disableInput])) return;
  busy.value = true;
  try {
    await totpService.disable(disableState.code.trim());
    disableState.code = '';
    success(t('profile.twoFactor.toasts.disabledTitle'), t('profile.twoFactor.toasts.disabledBody'));
    await refreshStatus();
  } catch (err: any) {
    error(t('profile.twoFactor.toasts.invalidCodeTitle'), t('profile.twoFactor.toasts.invalidCodeBody'));
  } finally {
    busy.value = false;
  }
}

function copyRecoveryCodes() {
  const text = recoveryCodes.value.join('\n');
  navigator.clipboard?.writeText(text).then(
    () => success(t('profile.twoFactor.recovery.copiedTitle'), t('profile.twoFactor.recovery.copiedBody')),
    () => {/* clipboard unavailable — codes are still visible on screen */},
  );
}
</script>

<template>
  <div>
    <h3 class="text-lg font-semibold">{{ t('profile.twoFactor.title') }}</h3>
    <p class="text-sm text-muted mt-1">{{ t('profile.twoFactor.description') }}</p>

    <div v-if="step === 'loading'" class="mt-4 text-sm text-muted">
      {{ t('common.actions.loading') }}
    </div>

    <!-- Off: offer to enable -->
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

    <!-- Enrolling: show QR + secret + confirm code -->
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
      <UForm :state="confirmState" class="space-y-4" @submit="onConfirm">
        <BaseInput ref="confirmInput"
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

    <!-- Recovery codes (shown once) -->
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
      <div class="flex gap-3 justify-end">
        <AppButton color="neutral" variant="outline" icon="i-lucide-copy" @click="copyRecoveryCodes">
          {{ t('profile.twoFactor.recovery.copy') }}
        </AppButton>
        <AppButton icon="i-lucide-check" @click="onDone">
          {{ t('profile.twoFactor.recovery.done') }}
        </AppButton>
      </div>
    </div>

    <!-- On: offer to disable (requires a code) -->
    <div v-else-if="step === 'enabled'" class="mt-4 space-y-4">
      <UAlert
        :title="t('profile.twoFactor.status.onTitle')"
        :description="t('profile.twoFactor.status.onDescription')"
        icon="i-lucide-shield-check"
        color="success"
        variant="subtle"/>
      <UForm :state="disableState" class="space-y-4" @submit="onDisable">
        <BaseInput ref="disableInput"
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
