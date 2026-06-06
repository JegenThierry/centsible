export type AuthType = 'OAUTH2' | 'API_KEY' | 'BASIC' | 'NONE';
export type Capability = 'ACCOUNTS' | 'TRANSACTIONS' | 'QUOTES' | 'OAUTH_FLOW';
export type FieldType =
  | 'STRING'
  | 'NUMBER'
  | 'BOOLEAN'
  | 'SELECT'
  | 'MULTILINE'
  | 'SELECT_REMOTE'
  | 'OAUTH_LAUNCH';

export interface SelectOption {
  value: string;
  label: string;
}

export interface ConfigField {
  name: string;
  label: string;
  type: FieldType;
  required: boolean;
  secret: boolean;
  options: SelectOption[];
  placeholder?: string;
  helpText?: string;
  dependsOn?: string[];
}

export interface ProviderDescriptor {
  key: string;
  displayName: string;
  description?: string;
  authType: AuthType;
  capabilities: Capability[];
  configFields: ConfigField[];
}
