import { $ } from "execa";

/**
 * Changes the current working directory.
 * @param dir The directory to change to.
 * @throws {Error} If the directory is not provided.
 */
export function changeDirectory(dir: string): void {
  if (!dir) {
    throw new Error("No directory provided.");
  }

  console.log(`Changing directory to: ${dir}`);
  process.chdir(dir.trim());
}

/** Moves the process to the git repository root so every path resolves from there. */
export async function navigateToRepoRoot(): Promise<void> {
  const root = (await $`git rev-parse --show-toplevel`).stdout.trim();
  changeDirectory(root);
}
