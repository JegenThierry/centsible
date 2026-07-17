interface InvalidatableCache {
  invalidate(): void;
}

const registered = new Set<InvalidatableCache>();

/** Enrolls a cache in ledger-wide invalidation; returns it so it can wrap a `createAsyncCache` call. */
export function registerLedgerAggregateCache<T extends InvalidatableCache>(cache: T): T {
  registered.add(cache);
  return cache;
}

/** Drops every enrolled aggregate memo. Called by the services that write ledger rows. */
export function invalidateLedgerAggregates(): void {
  for (const cache of registered) cache.invalidate();
}
