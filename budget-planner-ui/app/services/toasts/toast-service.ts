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

  return {
    info,
    success,
    error
  }
}
