import {saveAs} from 'file-saver';

const BLOB_PREVIEW_REVOKE_DELAY_MS = 5 * 60_000;

/** Opens [blob] in a new tab; the object URL is revoked after a 5-minute grace period, not immediately. */
export function openBlobInNewTab(blob: Blob): void {
  const url = URL.createObjectURL(blob);
  window.open(url, '_blank', 'noopener');
  setTimeout(() => URL.revokeObjectURL(url), BLOB_PREVIEW_REVOKE_DELAY_MS);
}

export function triggerBrowserDownload(blob: Blob, filename: string): void {
  saveAs(blob, filename);
}
