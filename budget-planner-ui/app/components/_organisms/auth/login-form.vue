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
      success(
        'Login successfully',
        'You have been logged in, redirecting to the dashboard.'
      );
    })
    .catch((err) => {
      console.error(err);
      error(
        'Login failed',
        'Username or password are incorrect.'
      );
    })
    .finally(() => loading.value = false);
}
</script>

<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput ref="usernameInput"
               v-model="state.username"
               autofocus
               label="Username"
               required
               type="text"/>

    <PasswordInput ref="passwordInput"
                   v-model="state.password"
                   label="Password"
                   required/>

    <UButton :loading="loading" class="ml-auto" type="submit">
      Submit
    </UButton>
  </UForm>
</template>
