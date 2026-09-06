# ADR-0005: Plugin SPI — One Gradle Submodule per Provider/Format

Status:  Accepted
Date:    2026-06-06
Scope:   backend

## Context

Centsible parses several import file formats (CSV, OFX, …). This is an open set that will
grow. A central registry or `when (type)` switch would force an edit to shared code for
every addition and couple all formats together.

## Decision

A single SPI following this **pattern**: an interface in a `core` module, one implementation
per Gradle submodule, auto-discovered by Spring component scanning, wired in by a **single
`implementation(project(...))` line** in `centsible-rest/build.gradle.kts`.

- **File imports** (`centsible-imports/`): SPI is `imports.core.FileFormatParser`,
  collected by `FileImportRegistry`. Submodules: `core` (SPI + registry), `csv`, `ofx`.

**Adding a format = a new submodule + one dependency line. Removing = delete the line. No
central registry edits.**

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
- Make a parser depend on `centsible-core` or `centsible-rest`, or implement a mixin it
  can't actually fulfil.

## Consequences

- Formats are independently buildable, testable, and removable; the wiring is a one-line
  diff.
- Spring assembles the set at runtime via `List<FileFormatParser>` / component scanning —
  no registry to keep in sync.
- Cost: each plugin carries Gradle submodule boilerplate. Worth it for the isolation.

## Superseded scope

This ADR previously also covered an **account-provider SPI** (`centsible-integrations/`,
PayPal, GoCardless, …). That integration layer was removed so the core features could be
stabilized first; see the removal work for provider connections, sync, OAuth and the FX
provider. Only the file-import SPI above remains in effect.

Related: [ADR-0001](0001-onion-architecture.md) (imports obey the inward-only rule).
