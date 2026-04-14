export function hasKey<K extends string, T>(
  obj: unknown,
  key: K,
  valueGuard?: (value: unknown) => value is T
): obj is Record<K, T> {
  if (typeof obj !== 'object' || obj === null || !(key in obj)) {
    return false;
  }

  if (valueGuard) {
    return valueGuard((obj as Record<K, unknown>)[key]);
  }

  return true;
}

export const isString = (value: unknown): value is string => typeof value === 'string';
export const isNumber = (value: unknown): value is number => typeof value === 'number';
