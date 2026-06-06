# ADR-0003: Authorization Is Enforced in the Service Layer

Status:  Accepted
Date:    2026-06-06
Scope:   backend · security

## Context

Centsible is multi-tenant: every row belongs to a user, and one user must never read or
mutate another's data. If authorization lived in controllers, every new endpoint would
be a chance to forget a check, and any internal caller (a job, the export worker, another
service) would bypass it entirely. Security must sit where the data is touched.

## Decision

- **Controllers do no authorization.** A `*Resource` extracts the caller via
  `@AuthenticationPrincipal authenticatedUser: UserDTO` and passes it, unmodified,
  straight into the service. Controllers never decide ownership and never throw
  `Forbidden`.
- **Services and repositories gate every read and write on ownership.** The typical
  mechanism is to scope the query to the user — `WHERE account.user_id = :userId` — or to
  re-fetch the parent entity scoped to the user (which throws if it isn't theirs) before
  acting on a child.
- **Never trust an id from the request without an ownership check.** An incoming
  `accountId`/`transactionId` is untrusted input until it has been confirmed against the
  authenticated user.

## Examples

**Do** — controller just forwards the principal:

```kotlin
@PostMapping("")
fun createAccount(
    @Valid @RequestBody req: CreateBudgetAccountRequest,
    @AuthenticationPrincipal authenticatedUser: UserDTO,
): ResponseEntity<BudgetAccountDTO> =
    ResponseEntity.ok(budgetAccountService.createAccount(req, authenticatedUser))
```

**Do** — the repository folds ownership into the SQL (every query, including
UPDATE/DELETE, carries `ACCOUNTS.USER_ID.eq(authenticatedUser.id)`).

**Don't**
- Add a service method that takes a bare `accountId` and acts on it without scoping to a
  `UserDTO`.
- Check ownership in the controller (`if (account.userId != user.id) ...`) and trust the
  service to be called only from there.

## Consequences

- The authorization boundary is the same for HTTP callers, scheduled jobs, and the export
  worker — there is exactly one place to audit.
- A missing check fails closed (the row simply isn't found for that user) rather than
  leaking data.
- Cost: service signatures must thread `UserDTO` through, and repositories carry the
  ownership predicate on every statement. Keep it that way.

Related: [ADR-0004](0004-localized-exception-hierarchy.md) (how the "not yours" case is
surfaced).
