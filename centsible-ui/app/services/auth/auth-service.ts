import type {AuthRequest} from "~/models/auth/auth-request";
import type {AuthResponse} from "~/models/auth/auth-response";
import type {LoginResponse} from "~/models/auth/login-response";
import type {RegisterRequest} from "~/models/user/register-request";
import type {AxiosInstance} from "axios";
import {validateRequest} from "~/composables/use-api";

export function useAuthService(api: AxiosInstance) {
  async function login(authRequest: AuthRequest): Promise<LoginResponse> {
    // 200 = fully authenticated; 202 = password ok but a TOTP challenge is required.
    const response = await api.post<LoginResponse>('/auth/login', authRequest, {
      validateStatus: (status) => status === 200 || status === 202,
    });
    return response.data;
  }

  async function twoFactorChallenge(code: string): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/2fa/challenge', {code});
    return validateRequest<AuthResponse>(response);
  }

  async function register(registerRequest: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', registerRequest);
    return validateRequest<AuthResponse>(response);
  }

  async function verify(): Promise<boolean> {
    const response = await api.get<string>('/auth/verify');
    return response.status === 200;
  }

  async function confirm(token: string): Promise<boolean> {
    const response = await api.get<string>('/auth/confirm', {
      params: {token},
      validateStatus: (status) => status === 200 || status === 400,
    });
    return response.status === 200;
  }

  async function logout(): Promise<void> {
    await api.post('/auth/logout');
  }

  async function forgotPassword(username: string): Promise<void> {
    await api.post('/auth/forgot-password', {username});
  }

  async function resetPassword(token: string, password: string): Promise<boolean> {
    const response = await api.post('/auth/reset-password', {token, password}, {
      validateStatus: (status) => status === 204 || status === 400,
    });
    return response.status === 204;
  }

  return {
    login, twoFactorChallenge, register, verify, confirm, logout, forgotPassword, resetPassword
  }
}
