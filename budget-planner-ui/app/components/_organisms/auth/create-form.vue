<script setup lang="ts">
import {useApi} from "~/composables/use-api";
import {useAuthService} from "~/services/auth/auth-service";
import {useToasts} from "~/services/toasts/toast-service";
import type {AxiosInstance} from "axios";
import type {AuthResponse} from "~/models/auth/auth-response";
import BaseInput from "~/components/_atoms/inputs/base-input.vue";
import RegisterPasswordInput from "~/components/_organisms/inputs/register-password-input.vue";

import {useUserStore} from "~/stores/userStore";

const api = useApi();
const authStore = useAuthStore();
const userStore = useUserStore();
const {success, error} = useToasts();

const state = reactive({
  username: '',
  email: '',
  firstName: '',
  lastName: '',
  password: '',
  confirmPassword: '',
})
const usernameInput = ref<InstanceType<typeof BaseInput>>();
const emailInput = ref<InstanceType<typeof BaseInput>>();
const firstnameInput = ref<InstanceType<typeof BaseInput>>();
const lastnameInput = ref<InstanceType<typeof BaseInput>>();
const passwordsInput = ref<InstanceType<typeof RegisterPasswordInput>>();

const loading = ref<boolean>(false);

function onSubmit() {
  if (!validate()) {
    return;
  }
  loading.value = true;

  useAuthService(api as AxiosInstance)
      .register(state)
      .then((res: AuthResponse) => {
        authStore.setToken(res.token);
        userStore.fetchMyself();
        navigateTo('/accounts');
        success(
            'Registered successfully',
            'You have successfully registered, you will be redirected to the dashboard.'
        );
      })
      .catch((err) => {
        console.error(err);
        error(
            'Registration failed',
            'Please try again later.'
        );
      })
      .finally(() => loading.value = false);
}


function validate(): boolean {
  const inputs = [
      usernameInput.value,
      emailInput.value,
      firstnameInput.value,
      lastnameInput.value,
      passwordsInput.value,
  ]

  let valid = true;
  inputs.forEach((input) => {
    valid = input?.validate() ?? false
  });

  if(!valid) {
    error(
        'Validation failed.',
        'One or more validation errors occurred.'
    )
  }

  return valid;
}
</script>

<template>
  <UForm :state="state" class="space-y-6 pt-4 flex flex-col" @submit="onSubmit">
    <BaseInput v-model="state.username"
               autofocus
               ref="usernameInput"
               label="Username"
               placeholder="Username"
               type="text"
               required />

    <BaseInput v-model="state.email"
               ref="emailInput"
               label="E-Mail"
               placeholder="E-Mail"
               type="email"
               required />

    <BaseInput v-model="state.firstName"
               ref="firstnameInput"
               label="Firstname"
               placeholder="Firstname"
               type="text"
               required />

    <BaseInput v-model="state.lastName"
               ref="lastnameInput"
               label="Lastname"
               placeholder="Lastname"
               type="text"
               required />

    <RegisterPasswordInput v-model:password="state.password"
                           v-model:confirm-password="state.confirmPassword"
                           ref="passwordsInput" />

    <UButton class="ml-auto" :loading="loading" type="submit">
      Register
    </UButton>
  </UForm>
</template>
