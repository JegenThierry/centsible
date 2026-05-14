import type {Ref} from 'vue';

export interface Validatable {
  validate: () => boolean;
}

export function useValidator() {
  /**
   * Validates all inputs in the given refs array.
   * Undefined inputs or inputs not supporting validate() are considered valid.
   */
  function validateInputs(refs: Ref<Validatable | undefined>[]) {
    return refs.every(x => x.value?.validate?.() ?? true);
  }

  return {
    validateInputs,
  }
}
