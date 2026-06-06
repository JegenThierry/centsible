# ADR-0010: Logging — adze (Frontend) + SLF4J/MDC/Redaction (Backend)

Status:  Accepted
Date:    2026-06-06
Scope:   cross-cutting

## Context

Logs need to be consistent, correlatable across the client/server boundary, and safe (no
secrets or PII). Bare `console.log` on the client and ad-hoc string-concatenated logs on
the server give none of those.

## Decision

### Frontend
- Use **adze**, never `console.*`. Obtain a **namespaced** logger and log at the catch
  site: `adze.ns('<domain>').error('message', context, error)`.
- Namespaces follow the domain (`auth`, `budgets`, `transactions`, `api`, …).
- Logger setup lives in `app/plugins/logger.ts` — `activeLevel` is `verbose` in dev,
  `info` in production.
- Frontend logging is **error/warn-centric**: log failures and recoverable anomalies, not
  routine flow.

### Backend
- Declare a logger as a private field:
  `private val log = LoggerFactory.getLogger(X::class.java)` (SLF4J + Logback). Filters
  that extend `OncePerRequestFilter` may use the inherited `logger`.
- Use **parameterized templates**, never string interpolation:
  `log.info("Created contact id={} userId={}", created.id, user.id)`.
- **Levels:** `ERROR` = unhandled exceptions / security / data-integrity (pass the
  throwable); `WARN` = validation failures, request rejections, recoverable security
  events; `INFO` = successful mutations (with entity id + userId); `DEBUG` = off by
  default, raise only when chasing a bug.
- **Correlation:** every request gets a `requestId` and `userId` in the MDC (set by
  `RequestCorrelationFilter` + `UserContextMdcFilter`); the `X-Request-Id` is echoed in
  the response so client logs can be tied to server logs.
- **Never log secrets.** `SecretRedactingConverter` masks tokens/passwords/auth headers as
  a defense-in-depth **safety net** — not a licence to log sensitive data.

## Examples

**Do**
- Frontend: `adze.ns('budgets').error('Failed to fetch budgets for month', month, error);`
- Backend: `log.warn("Rate limit exceeded path={} ip={}", path, ip)`

**Don't**
- `console.error(err)` anywhere in the UI.
- `log.info("Created contact " + id + " for " + userId)` (string concatenation).
- Log an access token, password, or full request body; rely on parameter templates and let
  the redactor catch accidents only.

## Consequences

- Client and server logs share a namespace/level discipline and a request id, so a failing
  request is traceable end to end.
- Secret leakage has a structural backstop.
- Cost: contributors must reach for `adze` / `LoggerFactory` instead of `console`/`println`
  and think about level + correlation.
