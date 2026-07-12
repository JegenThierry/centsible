import { $ } from "execa";
import type { Mutate } from "./mutate.mts";

export function useGitCommands(mutate: Mutate) {
  async function doesReleaseExist(version: string): Promise<boolean> {
    const tagName = version.startsWith("v") ? version : `v${version}`;
    console.log(`Checking if release ${tagName} exists...`);
    const output = (await $`git tag -l ${tagName}`).stdout;
    return output.trim() === tagName;
  }

  async function getCurrentBranch(): Promise<string> {
    const branch = (await $`git rev-parse --abbrev-ref HEAD`).stdout.trim();
    console.log(`Current branch: ${branch}`);
    return branch;
  }

  async function getUnstagedFiles(): Promise<string[]> {
    console.log("Checking for a clean working tree...");
    const output = (await $`git status --porcelain`).stdout;
    return output
      .split("\n")
      .filter((line) => line.trim().length > 0)
      .map((line) => line.slice(3));
  }

  async function runGitFetch(): Promise<void> {
    await mutate("Fetching latest changes from origin", () => $`git fetch origin`);
  }

  async function runGitPull(): Promise<void> {
    const currentBranch = await getCurrentBranch();
    await mutate(`Pulling latest changes for ${currentBranch}`, () =>
      $`git pull origin ${currentBranch}`,
    );
  }

  async function checkoutNewBranchBasedOnVersion(
    version: string,
  ): Promise<void> {
    const branchName = `release/${version}`;
    await mutate(`Creating branch ${branchName}`, () =>
      $`git checkout -b ${branchName}`,
    );
  }

  async function stageReleaseChanges(): Promise<void> {
    await mutate("Stage release changes", () =>
      $`git add build.gradle.kts centsible-ui/package.json centsible-db/migrations`,
    );
  }

  async function createVersionBumpCommit(version: string): Promise<void> {
    const commitMessage = `chore(release): bump version to ${version}`;
    await mutate(`Commit "${commitMessage}"`, () =>
      $`git commit -m ${commitMessage}`,
    );
  }

  async function pushBranch(version: string): Promise<void> {
    const branchName = `release/${version}`;
    await mutate(`Push branch ${branchName}`, () =>
      $`git push origin ${branchName}`,
    );
  }

  async function createTag(version: string): Promise<void> {
    const tagName = `v${version}`;
    await mutate(`Create tag ${tagName}`, () =>
      $`git tag -a ${tagName} -m ${`Release ${version}`}`,
    );
  }

  async function pushTag(version: string): Promise<void> {
    const tagName = `v${version}`;
    await mutate(`Push tag ${tagName}`, () => $`git push origin ${tagName}`);
  }

  return {
    doesReleaseExist,
    getCurrentBranch,
    getUnstagedFiles,
    runGitFetch,
    runGitPull,
    checkoutNewBranchBasedOnVersion,
    stageReleaseChanges,
    createVersionBumpCommit,
    pushBranch,
    createTag,
    pushTag,
  };
}
