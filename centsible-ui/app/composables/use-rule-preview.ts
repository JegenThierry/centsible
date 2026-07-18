import {watchDebounced} from "@vueuse/core";
import type {Ref} from "vue";
import type {RuleForm, RulePreviewMatch} from "~/models/rule/rule";
import {useRulesStore} from "~/stores/rulesStore";
import {createLatestRequestGate} from "~/utils/latest-request";

export function useRulePreview(params: {
  form: Ref<RuleForm>;
  enabled: Ref<boolean>;
}) {
  const store = useRulesStore();

  const matchedCount = ref<number | null>(null);
  const sample = ref<RulePreviewMatch[]>([]);
  const loading = ref(false);
  const failed = ref(false);
  const gate = createLatestRequestGate();

  watchDebounced(
    () => JSON.stringify({matchAll: params.form.value.matchAll, conditions: params.form.value.conditions}),
    async () => {
      failed.value = false;

      if (!params.enabled.value) {
        gate.supersede();
        matchedCount.value = null;
        sample.value = [];
        loading.value = false;
        return;
      }

      const isLatest = gate.begin();
      loading.value = true;
      try {
        const result = await store.previewRule(params.form.value);
        if (isLatest()) {
          matchedCount.value = result.matchedCount;
          sample.value = result.sample;
        }
      } catch {
        if (isLatest()) {
          failed.value = true;
          matchedCount.value = null;
          sample.value = [];
        }
      } finally {
        if (isLatest()) loading.value = false;
      }
    },
    {debounce: 350, immediate: true},
  );

  return {matchedCount, sample, loading, failed};
}
