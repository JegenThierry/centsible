export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
export const USERNAME_PATTERN = /^[a-zA-Z0-9_.-]+$/;
export const ICON_PATTERN = /^i-lucide-[a-z0-9-]+$/;

export const PASSWORD_RULES: { labelKey: string; test: (v: string) => boolean }[] = [
  {labelKey: 'auth.password.rules.length', test: (v) => v.length >= 8},
  {labelKey: 'auth.password.rules.case', test: (v) => /[A-Z]/.test(v) && /[a-z]/.test(v)},
  {labelKey: 'auth.password.rules.digit', test: (v) => /\d/.test(v)},
  {labelKey: 'auth.password.rules.special', test: (v) => /[@$!%*?&]/.test(v)},
];

export const isStrongPassword = (v: string): boolean => PASSWORD_RULES.every((r) => r.test(v));
