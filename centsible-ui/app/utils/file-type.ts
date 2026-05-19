export function iconFor(contentType: string): string {
  if (contentType === 'application/pdf') return 'i-lucide-file-text';
  if (contentType.startsWith('image/')) return 'i-lucide-image';
  return 'i-lucide-paperclip';
}
