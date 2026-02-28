import type {AxiosInstance} from "axios";
import type {UserDto} from "~/models/user/user-dto";
import {validateRequest} from "~/composables/use-api";

export function useUserService(api: AxiosInstance) {
    async function fetchMyself(): Promise<UserDto> {
        const response = await api.get<UserDto>('/users/myself');
        return validateRequest(response);
    }

    return {
        fetchMyself,
    }
}
