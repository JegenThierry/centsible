import {execFileSync} from "node:child_process";
import * as fs from "node:fs";
import * as path from "node:path";

export function useCommandHelper() {

    function executeCommand(cmd: string, args: string[]): string {
        try {
            console.log(`Executing command: ${cmd} ${args.join(' ')}`);
            return execFileSync(cmd, args, {encoding: 'utf-8'});
        } catch (error) {
            console.error(`Error executing command: ${error}`);
            throw error;
        }
    }

    function navigateToRepoRoot() {
        const root = executeCommand('git', ['rev-parse', '--show-toplevel']);
        console.log(`Navigating to repo root: ${root}`);
        process.chdir(root);
    }

    function doesReleaseExist(version: string): boolean {
        const tagName = version.startsWith('v') ? version : `v${version}`;
        console.log(`Checking if release ${tagName} exists...`);
        const output = executeCommand('git', ['tag', '-l', tagName]);
        return output.trim() === tagName;
    }

    function getCurrentBranch(): string {
        console.log('Getting current branch...');
        const branch = executeCommand('git', ['rev-parse', '--abbrev-ref', 'HEAD']);
        console.log(`Current branch: ${branch.trim()}`);
        return branch.trim();
    }

    function getUnstagedFiles(): string[] {
        console.log('Getting unstaged files...');
        const output = executeCommand('git', ['status', '--porcelain']);
        return output.split('\n').map(line => line.trim().slice(3));
    }

    function runGitFetch(): void {
        console.log('Fetching latest changes...');
        executeCommand('git', ['fetch', 'origin']);
    }

    function runGitPull(): void {
        console.log('Pulling latest changes...');
        executeCommand('git', ['pull', 'origin', getCurrentBranch()]);
    }

    function checkoutNewBranchBasedOnVersion(version: string): void {
        const branchName = `release/${version}`;
        console.log(`Creating new branch ${branchName}...`);
        executeCommand('git', ['checkout', '-b', branchName]);
    }

    function createTag(version: string): void {
        const tagName = `v${version}`;
        console.log(`Creating tag ${tagName}...`);
        executeCommand('git', ['tag', '-a', tagName, '-m', `Release ${version}`]);
    }

    function createVersionBumpCommit(version: string): void {
        const commitMessage = `chore: bump version to ${version}`;
        console.log(`Creating commit ${commitMessage}...`);
        executeCommand('git', ['commit', '-m', commitMessage]);
    }

    function pushTag(version: string): void {
        const tagName = `v${version}`;
        console.log(`Pushing tag ${tagName}...`);
        executeCommand('git', ['push', 'origin', tagName]);
    }

    function pushBranch(version: string): void {
        const branchName = `release/${version}`;
        console.log(`Pushing branch ${branchName}...`);
        executeCommand('git', ['push', 'origin', branchName]);
    }

    function updatePackageJsonVersion(version: string): void {
        const packageJsonPath = path.resolve('centsible-ui', 'package.json');
        console.log(`Updating package.json at ${packageJsonPath} to ${version}...`);

        const content = fs.readFileSync(packageJsonPath, 'utf-8');
        const packageJson = JSON.parse(content);
        packageJson.version = version;

        fs.writeFileSync(packageJsonPath, JSON.stringify(packageJson, null, 2) + '\n');
    }

    function updateBuildGradleVersion(version: string): void {
        const buildGradlePath = path.resolve('build.gradle.kts');
        console.log(`Updating build.gradle.kts at ${buildGradlePath} to ${version}...`);

        let content = fs.readFileSync(buildGradlePath, 'utf-8');
        content = content.replace(/version = ".*"/, `version = "${version}"`);

        fs.writeFileSync(buildGradlePath, content);
    }

    function createMigrationFolder(version: string): void {
        try {
            const folderPath = path.resolve('centsible-db', 'migrations', version);
            if (!fs.existsSync(folderPath)) {
                fs.mkdirSync(folderPath, {recursive: true});
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
            if (file === '.gitkeep') continue;
            const src = path.join(srcDir, file);
            const dest = path.join(destDir, file);
            console.log(`Moving ${file} from ${srcDir} to ${destDir}...`);
            fs.renameSync(src, dest);
        }
    }

    function moveSnapshotScriptsToMigrationFolder(version: string): number {
        const snapshotDir = path.resolve('centsible-db', 'migrations', 'snapshot');
        const migrationDir = path.resolve('centsible-db', 'migrations', version);

        const files = fs.readdirSync(snapshotDir);

        const highestMigrationNumber = calculateHighestMigrationNumber(files);
        moveFiles(files, snapshotDir, migrationDir);

        return highestMigrationNumber;
    }

    function createMigrationScript(version: string, migrationNumber: number): void {
        const filename = `${migrationNumber}_SetVersion_${version.replaceAll('.', '_')}.sql`;
        const sqlContent = `UPDATE system_information
                            SET version     = '${version}',
                                released_at = CURRENT_DATE,
                                modified_at = now()
                            WHERE id = 1;`;
        const filePath = path.resolve('centsible-db', 'migrations', version, filename);
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
        createMigrationScript
    };

}
