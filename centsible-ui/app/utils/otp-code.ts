/**
 * Strips all whitespace from a one-time / TOTP code. Authenticator apps display the code grouped
 * as "123 456", so a user who types or pastes it verbatim would otherwise submit an unmatchable
 * value. The backend normalises the same way; this keeps the client consistent. Recovery codes
 * (hyphen-separated, no spaces) are unaffected.
 */
export function normalizeOtpCode(code: string): string {
  return code.replace(/\s+/g, '');
}
