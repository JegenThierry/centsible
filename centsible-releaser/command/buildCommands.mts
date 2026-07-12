import { $ } from "execa";
import type { Mutate } from "./mutate.mts";

export function useBuildCommands(mutate: Mutate) {
  async function verifyBuild(): Promise<void> {
    await mutate("Verify backend build (./gradlew build -x test)", () =>
      $({ stdio: "inherit" })`./gradlew build -x test`,
    );
    await mutate("Install frontend dependencies (npm ci)", () =>
      $({ stdio: "inherit", cwd: "centsible-ui" })`npm ci`,
    );
    await mutate("Verify frontend build (npm run build)", () =>
      $({ stdio: "inherit", cwd: "centsible-ui" })`npm run build`,
    );
  }

  return { verifyBuild };
}
