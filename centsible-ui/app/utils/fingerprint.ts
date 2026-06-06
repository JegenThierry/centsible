/**
 * Join a stable string fingerprint per item. Used to skip needless `.value = next` reassignments
 * when polled responses haven't changed (which would otherwise re-render dependent tables).
 */
export function fingerprint<T>(items: T[], project: (item: T) => string): string {
  return items.map(project).join(',');
}
