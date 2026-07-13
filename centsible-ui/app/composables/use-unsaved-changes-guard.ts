import {type Ref, computed, nextTick, ref, unref, watch} from 'vue';
import {onBeforeRouteLeave} from 'vue-router';

type MaybeRef<T> = T | Ref<T>;

export function useUnsavedChangesGuard(dirty: Ref<boolean>, message?: MaybeRef<string>) {
  const {t} = useI18n();

  function getMessage(): string {
    return unref(message) ?? t('common.unsavedChanges.confirmLeave');
  }

  useEventListener('beforeunload', (event: BeforeUnloadEvent) => {
    if (!dirty.value) return;
    event.preventDefault();
    event.returnValue = '';
  });

  onBeforeRouteLeave(() => {
    if (!dirty.value) return true;
    return window.confirm(getMessage());
  });
}

/** Tracks edits to a modal via JSON snapshots; [requestClose]`(false)` confirms discard when dirty, `(true)` opens it. */
export function useModalDirtyGuard(options: {
  isOpen: Ref<boolean>;
  loading: Ref<boolean>;
  getSnapshot: () => unknown;
  onResetOnOpen?: () => void;
}) {
  const {t} = useI18n();
  const initialSnapshot = ref<string>('');

  const isDirty = computed(() => JSON.stringify(options.getSnapshot()) !== initialSnapshot.value);

  function captureSnapshot() {
    initialSnapshot.value = JSON.stringify(options.getSnapshot());
  }

  watch(options.isOpen, (open) => {
    if (!open) return;
    options.onResetOnOpen?.();
    nextTick(captureSnapshot);
  }, {immediate: true});

  function requestClose(next: boolean) {
    if (next) {
      options.isOpen.value = true;
      return;
    }
    if (options.loading.value) return;
    if (isDirty.value && !window.confirm(t('common.unsavedChanges.confirmDiscard'))) return;
    options.isOpen.value = false;
  }

  return {isDirty, requestClose, captureSnapshot};
}
