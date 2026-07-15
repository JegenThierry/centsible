# ADR-0008: Frontend Layered Data Flow (models → services → stores → components)

Status:  Accepted
Date:    2026-06-06
Scope:   frontend

## Context

Without a layering rule, components call `axios` directly, error handling and loading
state are reinvented per component, and API shapes leak into templates. The frontend
should mirror the backend's onion ([ADR-0001](0001-onion-architecture.md)) so the data
flow is predictable end to end.

## Decision

Data flows in **one direction**, `models → services → stores → components`:

- **`models/<domain>/`** — plain TypeScript types mirroring the backend DTOs. No logic.
- **`services/<domain>/`** — `useXService(api: AxiosInstance)` **factory functions**.
  Stateless. They call REST endpoints and validate responses with the helpers in
  `composables/use-api.ts` (`validateRequest`, `assertStatus`, `postMultipart`). A service
  holds no reactive state.
- **`stores/`** — Pinia **setup-syntax** stores. Each instantiates its service via
  `useXService(useApi())`, owns the reactive state, and exposes one **`loading` flag per
  async action** (set in `try` / cleared in `finally`). Stores catch errors, log them via
  `adze`, and surface user feedback (toasts).
- **`components/`** — read **stores**, not services. Call store actions; never `axios`
  directly.

**Pragmatic exception:** a transient modal/form may call a service directly for a one-shot
mutation and then `emit` success, rather than routing through a store. Keep this to
short-lived, self-contained widgets.

**Pragmatic exception:** a store action may **reject instead of catching** when its caller has to
distinguish failure kinds that a toast would flatten — `budgetAccountsStore.loadActiveAccount`
rejects so `middleware/account-loader` can tell a missing account (redirect to the list) from a
transient failure (keep the user where they are); a swallowed error makes those identical. The
caller then owns the logging and the user feedback. Keep this to actions consumed by a route
guard, where the outcome is a redirect rather than a toast.

## Examples

**Do** — a store owns state, loading, service, and error logging:

```ts
export const useBudgetsStore = defineStore('budgetsStore', () => {
  const service = useBudgetService(useApi());
  const items = ref<Budget[]>([]);
  const loading = ref(false);

  async function fetchCurrentMonth() {
    loading.value = true;
    try { items.value = await service.fetchAll(); }
    catch (error) { adze.ns('budgets').error('Failed to fetch budgets', error); items.value = []; }
    finally { loading.value = false; }
  }
  return { items, loading, fetchCurrentMonth };
});
```

**Don't**
- Call `useApi()` / `axios` from a component to fetch data.
- Hold reactive state inside a service, or use the Pinia Options API in a store.
- Skip the `loading` flag or swallow errors without logging.

## Consequences

- API access, validation, loading state, and error UX are centralized and uniform.
- Swapping an endpoint touches one service; components are unaffected.
- Cost: four files per domain (model, service, store, component). That symmetry is what
  makes the codebase navigable — see [ADR-0002](0002-layered-naming-and-domain-packaging.md).

Related: [ADR-0010](0010-logging-adze-and-slf4j-mdc.md) (the `adze` call above).
