import type {Ref} from 'vue';

/** Replaces [item] in [list] by matching id, or prepends it when absent. */
export function upsertById<T extends {id: string}>(list: Ref<T[]>, item: T): void {
  const idx = list.value.findIndex(x => x.id === item.id);
  if (idx >= 0) {
    list.value[idx] = item;
    return;
  }
  list.value = [item, ...list.value];
}

/** Locale-aware comparator that sorts entities by their `name`. */
export const compareByName = <T extends {name: string}>(a: T, b: T): number => a.name.localeCompare(b.name);

/** Replaces [item] in [list] by matching id (or appends it), then keeps the list name-sorted. */
export function upsertSortedByName<T extends {id: string | number; name: string}>(list: Ref<T[]>, item: T): void {
  const others = list.value.filter(x => x.id !== item.id);
  list.value = [...others, item].sort(compareByName);
}
