import * as fs from "node:fs";
import * as path from "node:path";
import type { Mutate } from "./mutate.mts";

export function useMigrationCommands(mutate: Mutate) {
  async function createMigrationFolder(version: string): Promise<void> {
    const folderPath = path.resolve("centsible-db", "migrations", version);
    if (fs.existsSync(folderPath)) {
      return;
    }

    await mutate(`Create migration folder migrations/${version}`, () =>
      fs.mkdirSync(folderPath, { recursive: true }),
    );
  }

  function calculateHighestMigrationNumber(files: string[]): number {
    return files.reduce((max, file) => {
      const match = file.match(/^(\d+)_/);
      return match ? Math.max(max, parseInt(match[1], 10)) : max;
    }, 0);
  }

  async function moveFiles(
    files: string[],
    srcDir: string,
    destDir: string,
  ): Promise<void> {
    for (const file of files) {
      if (file === ".gitkeep") continue;
      const src = path.join(srcDir, file);
      const dest = path.join(destDir, file);
      await mutate(
        `Move ${file} into migrations/${path.basename(destDir)}`,
        () => fs.renameSync(src, dest),
      );
    }
  }

  async function moveSnapshotScriptsToMigrationFolder(
    version: string,
  ): Promise<number> {
    const snapshotDir = path.resolve("centsible-db", "migrations", "snapshot");
    const migrationDir = path.resolve("centsible-db", "migrations", version);

    const files = fs.readdirSync(snapshotDir);
    const highestMigrationNumber = calculateHighestMigrationNumber(files);
    await moveFiles(files, snapshotDir, migrationDir);

    return highestMigrationNumber;
  }

  async function createMigrationScript(
    version: string,
    highestMigrationNumber: number,
  ): Promise<void> {
    const nextNumber = String(highestMigrationNumber + 1).padStart(2, "0");
    const filename = `${nextNumber}_SetVersion_${version.replaceAll(".", "_")}.sql`;
    const filePath = path.resolve(
      "centsible-db",
      "migrations",
      version,
      filename,
    );
    const sqlContent = `UPDATE system_information
SET version     = '${version}',
    released_at = CURRENT_DATE,
    modified_at = now()
WHERE id = 1;
`;

    await mutate(`Create migration ${version}/${filename}`, () =>
      fs.writeFileSync(filePath, sqlContent),
    );
  }

  return {
    createMigrationFolder,
    moveSnapshotScriptsToMigrationFolder,
    createMigrationScript,
  };
}
