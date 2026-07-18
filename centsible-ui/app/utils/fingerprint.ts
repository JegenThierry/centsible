export function fingerprint<T>(items: T[], project: (item: T) => string): string {
  return items.map(project).join(',');
}
