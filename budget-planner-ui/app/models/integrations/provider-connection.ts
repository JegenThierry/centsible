export type ProviderConnectionStatus = 'NEW' | 'ACTIVE' | 'ERROR' | 'REVOKED';

export interface ProviderConnection {
  id: string;
  providerKey: string;
  displayName: string;
  status: ProviderConnectionStatus;
  config: Record<string, unknown>;
  lastSyncAt?: string;
  lastError?: string;
  createdAt: string;
  modifiedAt: string;
}

export interface ProviderConnectionForm {
  providerKey: string;
  displayName: string;
  values: Record<string, unknown>;
}
