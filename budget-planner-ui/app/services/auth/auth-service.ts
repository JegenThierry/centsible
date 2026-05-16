import type {AuthRequest} from "~/models/auth/auth-request";
import type {AuthResponse} from "~/models/auth/auth-response";
import type {RegisterRequest} from "~/models/user/register-request";
import type {AxiosInstance} from "axios";

export function useAuthService(api: AxiosInstance) {
  async function login(authRequest: AuthRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/login', authRequest);

    if (response.status === 200 && response.data) {
      return response.data;
    }

    throw new Error(response.statusText);
  }

  async function register(registerRequest: RegisterRequest): Promise<AuthResponse> {
    const response = await api.post<AuthResponse>('/auth/register', registerRequest);

    if (response.status === 200 && response.data) {
      return response.data;
    }

    throw new Error(response.statusText);
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
    login, register, verify, confirm, logout, forgotPassword, resetPassword
  }
}
