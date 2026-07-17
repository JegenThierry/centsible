import {ref, type Ref} from 'vue';
import {useApiErrors} from '~/composables/use-api-errors';
import {useToasts} from '~/services/toasts/toast-service';

interface ModalSubmitOptions<T> {
  /** The modal's `open` model; flipped to `false` on success to close it (bypassing the dirty guard). */
  open: Ref<boolean>;
  /** The async mutation to run. Its resolved value is threaded to [successBody] and [onSuccess]. */
  action: () => Promise<T>;
  successTitle: string;
  successBody: string | ((result: T) => string);
  errorTitle: string;
  errorBody: string;
  /** Post-success side effects (navigation, emits). Runs after the modal closes. */
  onSuccess?: (result: T) => void;
}

/**
 * Wraps a modal's async submit: toggles a [pending] flag, toasts success/failure, closes the modal on
 * success and runs [onSuccess]. `useToasts()`/`useApiErrors()` are resolved here at setup time (not
 * inside the awaited handler, where Nuxt's async context is gone).
 */
export function useModalSubmit<T>(options: ModalSubmitOptions<T>) {
  const toasts = useToasts();
  const {toastError} = useApiErrors();
  const pending = ref(false);

  async function submit(): Promise<void> {
    pending.value = true;
    try {
      const result = await options.action();
      const body = typeof options.successBody === 'function' ? options.successBody(result) : options.successBody;
      toasts.success(options.successTitle, body);
      options.open.value = false;
      options.onSuccess?.(result);
    } catch (error) {
      toastError(error, options.errorTitle, options.errorBody);
    } finally {
      pending.value = false;
    }
  }

  return {pending, submit};
}
