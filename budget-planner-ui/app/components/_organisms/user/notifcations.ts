import {useToasts} from "~/services/toasts/toast-service";
import {hasKey, isString} from "~/composables/use-type-helper";

export function useUserNotifications() {
  const {success, error: showError} = useToasts();

  function onAvatarUpdateSuccess() {
    success('Profile picture updated', 'Your profile picture has been updated successfully.');
  }

  function onAvatarUpdateError(error?: unknown) {
    if (hasKey(error, 'message', isString)) {
      showError(
        'Profile picture update failed',
        error.message
      );
      return;
    }

    showError(
      'Profile picture update failed',
      'Failed to update your profile picture. Please try again later.'
    );
  }

  function onValidationError() {
    showError(
      'Validation failed',
      'Please check the form for errors.'
    );
  }

  function onProfileUpdateSuccess() {
    success(
      'Profile updated',
      'Your profile information has been saved successfully.'
    );
  }

  function onProfileUpdateError(error?: unknown) {
    if (hasKey(error, 'message', isString)) {
      showError(
        'Error updating profile',
        error.message
      );
      return;
    }
    showError(
      'Error updating profile',
      'Your profile information could not be updated. Please try again later.'
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
