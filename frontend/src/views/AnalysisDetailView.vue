<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import AppIcon from '@/components/AppIcon.vue'
import { useAnalysisStore } from '@/stores/analysis'

const route = useRoute()
const router = useRouter()
const analysisStore = useAnalysisStore()
const { currentDetail, loading } = storeToRefs(analysisStore)

const taskId = computed(() => String(route.params.taskId || ''))
const detail = computed(() => currentDetail.value)

const primaryAsset = computed(() => {
  const assets = detail.value?.task.visualAssets || []
  return assets.find((item) => item.url) || null
})

const extraAssets = computed(() => {
  const assets = detail.value?.task.visualAssets || []
  return assets.filter((item) => item.url && item !== primaryAsset.value)
})

const severityTone = computed(() => {
  const grade = (detail.value?.summary.grade || '').toUpperCase()
  if (grade === 'C0' || grade === 'G0') return 'background: var(--ok-100); color: var(--ok-700);'
  if (grade === 'C1' || grade === 'G1') return 'background: var(--brand-100); color: var(--brand-800);'
  if (grade === 'C2' || grade === 'G2') return 'background: var(--warn-100); color: var(--warn-700);'
  return 'background: var(--danger-100); color: var(--danger-700);'
})

const loadDetail = async () => {
  if (!taskId.value) return
  try {
    await analysisStore.fetchDetail(taskId.value)
  } catch (error) {
    console.error('Failed to load analysis detail', error)
  }
}

const goBack = () => {
  router.push('/analysis')
}

const formatDateTime = (value?: string) => {
  if (!value) return '--'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '--'
  return date.toLocaleString()
}

const formatPercent = (value?: number) => {
  if (typeof value !== 'number') return '--'
  return `${Math.round(value * 100)}%`
}

const formatMillis = (value?: number) => {
  if (typeof value !== 'number' || value <= 0) return '--'
  return `${(value / 1000).toFixed(2)}s`
}

onMounted(() => {
  void loadDetail()
})

watch(taskId, () => {
  void loadDetail()
})
</script>

<template>
  <div class="page">
    <div class="page-hello" style="margin-bottom: 18px">
      <div>
        <button class="btn btn-ghost btn-sm" style="margin-bottom: 10px" @click="goBack">
          <AppIcon name="chevron_right" :size="14" style="transform: rotate(180deg)" />
          Back to queue
        </button>
        <div class="micro">Analysis Detail</div>
        <h1 class="page-hello-title">{{ detail?.task.no || `TASK-${taskId}` }}</h1>
      </div>
      <div style="display: flex; gap: 10px; align-items: center; flex-wrap: wrap">
        <span class="chip" :style="severityTone">{{ detail?.summary.grade || '--' }}</span>
        <span class="chip chip-neutral">{{ detail?.task.status || 'PENDING' }}</span>
      </div>
    </div>

    <div v-if="loading" class="card" style="padding: 24px; color: var(--ink-3)">
      Loading analysis detail...
    </div>

    <div v-else-if="!detail" class="card" style="padding: 24px; color: var(--ink-3)">
      Analysis detail is unavailable.
    </div>

    <template v-else>
      <div style="display: grid; grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.9fr); gap: 16px; align-items: start">
        <section class="card" style="padding: 16px">
          <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 16px">
            <h3>Imaging Output</h3>
            <div class="card-head-actions">
              <span class="micro">{{ detail.image.sourceDevice || 'IMAGE' }}</span>
            </div>
          </div>

          <div
            style="aspect-ratio: 16 / 10; border-radius: 12px; overflow: hidden; background: var(--surface-sunk); border: 1px solid var(--line); display: grid; place-items: center"
          >
            <img
              v-if="primaryAsset?.url || detail.image.url"
              :src="primaryAsset?.url || detail.image.url"
              alt="analysis preview"
              style="width: 100%; height: 100%; object-fit: contain; background: var(--surface-sunk)"
            />
            <div v-else style="color: var(--ink-3)">No preview available</div>
          </div>

          <div v-if="extraAssets.length" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: 12px; margin-top: 14px">
            <div
              v-for="asset in extraAssets"
              :key="`${asset.type}-${asset.attachmentId || asset.label}`"
              class="card"
              style="padding: 10px"
            >
              <div style="aspect-ratio: 4 / 3; border-radius: 10px; overflow: hidden; background: var(--surface-sunk); margin-bottom: 8px">
                <img :src="asset.url" :alt="asset.label" style="width: 100%; height: 100%; object-fit: cover" />
              </div>
              <div style="font-size: 12px; font-weight: 600">{{ asset.label }}</div>
              <div class="micro">{{ asset.type }}</div>
            </div>
          </div>
        </section>

        <aside style="display: grid; gap: 16px">
          <section class="card" style="padding: 16px">
            <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
              <h3>Case Summary</h3>
            </div>
            <div style="display: grid; gap: 10px">
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <span class="micro">Patient</span>
                <span>{{ detail.patient.name || detail.patient.idMasked || '--' }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <span class="micro">Case</span>
                <span class="mono">{{ detail.caseInfo.no || '--' }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <span class="micro">Created</span>
                <span class="mono">{{ formatDateTime(detail.task.createdAt) }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <span class="micro">Completed</span>
                <span class="mono">{{ formatDateTime(detail.task.completedAt) }}</span>
              </div>
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <span class="micro">Latency</span>
                <span class="mono">{{ formatMillis(detail.task.inferenceMillis) }}</span>
              </div>
            </div>
          </section>

          <section class="card" style="padding: 16px">
            <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
              <h3>Clinical Summary</h3>
            </div>
            <div style="display: grid; gap: 12px">
              <div style="display: flex; gap: 10px; flex-wrap: wrap">
                <span class="chip" :style="severityTone">{{ detail.summary.grade }}</span>
                <span v-if="detail.summary.riskLevel" class="chip chip-neutral">{{ detail.summary.riskLevel }}</span>
                <span v-if="detail.summary.needsReview" class="chip" style="background: var(--warn-100); color: var(--warn-700)">Review required</span>
              </div>
              <div style="display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px">
                <div class="card" style="padding: 12px">
                  <div class="micro">Confidence</div>
                  <div style="font-size: 20px; font-weight: 600; margin-top: 6px">{{ formatPercent(detail.summary.confidence) }}</div>
                </div>
                <div class="card" style="padding: 12px">
                  <div class="micro">Uncertainty</div>
                  <div style="font-size: 20px; font-weight: 600; margin-top: 6px">{{ formatPercent(detail.summary.uncertainty) }}</div>
                </div>
                <div class="card" style="padding: 12px">
                  <div class="micro">Lesions</div>
                  <div style="font-size: 20px; font-weight: 600; margin-top: 6px">{{ detail.summary.lesionCount }}</div>
                </div>
                <div class="card" style="padding: 12px">
                  <div class="micro">Abnormal teeth</div>
                  <div style="font-size: 20px; font-weight: 600; margin-top: 6px">{{ detail.summary.abnormalToothCount }}</div>
                </div>
              </div>
              <div v-if="detail.summary.clinicalSummary" class="card" style="padding: 12px">
                <div class="micro">Clinical summary</div>
                <div style="margin-top: 6px; line-height: 1.7">{{ detail.summary.clinicalSummary }}</div>
              </div>
              <div v-if="detail.summary.followUpRecommendation" class="card" style="padding: 12px">
                <div class="micro">AI recommendation</div>
                <div style="margin-top: 6px; line-height: 1.7">{{ detail.summary.followUpRecommendation }}</div>
              </div>
            </div>
          </section>
        </aside>
      </div>

      <div style="display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; margin-top: 16px">
        <section class="card" style="padding: 16px">
          <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
            <h3>Risk Factors</h3>
          </div>
          <ul v-if="detail.summary.riskFactors.length" style="display: grid; gap: 8px; padding-left: 18px; margin: 0">
            <li v-for="item in detail.summary.riskFactors" :key="item">{{ item }}</li>
          </ul>
          <div v-else style="color: var(--ink-3)">No structured risk factors.</div>
        </section>

        <section class="card" style="padding: 16px">
          <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
            <h3>Treatment Plan</h3>
          </div>
          <div v-if="detail.summary.treatmentPlan.length" style="display: grid; gap: 10px">
            <div
              v-for="item in detail.summary.treatmentPlan"
              :key="`${item.priority}-${item.title}`"
              class="card"
              style="padding: 12px"
            >
              <div style="display: flex; justify-content: space-between; gap: 12px; margin-bottom: 6px">
                <strong>{{ item.title }}</strong>
                <span class="chip chip-neutral">{{ item.priority }}</span>
              </div>
              <div style="line-height: 1.7; color: var(--ink-2)">{{ item.details }}</div>
            </div>
          </div>
          <div v-else style="color: var(--ink-3)">No treatment plan output.</div>
        </section>

        <section class="card" style="padding: 16px">
          <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
            <h3>Timeline</h3>
          </div>
          <div v-if="detail.timeline.length" style="display: grid; gap: 10px">
            <div v-for="node in detail.timeline" :key="node.code" class="card" style="padding: 12px">
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <strong>{{ node.name }}</strong>
                <span class="chip chip-neutral">{{ node.status }}</span>
              </div>
              <div class="micro" style="margin-top: 6px">{{ formatDateTime(node.time) }}</div>
              <div v-if="node.description" style="margin-top: 6px; color: var(--ink-2)">{{ node.description }}</div>
            </div>
          </div>
          <div v-else style="color: var(--ink-3)">No timeline available.</div>
        </section>
      </div>

      <div style="display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); gap: 16px; margin-top: 16px">
        <section class="card" style="padding: 16px">
          <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
            <h3>Lesions</h3>
            <div class="card-head-actions">
              <span class="micro">{{ detail.summary.lesions.length }}</span>
            </div>
          </div>
          <div v-if="detail.summary.lesions.length" style="display: grid; gap: 10px">
            <div
              v-for="item in detail.summary.lesions"
              :key="item.id"
              class="card"
              style="padding: 12px"
            >
              <div style="display: flex; justify-content: space-between; gap: 12px; align-items: start">
                <div>
                  <strong>{{ item.toothCode || item.id }}</strong>
                  <div class="micro">{{ item.severityCode || 'UNKNOWN' }}</div>
                </div>
                <div style="display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end">
                  <span v-if="item.confidence != null" class="chip chip-neutral">{{ formatPercent(item.confidence) }}</span>
                  <span v-if="item.uncertainty != null" class="chip chip-neutral">U {{ formatPercent(item.uncertainty) }}</span>
                </div>
              </div>
              <div v-if="item.summary" style="margin-top: 8px; line-height: 1.7">{{ item.summary }}</div>
              <div v-if="item.treatmentSuggestion" style="margin-top: 6px; color: var(--ink-2)">
                {{ item.treatmentSuggestion }}
              </div>
            </div>
          </div>
          <div v-else style="color: var(--ink-3)">No lesion records.</div>
        </section>

        <section class="card" style="padding: 16px">
          <div class="card-head" style="padding: 0 0 12px; border-bottom: 1px solid var(--line); margin-bottom: 12px">
            <h3>Evidence</h3>
            <div class="card-head-actions">
              <span class="micro">{{ detail.summary.citations.length }}</span>
            </div>
          </div>
          <div v-if="detail.summary.citations.length" style="display: grid; gap: 10px">
            <div
              v-for="citation in detail.summary.citations"
              :key="`${citation.index}-${citation.title}`"
              class="card"
              style="padding: 12px"
            >
              <div style="display: flex; justify-content: space-between; gap: 12px">
                <strong>{{ citation.title }}</strong>
                <span v-if="citation.score != null" class="micro">{{ formatPercent(citation.score) }}</span>
              </div>
              <div v-if="citation.excerpt" style="margin-top: 8px; color: var(--ink-2); line-height: 1.7">{{ citation.excerpt }}</div>
              <div v-if="citation.sourceUri" class="micro" style="margin-top: 8px">{{ citation.sourceUri }}</div>
            </div>
          </div>
          <div v-else style="color: var(--ink-3)">No citations were returned.</div>
        </section>
      </div>
    </template>
  </div>
</template>
