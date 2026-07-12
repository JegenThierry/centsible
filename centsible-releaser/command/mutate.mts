export type Mutate = (label: string, action: () => unknown) => Promise<void>;

/**
 * Builds the dry-run gate shared by every mutating command.
 *
 * In dry-run mode it only logs what it would do and skips the side effect;
 * otherwise it logs and runs the action. Read-only queries never go through
 * here, so a dry-run preview stays truthful.
 */
export function createMutate(isDryRun: boolean): Mutate {
  return async function mutate(
    label: string,
    action: () => unknown,
  ): Promise<void> {
    if (isDryRun) {
      console.log(`[dry-run] ${label}`);
      return;
    }
    console.log(label);
    await action();
  };
}
