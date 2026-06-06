# ADR-0004: Sealed, Locale-Resolved Exception Hierarchy

Status:  Accepted
Date:    2026-06-06
Scope:   backend

## Context

Errors must reach the user in their language (en/fr/de), but the business layer should
not know about locale resolution, Spring's `MessageSource`, or HTTP status codes. We also
must never leak raw exception text or stack details to clients.

## Decision

- Services and repositories throw **`LocalizedException`** (a sealed class in
  `centsible-api/exceptions/`) carrying a **message-bundle key + args**, not a
  user-facing string. Use the status-typed subclasses: `BadRequest` (400),
  `Unauthorized` (401), `Forbidden` (403), `NotFound` (404), `Conflict` (409),
  `InternalError` (500).
- A single `GlobalExceptionHandler` (`@ControllerAdvice`) in `centsible-rest` resolves the
  key against the **active request locale**, maps to the HTTP status, and builds the
  response. It also handles Jakarta validation failures (→ 400 with field errors).
- For **unhandled** exceptions it returns a generic localized message plus the opaque
  request id (see [ADR-0010](0010-logging-adze-and-slf4j-mdc.md)) — never the raw message
  or stack trace.
- Every message key must exist in all locale bundles (see
  [ADR-0009](0009-i18n-feature-keyed-three-locale-parity.md)).

## Examples

**Do**

```kotlin
// in a service / repository
throw LocalizedException.NotFound("budget.notFound", budgetId)
throw LocalizedException.Conflict("account.name.duplicate", name)
```

`LocalizedException` implements `MessageSourceResolvable`, so the handler resolves it with
zero locale knowledge in the service.

**Don't**
- Throw `ResponseStatusException(HttpStatus.NOT_FOUND, "Budget not found")` with a
  hardcoded English string.
- Build a `ResponseEntity` with an error body inside a controller.
- Let a raw `IllegalStateException` message reach the client.

## Consequences

- Business code stays i18n-agnostic; all locale/HTTP mapping lives in one handler.
- Clients get consistent, translated, leak-free error payloads with a correlatable
  request id.
- Cost: every new error needs a message key added to all three locale bundles.
