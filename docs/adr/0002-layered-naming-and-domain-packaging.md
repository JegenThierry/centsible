# ADR-0002: Layered Naming & Domain-Parallel Packaging

Status:  Accepted
Date:    2026-06-06
Scope:   backend (with a frontend mirror)

## Context

With four backend layers and ~16 domains, a developer (or Claude) needs to find "the
service for transactions" or "the DTO for a budget account" without searching. Ad-hoc
names make the layered architecture invisible and slow every change.

## Decision

**Each artifact type has a fixed name and home**, keyed by the layer it lives in:

| Artifact | Name pattern | Location |
| --- | --- | --- |
| Controller | `XResource` | `centsible-rest/.../resources/` |
| Service interface | `IXService` | `centsible-api/.../services/<domain>/` |
| Service implementation | `XService` | `centsible-core/.../services/<domain>/` |
| Repository interface | `IXRepository` | `centsible-api/.../repository/` |
| Repository implementation | `XRepository` | `centsible-jooq/.../repository/` |
| DTOs / request forms | `XDTO`, `XForm`, `XRequest` | `centsible-api/.../model/<domain>/` |

**Domains run parallel across every layer.** A domain (`transactions`, `budget`,
`account`, `categories`, `categorization`, `contacts`, `loans`, `recurring`, `reports`,
`notifications`, `imports`, `integrations`, `users`, `authentication`, `email`,
`export`, `system`) appears as the **same package name** in `api`, `core`, and as a
matching folder under `centsible-ui/app/{models,services,stores}/`.

To add or change a feature, follow the one domain name through every layer.

## Examples

**Do**
- `BudgetAccountResource` (rest) → `IBudgetAccountService` (api) → `BudgetAccountService`
  (core) → `IBudgetAccountsRepository` (api) → `BudgetAccountsRepository` (jooq).
- Put a new `TransferDTO` under `centsible-api/.../model/transaction/`, not next to the
  controller.

**Don't**
- Name a controller `BudgetAccountController` or put it outside `resources/`.
- Drop the `I` prefix on an `api`-layer interface, or add it to a jOOQ impl class.
- Invent a new package name for a domain that already exists in another layer.

## Consequences

- Any artifact is findable by name + layer alone; the architecture is self-documenting.
- New contributors can predict where code goes before reading it.
- Cost: a single feature spreads its files across modules. That is the price of the
  layering in [ADR-0001](0001-onion-architecture.md), not extra overhead.
