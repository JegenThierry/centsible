export function useValidator() {
  function validate(value: 'string' | 'number', type: 'email' | 'password' | 'text') {
  }

  /**
   * Validates all inputs in the given refs array.
   * Undefined inputs or inputs not supporting validate() are considered valid.
   * @param refs
   * @return true if all inputs are valid, false otherwise.
   */
  function validateInputs(refs: Ref<any>[]) {
    return refs.map(x => x.value?.validate() ?? true).every(x => x)
  }

  return {
    validate,
    validateInputs,
  }
}
