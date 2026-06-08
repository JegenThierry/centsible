import {useArgumentParser} from "./arguments/argumentParser.mts";

const args = process.argv.slice(2)

const parser = useArgumentParser(args);
const version = parser.getVersion()
const dryRun = parser.isDryRun()
const push = parser.isPush()

console.log(parser.getVersion(), parser.isPush(), parser.isDryRun())


/**
 * General workflow:
 * 1. Parse and validate arguments.
 * 1.1 Validate the version.
 * 1.2 Check if Tag already exists.
 *
 * 2. Ensure a clean working tree.
 * 2.1 Check if the working tree is clean.
 * 2.2 Check if the current branch is 'develop'.
 * 2.3 Fetch and Pull develop
 *
 * 3. Create a release branch: git checkout -b release/<version>
 *
 * 4. Bump versions
 * 4.1 Bump version in package.json
 * 4.2 Bump version in build.gradle.kts
 *
 * 5. Create a release migration script
 * 5.1 Create a new migration folder: <version>
 * 5.2 Move snapshot files into the new folder. Keep the .gitkeep in snapshot.
 * 5.3 Check the highest number in the moved files.
 * 5.4 Create a new migration script: <NN>_SetVersion_<version>.sql (updates system_information)
 *
 * 6. Verify the build before tagging
 * 7. Commit with git commit -m "chore: Bump versions for release: <version>"
 * 8. Push to remote: git push origin release/<version>
 * 9. Create tag: git tag -a <version> -m "Release <version>"
 * 10. Push tag: git push origin <version>
 * 11. Create MRs on CodeBerg for develop and main.
 *
 * Notes:
 * --dry-run skips every destructive operation.
 */

