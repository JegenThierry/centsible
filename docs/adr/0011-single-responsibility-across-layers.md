# ADR-0011: Single Responsibility Principle Across Layers

Status:  Accepted
Date:    2026-06-06
Scope:   cross-cutting

## Context

The layered backend ([ADR-0001](0001-onion-architecture.md)) and tiered frontend
([ADR-0006](0006-atomic-design-components.md)) only stay maintainable if each unit keeps a
single reason to change. Multi-purpose services and god-components erode both
architectures from the inside even when the module boundaries are respected.

## Decision

Every unit has **one responsibility**; when it grows a second, split along the existing
seams rather than letting it widen.

**Backend**
- **One service per domain.** Cross-domain work goes through **injected interfaces**, not
  inline logic — e.g. `TransactionService` calls `ICategorizationService` and
  `INotificationService` instead of doing categorization/notification itself.
- **Mappers are separate** (`@Konverter` interfaces compiled by KSP), not hand-rolled
  inside services.
- **Repositories** do data access + ownership predicates only — no business rules.
- **Controllers** stay thin: extract the principal, delegate, log.

**Frontend**
- Atoms present, molecules compose, organisms orchestrate
  ([ADR-0006](0006-atomic-design-components.md)).
- **Services** talk to the API, **stores** hold state, **composables** (`use-*`) are
  reusable hooks — composables are **not** state containers
  ([ADR-0008](0008-frontend-layered-data-flow.md)).

## Examples

**Do**
- Have `TransactionService` depend on `ICategorizationService` and call it, keeping
  transaction logic and categorization logic in their own services.
- Extract a reusable behavior (an unsaved-changes guard) into a `use-*` composable instead
  of duplicating it across organisms.

**Don't**
- Inline categorization, notification, and persistence concerns into one fat service
  method.
- Put business rules in a repository, or data fetching in a molecule.
- Turn a composable into a hidden global store.

## Consequences

- Units are small, individually testable, and recomposable.
- Changes stay local: a categorization change touches `CategorizationService`, not
  `TransactionService`.
- Cost: more, smaller files and explicit interfaces between them. That granularity is the
  point.
