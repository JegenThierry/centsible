import axios from 'axios';
import adze from 'adze'
import {useToasts} from '~/services/toasts/toast-service';

interface BackendErrorResponse {
  message?: string;
  details?: string;
  fieldErrors?: Record<string, string>;
}

/**
 * Captures `useToasts()` eagerly at setup time. `toastError` is normally invoked inside a `.catch`
 * after an `await`, by which point Nuxt's async context is gone and a lazy `useToasts()` call
 * would warn ("composable called outside setup").
 */
export function useApiErrors() {
  const toasts = useToasts();

  function extractMessage(err: unknown, fallback: string): string {
    if (!axios.isAxiosError<BackendErrorResponse>(err) || !err.response?.data) return fallback;

    const {message, fieldErrors} = err.response.data;
    const base = message ?? fallback;
    if (!fieldErrors || Object.keys(fieldErrors).length === 0) return base;
    return `${base} ${Object.values(fieldErrors).join(' ')}`.trim();
  }

  function toastError(err: unknown, title: string, fallback: string): void {
    adze.ns('api').error(title, err);
    toasts.error(title, extractMessage(err, fallback));
  }

  return {extractMessage, toastError};
}
