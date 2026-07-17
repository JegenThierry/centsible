interface CacheEntry<T> {
  data: T;
  promise: Promise<T> | null;
}

/**
 * Tiny module-level memo for async loaders keyed by a string. Concurrent calls with the same key
 * share the in-flight promise instead of dispatching duplicate requests. Failed lookups evict the
 * entry so a retry can re-issue the call.
 */
export function createAsyncCache<T>(empty: () => T) {
  const cache = new Map<string, CacheEntry<T>>();

  async function loadOrCache(key: string, loader: () => Promise<T>): Promise<T> {
    // Module scope is shared across every request on the SSR server — caching there would grow
    // unbounded and never invalidate between requests, so only the client memoizes.
    if (import.meta.server) return loader();
    const entry = cache.get(key);
    if (entry) {
      if (entry.promise) return entry.promise;
      return entry.data;
    }
    const promise = loader();
    cache.set(key, {data: empty(), promise});
    try {
      const result = await promise;
      cache.set(key, {data: result, promise: null});
      return result;
    } catch (e) {
      cache.delete(key);
      throw e;
    }
  }

  function invalidate(): void {
    cache.clear();
  }

  return {loadOrCache, invalidate};
}
