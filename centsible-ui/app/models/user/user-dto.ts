import type {Currency} from "~/models/budget-account/currency";

export interface UserDto {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  name: string;
  profilePicture?: string;
  locale: string;
  defaultCurrency: Currency;
  /** True when this user is the instance admin designated by the server configuration. */
  admin?: boolean;
}
