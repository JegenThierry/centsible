import {useToasts} from "~/services/toasts/toast-service";
import {hasKey, isString} from "~/composables/use-type-helper";

export function useUserNotifications() {
  const {success, error: showError} = useToasts();
  const {t} = useI18n();

  function onAvatarUpdateSuccess() {
    success(t('profile.toasts.avatarSuccessTitle'), t('profile.toasts.avatarSuccessBody'));
  }

  function onAvatarUpdateError(error?: unknown) {
    if (hasKey(error, 'message', isString)) {
      showError(
        t('profile.toasts.avatarErrorTitle'),
        error.message,
      );
      return;
    }

    showError(
      t('profile.toasts.avatarErrorTitle'),
      t('profile.toasts.avatarErrorFallback'),
    );
  }

  function onValidationError() {
    showError(
      t('profile.toasts.validationErrorTitle'),
      t('profile.toasts.validationErrorBody'),
    );
  }

  function onProfileUpdateSuccess() {
    success(
      t('profile.toasts.profileSuccessTitle'),
      t('profile.toasts.profileSuccessBody'),
    );
  }

  function onProfileUpdateError(error?: unknown) {
    if (hasKey(error, 'message', isString)) {
      showError(
        t('profile.toasts.profileErrorTitle'),
        error.message,
      );
      return;
    }
    showError(
      t('profile.toasts.profileErrorTitle'),
      t('profile.toasts.profileErrorFallback'),
    );
  }

  return {
    onAvatarUpdateSuccess,
    onAvatarUpdateError,
    onValidationError,
    onProfileUpdateSuccess,
    onProfileUpdateError,
  }
}
