/**
 * Monotonic "latest request wins" gate for state a slow response could clobber.
 *
 * Debouncing only coalesces changes inside its window: two requests dispatched further apart still
 * race, and backend latency variance lets the older one land last and paint stale data. Claim a
 * token *before* the await and re-check it after — an outrun request then drops its result instead
 * of overwriting a newer one's.
 *
 * ```ts
 * const gate = createLatestRequestGate();
 *
 * async function load(month: string) {
 *   const isLatest = gate.begin();
 *   const data = await service.fetchAll(month);
 *   if (!isLatest()) return;
 *   items.value = data;
 * }
 * ```
 *
 * One gate guards one piece of state. Share a single gate across every entry point that writes the
 * same ref, and share one `isLatest` across a batch of calls whose results must agree with each
 * other (a per-call token would let two halves of a batch settle from different generations).
 */
export function createLatestRequestGate() {
  let token = 0;

  return {
    /** Claims the newest token; the returned predicate reports whether this request still holds it. */
    begin(): () => boolean {
      const current = ++token;
      return () => current === token;
    },
    /**
     * Supersedes every in-flight request without starting one — for state written or cleared
     * locally, which an outstanding response would otherwise roll back or resurrect.
     */
    supersede(): void {
      token++;
    },
  };
}
