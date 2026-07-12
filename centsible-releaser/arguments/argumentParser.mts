import { parseArgs } from "node:util";

export interface Arguments {
  version: string;
  isDryRun: boolean;
}

const SEMVER_REGEX = /^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/;

export function useArgumentParser(args: string[]): Arguments {
  let values: { "dry-run": boolean };
  let positionals: string[];
  try {
    ({ values, positionals } = parseArgs({
      args,
      options: { "dry-run": { type: "boolean", default: false } },
      allowPositionals: true,
    }));
  } catch (error) {
    console.error(error instanceof Error ? error.message : String(error));
    process.exit(1);
  }

  const version = positionals[0];
  if (!version) {
    console.error("Version was not provided, aborting.");
    process.exit(1);
  }

  if (!SEMVER_REGEX.test(version)) {
    console.error(
      `Version ${version} is not a valid semantic version. Please use the format major.minor.patch`,
    );
    process.exit(1);
  }

  return { version, isDryRun: values["dry-run"] };
}
