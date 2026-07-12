import { useArgumentParser } from "./arguments/argumentParser.mts";
import { createMutate } from "./command/mutate.mts";
import { navigateToRepoRoot } from "./command/repository.mts";
import { useGitCommands } from "./command/gitCommands.mts";
import { useVersionCommands } from "./command/versionCommands.mts";
import { useMigrationCommands } from "./command/migrationCommands.mts";
import { useBuildCommands } from "./command/buildCommands.mts";

const args = process.argv.slice(2);
const { version, isDryRun } = useArgumentParser(args);

const mutate = createMutate(isDryRun);
const git = useGitCommands(mutate);
const versionBump = useVersionCommands(mutate);
const migrations = useMigrationCommands(mutate);
const build = useBuildCommands(mutate);

console.log(`Releasing version ${version}${isDryRun ? " (dry-run)" : ""}\n`);

/**
 * 1. Move to the repository root so every path and git command resolves from there.
 */
await navigateToRepoRoot();

/**
 * 2. Refuse to release a version that already has a tag.
 */
if (await git.doesReleaseExist(version)) {
  console.error(`Release v${version} already exists.`);
  process.exit(1);
}

/**
 * 3. Require a clean working tree on develop, then sync with origin.
 */
const unstagedFiles = await git.getUnstagedFiles();
if (unstagedFiles.length > 0) {
  console.error(`Working tree is not clean: ${unstagedFiles.join(", ")}`);
  process.exit(1);
}

const branch = await git.getCurrentBranch();
if (branch !== "develop") {
  console.error(
    `Current branch is ${branch}, but releases must start from develop.`,
  );
  process.exit(1);
}

await git.runGitFetch();
await git.runGitPull();

/**
 * 4. Create the release branch: release/<version>.
 */
await git.checkoutNewBranchBasedOnVersion(version);

/**
 * 5. Bump the version in package.json and build.gradle.kts.
 */
await versionBump.updatePackageJsonVersion(version);
await versionBump.updateBuildGradleVersion(version);

/**
 * 6. Roll pending snapshot migrations into a versioned folder and append a
 *    SetVersion script that records the release in system_information.
 */
await migrations.createMigrationFolder(version);
const highestMigrationNumber =
  await migrations.moveSnapshotScriptsToMigrationFolder(version);
await migrations.createMigrationScript(version, highestMigrationNumber);

/**
 * 7. Verify the backend and frontend build before committing.
 */
await build.verifyBuild();

/**
 * 8. Stage, commit, and tag the release.
 */
await git.stageReleaseChanges();
await git.createVersionBumpCommit(version);
await git.createTag(version);

/**
 * 9. Publish the release branch and tag to origin.
 */
await git.pushBranch(version);
await git.pushTag(version);

console.log(
  isDryRun
    ? "\nDry-run complete. No changes were made."
    : `\nDone. Released v${version}.`,
);
