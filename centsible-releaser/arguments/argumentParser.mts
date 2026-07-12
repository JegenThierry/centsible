export interface Arguments {
  version: string;
  isDryRun: boolean;
}

export function useArgumentParser(args: string[]): Arguments {
  const flags = args.filter((arg) => arg.startsWith("--"));
  const positional = args.filter((arg) => !arg.startsWith("--"));
  const SEMVER_REGEX = /^\d+\.\d+\.\d+(-[0-9A-Za-z.-]+)?$/;

  function getVersion(): string {
    const version = positional[0];
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

    return version;
  }

  return {
    version: getVersion(),
    isDryRun: flags.includes("--dry-run"),
  };
}
