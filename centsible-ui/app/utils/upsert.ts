import type {Ref} from 'vue';

export function upsertById<T extends {id: string}>(list: Ref<T[]>, item: T): void {
  const idx = list.value.findIndex(x => x.id === item.id);
  if (idx >= 0) {
    list.value[idx] = item;
    return;
  }
  list.value = [item, ...list.value];
}
