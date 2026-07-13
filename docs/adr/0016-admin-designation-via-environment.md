# ADR-0016: Admin Designation via Environment Configuration

Status:  Accepted
Date:    2026-07-13
Scope:   cross-cutting · security · backend + frontend

## Context

Centsible is self-hosted and multi-user, but had no operator-facing surface at all: every
authenticated principal carried a hardcoded `ROLE_USER`, every repository query was scoped to
`user_id`, and the only way to manage accounts was direct SQL. Self-hosters need a minimal admin
view (list users, delete an account, resend a stuck verification email) without introducing a
full role system, a roles table, or an in-app privilege-escalation path.

## Decision

- **The admin is designated by configuration, not by data.** `ADMIN_USERNAME` (property
  `admin.username`) names exactly one account; `ADMIN_ENABLED` (`admin.enabled`, default `false`)
  is the kill switch for the whole feature. There is no `is_admin` column, no migration, and no
  way to grant admin from inside the app — changing the admin means editing `.env` and
  restarting, which is deliberate for a self-hosted deployment.
- **`AdminAccessService` is the single source of truth** for "is this the admin?"
  (`IAdminAccessService` in `centsible-api`). It is dependency-free so the JWT filter and
  `UserService` can inject it without bean cycles. `UserDTO.admin` is computed from it at
  request time (never stored in the JWT), so flipping the flag takes effect on restart with no
  token reissue; when true the principal also carries `ROLE_ADMIN`.
- **Authorization stays in the service layer (ADR-0003).** Every `IAdminService` operation
  re-checks the caller and throws `LocalizedException.Forbidden` (`error.admin.disabled` /
  `error.admin.forbidden`). The `/api/admin/** → hasRole('ADMIN')` rule in `SecurityConfig` is
  defense in depth, not the authorization.
- **Cross-user reads are quarantined in `IAdminRepository`.** Every other repository filters by
  `user_id` (ADR-0003); the deliberately unscoped queries live in this one clearly-named place,
  and return **metadata only** (usernames, emails, status, coarse counts) — never balances,
  transactions, or documents.
- **The frontend gates purely on `UserDto.admin`** from `/users/myself` (sidebar entry +
  `admin-guard` middleware). No separate feature-flag endpoint: when the feature is disabled
  server-side, nobody carries the flag.
- **`ProductionGuard` fails boot** under the `prod` profile when `admin.enabled=true` with a
  blank `admin.username` (an enabled-but-unreachable admin area is a misconfiguration).

## Examples

**Do**
- Add new admin capabilities as `IAdminService` methods that call `requireAdmin` first, and new
  cross-user queries to `IAdminRepository`, returning metadata-shaped DTOs.
- Keep UI gating on `userStore.user?.admin` — the backend re-checks every call anyway.

**Don't**
- Add an unscoped query to a domain repository "because the admin needs it" — it belongs in
  `IAdminRepository`.
- Expose other users' financial data through an admin endpoint; that boundary is the decision.
- Check `admin.username` ad hoc with `@Value` in a new place — inject `IAdminAccessService`.

## Consequences

- (+) No schema change, no role framework, no in-app escalation path; the admin surface can be
  switched off entirely with one env var.
- (+) The metadata-only boundary keeps the per-user data-isolation guarantee intact even for the
  operator.
- (−) Exactly one admin, and changing it requires a restart. If multiple admins or runtime
  promotion are ever needed, that supersedes this ADR (an `is_admin` column seeded from env
  would be the natural evolution).
- (−) The admin username must match an existing account; a typo silently yields "nobody is
  admin" outside prod (prod boot fails only on a *blank* username).
