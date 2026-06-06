# Coding Conventions & Architecture Decisions

This folder is the **normative source of truth** for how Centsible is built. Each file
documents one convention as a lightweight ADR (Architecture Decision Record): the
*context* that justifies it, the *decision* stated as a rule, concrete *do / don't*
examples from this repo, and the *consequences*.

These are not suggestions. New code — human- or AI-authored — is expected to follow
them. `CLAUDE.md` instructs Claude Code to read this folder before making changes.

This is a **curated, high-value set**: the conventions most worth writing down because
they are load-bearing, non-obvious, or easy to get wrong. Lower-level conventions
(formatting, the build/test commands, the export pipeline) live in `CLAUDE.md` and
`README.md`.

## Index

| ADR | Convention | Scope |
| --- | --- | --- |
| [0001](0001-onion-architecture.md) | Onion architecture — dependencies point inward only | backend |
| [0002](0002-layered-naming-and-domain-packaging.md) | Layered naming & domain-parallel packaging | backend |
| [0003](0003-service-layer-authorization.md) | Authorization is enforced in the service layer | backend · security |
| [0004](0004-localized-exception-hierarchy.md) | Sealed, locale-resolved exception hierarchy | backend |
| [0005](0005-plugin-spi-submodule-per-provider.md) | Plugin SPI — one Gradle submodule per provider/format | backend |
| [0006](0006-atomic-design-components.md) | Atomic design for UI components | frontend |
| [0007](0007-thin-pages-delegate-to-organism.md) | Pages stay thin — delegate to a single organism | frontend |
| [0008](0008-frontend-layered-data-flow.md) | Frontend layered data flow (models → services → stores → components) | frontend |
| [0009](0009-i18n-feature-keyed-three-locale-parity.md) | i18n — feature-keyed strings, three-locale parity | frontend |
| [0010](0010-logging-adze-and-slf4j-mdc.md) | Logging — adze (frontend) + SLF4J/MDC/redaction (backend) | cross-cutting |
| [0011](0011-single-responsibility-across-layers.md) | Single Responsibility Principle across layers | cross-cutting |
| [0012](0012-dependency-version-catalog.md) | Dependency versions via the Gradle version catalog | build |
| [0013](0013-sql-migrations-and-jooq-codegen.md) | Plain-SQL migrations + committed jOOQ codegen | backend · db |
| [0014](0014-secrets-handling-precommit-hook.md) | Secrets handling — gitignored properties + pre-commit hook | cross-cutting · security |

## How to read an entry

Every ADR follows the same shape:

```
# ADR-NNNN: Title
Status:  Accepted
Date:    YYYY-MM-DD

## Context     — the problem / why the rule exists
## Decision    — the rule, stated normatively
## Examples    — Do / Don't, with real file references
## Consequences — what we gain, what it costs
```

## Adding a new ADR

1. Copy the structure above into `NNNN-kebab-title.md` (next free number).
2. Keep it to one convention. If you're documenting two things, write two ADRs.
3. Cite real files in the codebase as evidence — ADRs describe how the code *is*.
4. Add a row to the index table.
5. If it's a rule Claude must always apply, make sure `CLAUDE.md`'s "Coding
   conventions" pointer still covers it (the pointer references this folder as a whole).

A `Status` may later become `Superseded by ADR-NNNN` rather than being deleted, so the
history of a decision stays readable.
