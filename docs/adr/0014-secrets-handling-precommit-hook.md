# ADR-0014: Secrets Handling — Gitignored Properties + Pre-Commit Hook

Status:  Accepted
Date:    2026-06-06
Scope:   cross-cutting · security

## Context

Spring `application.properties` files routinely hold JWT secrets, DB credentials, and API
keys. A single accidental commit of one leaks them irreversibly into git history.

## Decision

- Real `application.properties` files are **gitignored**. Only `*.example` template files
  are committed; developers copy a template to `application.properties` and fill in real
  values locally.
- A `.githooks/pre-commit` hook **blocks committing any `application.properties`**. It is
  not active by default — enable it once per clone:
  `git config core.hooksPath .githooks`.
- **Secrets never appear** in committed files, in logs (see
  [ADR-0010](0010-logging-adze-and-slf4j-mdc.md)), or hardcoded in source.
- `ProductionGuard` (`@PostConstruct`, `prod` profile) **hard-fails the boot** if any
  secret is left at its `.env.example` placeholder, if email verification is skipped, or
  if cookies are insecure. Keep new production-safety invariants there.

## Examples

**Do**
- Commit `application.properties.example` with placeholder values; document the real keys
  there.
- Run `git config core.hooksPath .githooks` after cloning so the guard is live.
- Add a new prod-safety check to `ProductionGuard` when you introduce a new secret.

**Don't**
- `git add` an `application.properties` (the hook will refuse it — don't `--no-verify`
  around it).
- Put a real secret in a `.example` file, a test resource, or an inline default.

## Consequences

- Secrets stay out of history by default, with a mechanical backstop and a startup guard.
- Onboarding includes a one-line hook setup and a template copy.
- Cost: the hook must be enabled per clone (it can't auto-install), so it's part of setup,
  not a guarantee — treat the gitignore + review as the primary defense and the hook as the
  net.
