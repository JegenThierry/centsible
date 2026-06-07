export interface LoginResponse {
  twoFactorRequired: boolean;
  token: string;
}
