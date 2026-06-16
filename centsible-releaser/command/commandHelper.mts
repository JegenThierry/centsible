import { $ } from "execa";
import * as fs from "node:fs";
import * as path from "node:path";

/**
 * Changes the current directory to the specified directory.
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

export function useCommandHelper() {
  async function doesReleaseExist(version: string): Promise<boolean> {
    const tagName = version.startsWith("v") ? version : `v${version}`;
    console.log(`Checking if release ${tagName} exists...`);
    const output = (await $`git tag -l ${tagName}`).stdout;
    return output.trim() === tagName;
  }

  async function getCurrentBranch(): Promise<string> {
    console.log("Getting current branch...");
    const branch = (await $`git rev-parse --abbrev-ref HEAD`).stdout;
    console.log(`Current branch: ${branch.trim()}`);
    return branch.trim();
  }

  async function getUnstagedFiles(): Promise<string[]> {
    console.log("Getting unstaged files...");
    const output = (await $`git status --porcelain`).stdout;
    return output.split("\n").map((line) => line.trim().slice(3));
  }

  async function runGitFetch(): Promise<void> {
    console.log("Fetching latest changes...");
    await $`git fetch origin`;
  }

  async function runGitPull(): Promise<void> {
    console.log("Pulling latest changes...");
    const currentBranch = await getCurrentBranch();
    await $`git pull origin ${currentBranch}`;
  }

  async function checkoutNewBranchBasedOnVersion(version: string): Promise<void> {
    const branchName = `release/${version}`;
    console.log(`Creating new branch ${branchName}...`);
    await $`git checkout -b ${branchName}`;
  }

  async function createTag(version: string): Promise<void> {
    const tagName = `v${version}`;
    console.log(`Creating tag ${tagName}...`);
    await $`git tag -a ${tagName} -m ${`Release ${version}`}`;
  }

  async function createVersionBumpCommit(version: string): Promise<void> {
    const commitMessage = `chore: bump version to ${version}`;
    console.log(`Creating commit ${commitMessage}...`);
    await $`git commit -m ${commitMessage}`;
  }

  async function pushTag(version: string): Promise<void> {
    const tagName = `v${version}`;
    console.log(`Pushing tag ${tagName}...`);
    await $`git push origin ${tagName}`;
  }

  async function pushBranch(version: string): Promise<void> {
    const branchName = `release/${version}`;
    console.log(`Pushing branch ${branchName}...`);
    await $`git push origin ${branchName}`;
  }

  function updatePackageJsonVersion(version: string): void {
    const packageJsonPath = path.resolve("centsible-ui", "package.json");
    console.log(`Updating package.json at ${packageJsonPath} to ${version}...`);

    const content = fs.readFileSync(packageJsonPath, "utf-8");
    const packageJson = JSON.parse(content);
    packageJson.version = version;

    fs.writeFileSync(
      packageJsonPath,
      JSON.stringify(packageJson, null, 2) + "\n",
    );
  }

  function updateBuildGradleVersion(version: string): void {
    const buildGradlePath = path.resolve("build.gradle.kts");
    console.log(
      `Updating build.gradle.kts at ${buildGradlePath} to ${version}...`,
    );

    let content = fs.readFileSync(buildGradlePath, "utf-8");
    content = content.replace(/version = ".*"/, `version = "${version}"`);

    fs.writeFileSync(buildGradlePath, content);
  }

  function createMigrationFolder(version: string): void {
    try {
      const folderPath = path.resolve("centsible-db", "migrations", version);
      if (!fs.existsSync(folderPath)) {
        fs.mkdirSync(folderPath, { recursive: true });
      }
    } catch (e) {
      console.error(`Error creating migration folder: ${e}`);
      throw e;
    }
  }

  function calculateHighestMigrationNumber(files: string[]): number {
    return files.reduce((max, file) => {
      const match = file.match(/V(\d+)_/);
      return match ? Math.max(max, parseInt(match[1])) : max;
    }, 0);
  }

  function moveFiles(files: string[], srcDir: string, destDir: string): void {
    for (const file of files) {
      if (file === ".gitkeep") continue;
      const src = path.join(srcDir, file);
      const dest = path.join(destDir, file);
      console.log(`Moving ${file} from ${srcDir} to ${destDir}...`);
      fs.renameSync(src, dest);
    }
  }

  function moveSnapshotScriptsToMigrationFolder(version: string): number {
    const snapshotDir = path.resolve("centsible-db", "migrations", "snapshot");
    const migrationDir = path.resolve("centsible-db", "migrations", version);

    const files = fs.readdirSync(snapshotDir);

    const highestMigrationNumber = calculateHighestMigrationNumber(files);
    moveFiles(files, snapshotDir, migrationDir);

    return highestMigrationNumber;
  }

  function createMigrationScript(
    version: string,
    migrationNumber: number,
  ): void {
    const filename = `${migrationNumber}_SetVersion_${version.replaceAll(".", "_")}.sql`;
    const sqlContent = `UPDATE system_information
                            SET version     = '${version}',
                                released_at = CURRENT_DATE,
                                modified_at = now()
                            WHERE id = 1;`;
    const filePath = path.resolve(
      "centsible-db",
      "migrations",
      version,
      filename,
    );
    fs.writeFileSync(filePath, sqlContent);
  }

  return {
    checkoutNewBranchBasedOnVersion,
    createTag,
    createVersionBumpCommit,
    doesReleaseExist,
    getCurrentBranch,
    getUnstagedFiles,
    navigateToRepoRoot,
    pushBranch,
    pushTag,
    runGitFetch,
    runGitPull,
    updateBuildGradleVersion,
    updatePackageJsonVersion,
    createMigrationFolder,
    moveSnapshotScriptsToMigrationFolder,
    createMigrationScript,
  };
}
