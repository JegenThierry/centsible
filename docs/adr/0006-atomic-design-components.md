# ADR-0006: Atomic Design for UI Components

Status:  Accepted
Date:    2026-06-06
Scope:   frontend

## Context

The frontend has ~200 components. Without a shared notion of "how big is this component
and what may it do", components accrete responsibilities, presentational primitives start
fetching data, and reuse collapses.

## Decision

Components under `centsible-ui/app/components/` are organized into **four tiers by
composition complexity**, each with a defined responsibility ceiling:

- **`_atoms/`** (~31) — stateless presentational primitives. Props and emits only. No
  store, no service, no async, no business logic. e.g. `user-avatar.vue`,
  `formatted-date.vue`.
- **`_molecules/`** (~63) — small compositions of atoms with minor local logic (a form, a
  filter bar, a table shell). No store mutations, no business rules.
- **`_organisms/`** (~106) — feature units that orchestrate molecules/organisms, own modal
  state, **read stores**, and trigger mutations. e.g. `budgets/budgets.vue`.
- **`_wrapper/`** (1) — structural layout shells (header/content/footer slots).

Within a tier, group by **feature** subfolder (`budgets/`, `transactions/`), not by type.

**Authoring rules** (all tiers): `<script setup lang="ts">`, typed
`defineProps<Props>()` / `defineEmits<…>()`, PascalCase component name from a kebab-case
file (`_atoms/user/user-avatar.vue` → `<UserAvatar/>`).

## Examples

**Do**
- Keep `formatted-date.vue` an atom: it takes a date prop and renders — nothing else.
- Put data fetching + modal orchestration in an organism (`budgets.vue`), built from
  molecule sub-components.

**Don't**
- Call a store or `useApi()` from an atom or molecule.
- Create a flat `components/HugeForm.vue` that mixes fetching, validation, and layout —
  split it across the tiers.
- Use the Options API or untyped props.

## Consequences

- Predictable composition and high reuse; the tier of a file tells you what it may do.
- Easy to locate work: presentational bug → atom/molecule; behavior bug → organism.
- Cost: every new component requires a deliberate "which tier?" decision. That decision is
  the value.

Related: [ADR-0007](0007-thin-pages-delegate-to-organism.md),
[ADR-0008](0008-frontend-layered-data-flow.md),
[ADR-0011](0011-single-responsibility-across-layers.md).
