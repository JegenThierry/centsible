# ADR-0007: Pages Stay Thin — Delegate to a Single Organism

Status:  Accepted
Date:    2026-06-06
Scope:   frontend

## Context

In Nuxt, page components carry routing/SSR concerns (middleware, head/meta). If they also
carry UI and business logic, those concerns get entangled, the UI can't be reused outside
its route, and the routing layer becomes hard to scan.

## Decision

A file under `centsible-ui/app/pages/**` does **only** three things:

1. declare route middleware via `definePageMeta`,
2. set the document head / title (through i18n),
3. render **a single organism** that owns the actual UI.

No markup beyond that one organism tag, no business logic, no data fetching in a page.
The pre-auth landing `pages/index.vue` is the **one sanctioned exception** — it holds
session-redirect logic — and even it delegates all rendering to an organism.

## Examples

**Do** — `pages/budgets/index.vue`, in full:

```vue
<script lang="ts" setup>
import Budgets from "~/components/_organisms/budgets/budgets.vue";

definePageMeta({ middleware: ['auth-guard'] });
const { t } = useI18n();
useHead({ title: t('budgets.page.title') });
</script>

<template>
  <Budgets/>
</template>
```

Every feature page in the app fits in well under 30 lines this way.

**Don't**
- Put a `<table>`, form, or any feature markup directly in a page.
- Call a store, service, or `useApi()` from a page (push that into the organism).
- Spread one screen's logic across the page and several inline blocks.

## Consequences

- The `pages/` tree reads as a pure route map: middleware + which organism renders.
- Feature UI is reusable and testable independently of its route.
- Cost: every screen needs a matching organism even when it feels small. Accept the extra
  file — it keeps the seam clean.

Related: [ADR-0006](0006-atomic-design-components.md).
