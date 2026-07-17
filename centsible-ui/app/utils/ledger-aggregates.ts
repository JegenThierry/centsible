/**
 * Ledger-wide invalidation for the dashboard's aggregate memos.
 *
 * The by-day / by-month / by-category aggregates and the account snapshots each memoize into a
 * module-level {@link createAsyncCache} with no TTL, so *any* write to the ledger — from any page,
 * not just the dashboard — has to drop them or the charts serve pre-mutation money for the rest of
 * the session.
 *
 * Expecting every writer to remember four separate `invalidate*` calls didn't work (only the
 * dashboard's own create modal ever did). Instead each cache enrolls itself here on module load and
 * the services that write ledger rows call {@link invalidateLedgerAggregates} once, so new writers
 * are covered by default rather than by memory.
 *
 * A cache that was never imported was never populated, so an unregistered cache is nothing to drop.
 */
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
