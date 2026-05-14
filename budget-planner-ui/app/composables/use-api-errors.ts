import axios from 'axios';
import {useToasts} from '~/services/toasts/toast-service';

interface BackendErrorResponse {
  message?: string;
  details?: string;
  fieldErrors?: Record<string, string>;
}

export function useApiErrors() {
  function extractMessage(err: unknown, fallback: string): string {
    if (axios.isAxiosError<BackendErrorResponse>(err) && err.response?.data) {
      const data = err.response.data;
      const baseMessage = data.message ?? fallback;
      const fieldErrors = data.fieldErrors;
      if (fieldErrors && Object.keys(fieldErrors).length > 0) {
        return `${baseMessage} ${Object.values(fieldErrors).join(' ')}`.trim();
      }
      return baseMessage;
    }
    return fallback;
  }

  function toastError(err: unknown, title: string, fallback: string): void {
    console.error(err);
    useToasts().error(title, extractMessage(err, fallback));
  }

  return {extractMessage, toastError};
}
