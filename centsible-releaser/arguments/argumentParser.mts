export interface Arguments {
  version: string;
  isDryRun: boolean;
  isPush: boolean;
}

export function useArgumentParser(args: string[]): Arguments {
  const flags = args.filter((arg) => arg.startsWith("--"));
  const positional = args.filter((arg) => !arg.startsWith("--"));
  const SEMVER_REGEX = /^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/;

  function getVersion(): string {
    const version = positional[0];
    if (!version) {
      console.error(`Version was not provided aborting`);
      process.exit(1);
    }

    if (!SEMVER_REGEX.test(version)) {
      console.error(
        `Version ${version} is not a valid semantic version. Please use the format major.minor.patch`,
      );
      process.exit(1);
    }

    console.log(`Releasing version ${version}`);
    return version;
  }

  function isDryRun(): boolean {
    return flags.includes("--dry-run");
  }

  function isPush(): boolean {
    return flags.includes("--push");
  }

  return {
    version: getVersion(),
    isDryRun: isDryRun(),
    isPush: isPush(),
  };
}
