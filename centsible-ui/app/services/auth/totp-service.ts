import type {AxiosInstance} from "axios";
import type {TotpEnrollment, TotpStatus, RecoveryCodes} from "~/models/auth/totp";
import {assertStatus, validateRequest} from "~/composables/use-api";

export function useTotpService(api: AxiosInstance) {
  async function status(): Promise<TotpStatus> {
    const response = await api.get<TotpStatus>('/auth/2fa/status');
    return validateRequest<TotpStatus>(response);
  }

  async function enroll(): Promise<TotpEnrollment> {
    const response = await api.post<TotpEnrollment>('/auth/2fa/enroll');
    return validateRequest<TotpEnrollment>(response);
  }

  async function confirm(code: string): Promise<RecoveryCodes> {
    const response = await api.post<RecoveryCodes>('/auth/2fa/confirm', {code});
    return validateRequest<RecoveryCodes>(response);
  }

  async function disable(code: string): Promise<void> {
    const response = await api.post('/auth/2fa/disable', {code}, {
      validateStatus: (s) => s === 204,
    });
    assertStatus(response, [204]);
  }

  // Discards the pending secret left server-side when the user backs out of enrollment.
  async function cancelEnroll(): Promise<void> {
    const response = await api.post('/auth/2fa/enroll/cancel', {}, {validateStatus: (s) => s === 204});
    assertStatus(response, [204]);
  }

  // Verifies a current code, replaces every recovery code, and returns the new set (shown once).
  async function regenerateRecoveryCodes(code: string): Promise<RecoveryCodes> {
    const response = await api.post<RecoveryCodes>('/auth/2fa/recovery-codes/regenerate', {code});
    return validateRequest<RecoveryCodes>(response);
  }

  return {status, enroll, confirm, disable, cancelEnroll, regenerateRecoveryCodes};
}
