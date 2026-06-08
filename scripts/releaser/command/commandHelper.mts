import {execFileSync} from "node:child_process";

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



    return {navigateToRepoRoot, doesReleaseExist};

}
