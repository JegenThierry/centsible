# ADR-0005: Plugin SPI — One Gradle Submodule per Provider/Format

Status:  Accepted
Date:    2026-06-06
Scope:   backend

## Context

Centsible integrates with external account providers (PayPal, GoCardless, …) and parses
several import file formats (CSV, OFX, …). Both are open sets that will grow. A central
registry or `when (type)` switch would force an edit to shared code for every addition and
couple all providers/formats together.

## Decision

Two SPIs, both following the **same pattern**: an interface in a `core` module, one
implementation per Gradle submodule, auto-discovered by Spring component scanning, wired
in by a **single `implementation(project(...))` line** in
`centsible-rest/build.gradle.kts`.

- **Integrations** (`centsible-integrations/`): SPI is
  `api.services.integrations.ProviderModule` plus capability **mixin** interfaces —
  `IAccountProvider`, `ITransactionImporter`, `IOAuthFlowProvider`,
  `IRemoteOptionsProvider`, `IQuoteProvider`. A provider implements **only the mixins it
  supports**. Submodules: `support` (shared HTTP/retry/string helpers), `manual`,
  `paypal`, `banking-gocardless`. Providers depend on `api` (+ `support`), **never** on
  `core` or `rest`.
- **File imports** (`centsible-imports/`): SPI is `imports.core.FileFormatParser`,
  collected by `FileImportRegistry`. Submodules: `core` (SPI + registry), `csv`, `ofx`.

**Adding a provider/format = a new submodule + one dependency line. Removing = delete the
line. No central registry edits.**

## Examples

**Do** — add a format by creating `centsible-imports/qif/` with an
`@Component QifFileParser : FileFormatParser`, then one line in the REST build file:

```kotlin
implementation(project(":centsible-imports:qif"))
```

The REST build file even documents this inline: *"To remove a format, delete its line."*

**Don't**
- Add a `when (format) { CSV -> ...; OFX -> ... }` switch or a hand-maintained list of
  parsers.
- Make a provider depend on `centsible-core` or `centsible-rest`, or implement a mixin it
  can't actually fulfil.

## Consequences

- Providers/formats are independently buildable, testable, and removable; the wiring is a
  one-line diff.
- Spring assembles the set at runtime via `List<FileFormatParser>` / component scanning —
  no registry to keep in sync.
- Cost: each plugin carries Gradle submodule boilerplate. Worth it for the isolation.

Related: [ADR-0001](0001-onion-architecture.md) (providers obey the inward-only rule).
