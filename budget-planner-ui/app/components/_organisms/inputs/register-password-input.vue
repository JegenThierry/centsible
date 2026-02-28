<script setup lang="ts">

import PasswordInput from "~/components/_atoms/inputs/password-input.vue";

const password = defineModel<string>('password', {required: true});
const confirmPassword = defineModel<string>('confirm-password', {required: true});

function arePasswordsEqual(): boolean {
  return password.value === confirmPassword.value;
}
</script>

<template>
  <password-input v-model="password"
                  :additional-validation="arePasswordsEqual"/>

  <password-input v-model="confirmPassword"
                  :additional-validation="arePasswordsEqual"/>
</template>

<style scoped>

</style>