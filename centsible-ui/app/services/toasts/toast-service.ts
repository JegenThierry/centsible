export function useToasts() {
  const toast = useToast();

  function createToast(title: string, body: string, variant: 'success' | 'error' | 'info') {
    toast.add({
      title: title,
      description: body,
      color: variant,
    })
  }

  function info(title: string, body: string) {
    createToast(title, body, 'info');
  }

  function success(title: string, body: string) {
    createToast(title, body, 'success');
  }

  function error(title: string, body: string) {
    createToast(title, body, 'error');
  }

  /**
   * Toast carrying a single inline action button (e.g. an undo affordance). Clicking the button runs
   * [onAction] and dismisses the toast; the toast otherwise auto-expires after [durationMs].
   */
  function action(
    variant: 'success' | 'error' | 'info',
    title: string,
    body: string,
    actionLabel: string,
    onAction: () => void,
    durationMs = 6000,
  ) {
    const created = toast.add({
      title,
      description: body,
      color: variant,
      orientation: 'horizontal',
      duration: durationMs,
      actions: [{
        label: actionLabel,
        color: 'neutral',
        variant: 'outline',
        onClick: () => {
          onAction();
          toast.remove(created.id);
        },
      }],
    });
  }

  return {
    info,
    success,
    error,
    action,
  }
}
