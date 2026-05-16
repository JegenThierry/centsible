<script lang="ts" setup>
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import PasswordInput from "~/components/_atoms/inputs/password-input.vue";
import {useValidator} from "~/composables/use-validator";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success, error} = useToasts();
const {t} = useI18n();

const state = reactive({
  username: '',
  password: '',
})
const loading = ref<boolean>(false);

const usernameInput = ref<InstanceType<typeof BaseInput>>();
const passwordInput = ref<InstanceType<typeof PasswordInput>>();

function validate(): boolean {
  return useValidator().validateInputs([usernameInput, passwordInput]);
}

function onSubmit() {
  if (!validate()) {
    return;
  }
  loading.value = true;
  useAuthService(api)
    .login({username: state.username, password: state.password})
    .then(async () => {
      authStore.setAuthenticated(true);
      await userStore.fetchMyself();
      navigateTo('/accounts');
      success(t('auth.login.toastSuccessTitle'), t('auth.login.toastSuccessBody'));
    })
    .catch((err) => {
      console.error(err);
      error(t('auth.login.toastErrorTitle'), t('auth.login.toastErrorBody'));
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput ref="usernameInput"
               v-model="state.username"
               autofocus
               :label="t('auth.fields.username')"
               required
               type="text"/>

    <PasswordInput ref="passwordInput"
                   v-model="state.password"
                   :label="t('auth.fields.password')"
                   required/>

    <UButton :loading="loading" class="ml-auto" type="submit">
      {{ t('auth.login.submit') }}
    </UButton>
  </UForm>
</template>
