/**
 * Tracks whether the user has completed (or skipped) the first-run onboarding flow.
 * Backed by localStorage so the choice persists across sessions on the same device.
 *
 * Serializer preserves the legacy '1' / '0' string layout from the pre-VueUse implementation
 * so existing installations don't re-trigger onboarding after upgrading.
 */
export const useOnboarded = () =>
  useStorage<boolean>('centsible.onboarded', false, undefined, {
    serializer: {
      read: (v: string) => v === '1' || v === 'true',
      write: (v: boolean) => (v ? '1' : '0'),
    },
  });
