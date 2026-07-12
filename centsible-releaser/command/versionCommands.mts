import * as fs from "node:fs";
import * as path from "node:path";
import type { Mutate } from "./mutate.mts";

export function useVersionCommands(mutate: Mutate) {
  async function updatePackageJsonVersion(version: string): Promise<void> {
    const packageJsonPath = path.resolve("centsible-ui", "package.json");
    const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, "utf-8"));
    packageJson.version = version;

    await mutate(`Update centsible-ui/package.json to ${version}`, () =>
      fs.writeFileSync(
        packageJsonPath,
        JSON.stringify(packageJson, null, 2) + "\n",
      ),
    );
  }

  async function updateBuildGradleVersion(version: string): Promise<void> {
    const buildGradlePath = path.resolve("build.gradle.kts");
    const content = fs
      .readFileSync(buildGradlePath, "utf-8")
      .replace(/version = ".*"/, `version = "${version}"`);

    await mutate(`Update build.gradle.kts to ${version}`, () =>
      fs.writeFileSync(buildGradlePath, content),
    );
  }

  return { updatePackageJsonVersion, updateBuildGradleVersion };
}
