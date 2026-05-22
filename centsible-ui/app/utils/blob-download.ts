const BLOB_PREVIEW_REVOKE_DELAY_MS = 5 * 60_000;

export function openBlobInNewTab(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), BLOB_PREVIEW_REVOKE_DELAY_MS);
}
