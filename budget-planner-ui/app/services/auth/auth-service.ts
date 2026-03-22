import type {AuthRequest} from "~/models/auth/auth-request";
import type {AuthResponse} from "~/models/auth/auth-response";
import type {RegisterRequest} from "~/models/user/register-request";
import type {AxiosInstance} from "axios";

export function useAuthService(api: AxiosInstance) {
    async function login(authRequest: AuthRequest): Promise<AuthResponse> {
        const test = await api.get<String>('/auth/test');
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

    return {
        login,
        register,
    }
}