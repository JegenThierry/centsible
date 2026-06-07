export interface TotpStatus {
  enabled: boolean;
}

export interface TotpEnrollment {
  otpauthUri: string;
  secret: string;
}

export interface RecoveryCodes {
  recoveryCodes: string[];
}
