import type {Ref} from "vue";
import {useToasts} from "~/services/toasts/toast-service";
import {useApiErrors} from "~/composables/use-api-errors";

/**
 * The shared store mutation wrapper: flip a busy flag, run the mutation, refetch, toast success;
 * on failure toast the API error (with a per-store fallback body) and rethrow so callers still see
 * it. categoriesStore and rulesStore had byte-identical copies of this.
 *
 * The returned function keeps the original `(action, successTitle, successBody, errorTitle)` shape,
 * so call sites are unchanged; the per-store `refetch` and `fallbackBody` are bound here.
 */
export function useRunMutation(opts: {
  pending: Ref<boolean>;
  refetch: () => Promise<void>;
  /** Resolved at error time (a thunk so it tracks the current locale, as the inline version did). */
  fallbackBody: () => string;
  toasts?: ReturnType<typeof useToasts>;
  apiErrors?: ReturnType<typeof useApiErrors>;
}) {
  const toasts = opts.toasts ?? useToasts();
  const apiErrors = opts.apiErrors ?? useApiErrors();

  return async function runMutation(
    action: () => Promise<void>,
    successTitle: string,
    successBody: string,
    errorTitle: string,
  ): Promise<void> {
    opts.pending.value = true;
    try {
      await action();
      await opts.refetch();
      toasts.success(successTitle, successBody);
    } catch (error) {
      apiErrors.toastError(error, errorTitle, opts.fallbackBody());
      throw error;
    } finally {
      opts.pending.value = false;
    }
  };
}
