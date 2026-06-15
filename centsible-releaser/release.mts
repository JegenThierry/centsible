import { useArgumentParser } from "./arguments/argumentParser.mts";
import { useCommandHelper } from "./command/commandHelper.mts";

const args = process.argv.slice(2);
const commandHelper = useCommandHelper();

/**
 * General workflow:
 * 1. Parse and validate arguments.
 * 2. Navigate to the repository root.
 * 3. Check if the release already exists.
 */
const { version, isPush, isDryRun } = useArgumentParser(args);
commandHelper.navigateToRepoRoot();
if (commandHelper.doesReleaseExist(version)) {
  console.error(`Release ${version} already exists`);
  process.exit(1);
}

/**
 * 2. Ensure a clean working tree.
 * 2.1 Check if the working tree is clean.
 * 2.2 Check if the current branch is 'develop'.
 * 2.3 Fetch and Pull develop
 */
const unstagedFiles = commandHelper.getUnstagedFiles();
if (unstagedFiles.length > 0) {
  console.error(`Unstaged files: ${unstagedFiles.join(", ")}`);
  process.exit(1);
}

const branch = commandHelper.getCurrentBranch();
if (branch !== "develop") {
  console.error(`Current branch is ${branch}, but should be develop`);
  process.exit(1);
}

commandHelper.runGitFetch();
commandHelper.runGitPull();

/**
 * 3. Create a release branch: git checkout -b release/<version>
 */
commandHelper.checkoutNewBranchBasedOnVersion(version);

/**
 * 4. Bump versions
 * 4.1 Bump version in package.json
 * 4.2 Bump version in build.gradle.kts
 */
commandHelper.updatePackageJsonVersion(version);
commandHelper.updateBuildGradleVersion(version);

/**
 * 5. Create a release migration script
 * 5.1 Create a new migration folder: <version>
 * 5.2 Move snapshot files into the new folder. Keep the .gitkeep in snapshot.
 * 5.3 Check the highest number in the moved files.
 * 5.4 Create a new migration script: <NN>_SetVersion_<version>.sql (updates system_information)
 */
commandHelper.createMigrationFolder(version);
const highestMigrationNumber =
  commandHelper.moveSnapshotScriptsToMigrationFolder(version);
commandHelper.createMigrationScript(version, highestMigrationNumber);

// TODO: Verify build

/**
 * 7. Push to remote: git push origin release/<version>
 * 8. Commit with git commit -m "chore: Bump versions for release: <version>"
 * 9. Create tag: git tag -a <version> -m "Release <version>"
 * 10. Push tag: git push origin <version>
 */
commandHelper.createVersionBumpCommit(version);
commandHelper.pushBranch(version);
commandHelper.createTag(version);
commandHelper.pushTag(version);
