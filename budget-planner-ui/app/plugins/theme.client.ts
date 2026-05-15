import {useTheme} from "~/composables/use-theme";

export default defineNuxtPlugin(() => {
  const {restore} = useTheme();
  restore();
});
