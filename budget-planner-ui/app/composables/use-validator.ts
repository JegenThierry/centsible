import type {Ref} from 'vue';

export interface Validatable {
  validate: () => boolean;
}

export function useValidator() {
  function validateInputs(refs: Ref<Validatable | undefined>[]) {
    return refs.reduce((valid, x) => (x.value?.validate?.() ?? true) && valid, true);
  }

  return {
    validateInputs,
  }
}
