export interface TotpStatus {
  enabled: boolean;
  recoveryCodesRemaining: number;
}

export interface TotpEnrollment {
  otpauthUri: string;
  secret: string;
}

export interface RecoveryCodes {
  recoveryCodes: string[];
}
