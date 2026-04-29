<template>
  <div class="page diagnosis-page">
    <div class="page-hello diagnosis-hello">
      <div>
        <div class="micro">Oral Imaging Intelligence</div>
        <h1 class="page-hello-title">AI 诊断分析</h1>
        <p class="diagnosis-subtitle">
          上传口腔影像后，系统将模拟完成牙体识别、风险分层与诊疗建议汇总。
        </p>
      </div>

      <div class="diagnosis-hello-actions">
        <span class="diagnosis-meta">支持 JPG / PNG / DICOM</span>
        <span class="diagnosis-meta">单文件 ≤ 50MB</span>
        <button class="diag-btn diag-btn-ghost" type="button" :disabled="state === 'empty'" @click="reset">
          重新上传
        </button>
        <button class="diag-btn diag-btn-primary" type="button" @click="triggerUpload">
          上传影像
        </button>
      </div>
    </div>

    <div class="content-grid">
      <div class="left-column">
        <section class="panel image-panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">影像分析</div>
              <p class="panel-subtitle">支持拖拽上传，自动进入 AI 扫描流程。</p>
            </div>
            <div class="panel-head-actions">
              <span class="state-chip" :class="state">{{ stateLabel }}</span>
              <button
                v-if="state === 'result'"
                class="icon-btn"
                type="button"
                title="重新上传"
                @click="reset"
              >
                <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
                  <path
                    d="M3 8a5 5 0 1 1 1.5 3.5M3 12V8h4"
                    stroke="currentColor"
                    stroke-width="1.6"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </button>
            </div>
          </div>

          <div
            class="image-stage"
            @dragover.prevent="onDragOver"
            @dragleave="onDragLeave"
            @drop.prevent="onDrop"
          >
            <div
              v-if="state === 'empty'"
              class="empty-state"
              :class="{ 'drag-over': isDragOver }"
              @click="triggerUpload"
            >
              <div class="empty-state-grid">
                <div class="empty-visual" aria-hidden="true">
                  <div class="empty-drop-chip">
                    <span class="empty-drop-chip-dot"></span>
                    <span>拖拽到此区域</span>
                  </div>
                </div>

                <div class="empty-copy">
                  <div class="empty-kicker">AI 影像采集入口</div>
                  <p class="empty-title">点击或拖拽上传口腔影像</p>
                  <p class="empty-hint">支持 JPG、PNG、DICOM，上传后自动进入 AI 扫描流程。</p>

                  <div class="empty-specs">
                    <span class="empty-spec-chip">JPG / PNG / DICOM</span>
                    <span class="empty-spec-chip">单文件 ≤ 50MB</span>
                    <span class="empty-spec-chip">自动进入分析</span>
                  </div>

                  <button class="upload-btn" type="button" @click.stop="triggerUpload">
                    <span class="upload-btn-icon">+</span>
                    <span>上传影像</span>
                  </button>
                </div>
              </div>
            </div>

            <div v-else-if="state === 'scanning'" class="scan-state">
              <div class="scan-box">
                <img :src="imagePreview" alt="待分析影像" class="stage-image" />
                <div class="scan-overlay"></div>
                <div class="scan-line"></div>
                <div class="scan-trail"></div>
                <div class="scan-corner tl"></div>
                <div class="scan-corner tr"></div>
                <div class="scan-corner bl"></div>
                <div class="scan-corner br"></div>
              </div>
              <div class="scan-progress">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: `${scanProgress}%` }"></div>
                </div>
                <span class="progress-text">AI 分析中... {{ scanProgress }}%</span>
              </div>
            </div>

            <div v-else class="result-state">
              <div class="result-toolbar">
                <div class="view-switch" role="tablist" aria-label="影像视图切换">
                  <button
                    v-for="mode in viewModes"
                    :key="mode.key"
                    class="view-switch-btn"
                    :class="{ active: activeView === mode.key }"
                    type="button"
                    :aria-pressed="activeView === mode.key"
                    @click="activeView = mode.key"
                  >
                    <span class="view-switch-icon" v-html="mode.icon"></span>
                    <span>{{ mode.label }}</span>
                  </button>
                </div>

                <div class="view-note">
                  <span class="view-note-label">{{ activeViewMeta.label }}</span>
                  <p>{{ activeViewMeta.description }}</p>
                </div>
              </div>

              <div class="result-box advanced-result-box">
                <template v-if="activeView === 'compare'">
                  <div class="compare-layout">
                    <div class="compare-pane">
                      <span class="compare-badge">原始影像</span>
                      <img :src="imagePreview" alt="原始口腔影像" class="stage-image" />
                    </div>

                    <div class="compare-divider">
                      <span>AI 对比</span>
                    </div>

                    <div class="compare-pane compare-pane-annotated">
                      <span class="compare-badge compare-badge-accent">AI 标注</span>
                      <template v-if="useContainedDemoCase">
                        <div class="stage-media-shell compare-media-shell">
                          <div class="stage-media-frame">
                            <img :src="resultImagePreview" alt="AI 标注结果影像" class="stage-image stage-image-contained" />
                            <div class="annotation-layer compare-overlay">
                              <button
                                v-for="annotation in activeAnnotations"
                                :key="annotation.id"
                                class="annotation-region"
                                :class="{ focused: focusAnnotation?.id === annotation.id }"
                                :style="annotationStyle(annotation)"
                                type="button"
                                @mouseenter="hoveredAnno = annotation.id"
                                @mouseleave="hoveredAnno = null"
                              >
                                <span class="annotation-region-outline"></span>
                                <span class="annotation-tag">
                                  <span class="annotation-tag-id">{{ regionCode(annotation.id) }}</span>
                                  <span class="annotation-tag-text">{{ annotation.finding }}</span>
                                </span>
                              </button>
                            </div>

                            <div class="stage-legend compare-legend">
                              <div class="stage-legend-head">
                                <span>风险图例</span>
                                <small>编号与明细表同步</small>
                              </div>
                              <div class="stage-legend-list">
                                <div v-for="item in annotationLegend" :key="item.key" class="stage-legend-item">
                                  <span class="stage-legend-dot" :style="{ backgroundColor: item.color }"></span>
                                  <span>{{ item.label }}</span>
                                </div>
                              </div>
                            </div>
                          </div>
                        </div>
                      </template>

                      <template v-else>
                        <img :src="resultImagePreview" alt="AI 标注结果影像" class="stage-image" />
                        <div class="annotation-layer compare-overlay">
                          <button
                            v-for="annotation in activeAnnotations"
                            :key="annotation.id"
                            class="annotation-region"
                            :class="{ focused: focusAnnotation?.id === annotation.id }"
                            :style="annotationStyle(annotation)"
                            type="button"
                            @mouseenter="hoveredAnno = annotation.id"
                            @mouseleave="hoveredAnno = null"
                          >
                            <span class="annotation-region-outline"></span>
                            <span class="annotation-tag">
                              <span class="annotation-tag-id">{{ regionCode(annotation.id) }}</span>
                              <span class="annotation-tag-text">{{ annotation.finding }}</span>
                            </span>
                          </button>
                        </div>

                        <div class="stage-legend compare-legend">
                          <div class="stage-legend-head">
                            <span>风险图例</span>
                            <small>编号与明细表同步</small>
                          </div>
                          <div class="stage-legend-list">
                            <div v-for="item in annotationLegend" :key="item.key" class="stage-legend-item">
                              <span class="stage-legend-dot" :style="{ backgroundColor: item.color }"></span>
                              <span>{{ item.label }}</span>
                            </div>
                          </div>
                        </div>
                      </template>
                    </div>
                  </div>
                </template>

                <template v-else>
                  <template v-if="useContainedDemoCase">
                    <div class="stage-media-shell">
                      <div class="stage-media-frame">
                        <img
                          :src="activeView === 'original' ? imagePreview : resultImagePreview"
                          :alt="activeView === 'original' ? '原始口腔影像' : 'AI 诊断结果影像'"
                          class="stage-image stage-image-contained"
                        />

                        <div v-if="activeView === 'annotation'" class="annotation-layer">
                          <button
                            v-for="annotation in activeAnnotations"
                            :key="annotation.id"
                            class="annotation-region"
                            :class="{ focused: focusAnnotation?.id === annotation.id }"
                            :style="annotationStyle(annotation)"
                            type="button"
                            @mouseenter="hoveredAnno = annotation.id"
                            @mouseleave="hoveredAnno = null"
                          >
                            <span class="annotation-region-outline"></span>
                            <span class="annotation-tag">
                              <span class="annotation-tag-id">{{ regionCode(annotation.id) }}</span>
                              <span class="annotation-tag-text">{{ annotation.finding }}</span>
                            </span>
                          </button>
                        </div>

                        <div v-if="activeView === 'heatmap'" class="heatmap-layer">
                          <div
                            v-for="annotation in activeAnnotations"
                            :key="annotation.id"
                            class="heat-cloud"
                            :style="heatStyle(annotation)"
                          ></div>
                          <button
                            v-for="annotation in activeAnnotations"
                            :key="`heat-${annotation.id}`"
                            class="heat-anchor"
                            :style="heatAnchorStyle(annotation)"
                            type="button"
                            @mouseenter="hoveredAnno = annotation.id"
                            @mouseleave="hoveredAnno = null"
                          >
                            {{ regionCode(annotation.id) }}
                          </button>
                        </div>

                        <div v-if="activeView === 'annotation'" class="stage-legend">
                          <div class="stage-legend-head">
                            <span>风险图例</span>
                            <small>编号与明细表同步</small>
                          </div>
                          <div class="stage-legend-list">
                            <div v-for="item in annotationLegend" :key="item.key" class="stage-legend-item">
                              <span class="stage-legend-dot" :style="{ backgroundColor: item.color }"></span>
                              <span>{{ item.label }}</span>
                            </div>
                          </div>
                        </div>

                        <div v-if="activeView === 'heatmap'" class="heatmap-scale">
                          <span>模型关注度</span>
                          <div class="heatmap-scale-bar"></div>
                          <div class="heatmap-scale-labels">
                            <small>低</small>
                            <small>高</small>
                          </div>
                        </div>
                      </div>
                    </div>
                  </template>

                  <template v-else>
                    <img
                      :src="activeView === 'original' ? imagePreview : resultImagePreview"
                      :alt="activeView === 'original' ? '原始口腔影像' : 'AI 诊断结果影像'"
                      class="stage-image"
                    />

                    <div v-if="activeView === 'annotation'" class="annotation-layer">
                      <button
                        v-for="annotation in activeAnnotations"
                        :key="annotation.id"
                        class="annotation-region"
                        :class="{ focused: focusAnnotation?.id === annotation.id }"
                        :style="annotationStyle(annotation)"
                        type="button"
                        @mouseenter="hoveredAnno = annotation.id"
                        @mouseleave="hoveredAnno = null"
                      >
                        <span class="annotation-region-outline"></span>
                        <span class="annotation-tag">
                          <span class="annotation-tag-id">{{ regionCode(annotation.id) }}</span>
                          <span class="annotation-tag-text">{{ annotation.finding }}</span>
                        </span>
                      </button>
                    </div>

                    <div v-if="activeView === 'heatmap'" class="heatmap-layer">
                      <div
                        v-for="annotation in activeAnnotations"
                        :key="annotation.id"
                        class="heat-cloud"
                        :style="heatStyle(annotation)"
                      ></div>
                      <button
                        v-for="annotation in activeAnnotations"
                        :key="`heat-${annotation.id}`"
                        class="heat-anchor"
                        :style="heatAnchorStyle(annotation)"
                        type="button"
                        @mouseenter="hoveredAnno = annotation.id"
                        @mouseleave="hoveredAnno = null"
                      >
                        {{ regionCode(annotation.id) }}
                      </button>
                    </div>

                    <div v-if="activeView === 'annotation'" class="stage-legend">
                      <div class="stage-legend-head">
                        <span>风险图例</span>
                        <small>编号与明细表同步</small>
                      </div>
                      <div class="stage-legend-list">
                        <div v-for="item in annotationLegend" :key="item.key" class="stage-legend-item">
                          <span class="stage-legend-dot" :style="{ backgroundColor: item.color }"></span>
                          <span>{{ item.label }}</span>
                        </div>
                      </div>
                    </div>

                    <div v-if="activeView === 'heatmap'" class="heatmap-scale">
                      <span>模型关注度</span>
                      <div class="heatmap-scale-bar"></div>
                      <div class="heatmap-scale-labels">
                        <small>低</small>
                        <small>高</small>
                      </div>
                    </div>
                  </template>
                </template>

                <div
                  v-if="focusAnnotation && activeView !== 'original'"
                  class="focus-card"
                  :class="{ compare: activeView === 'compare' }"
                >
                  <div class="focus-card-head">
                    <span class="focus-card-code">{{ regionCode(focusAnnotation.id) }}</span>
                    <span class="focus-card-risk" :style="riskBadgeStyle(focusAnnotation.level)">
                      {{ riskLabelFor(focusAnnotation.level) }}
                    </span>
                  </div>
                  <strong>{{ focusAnnotation.finding }}</strong>
                  <p>{{ focusAnnotation.tooth }} · {{ focusAnnotation.region }}</p>
                  <small>置信度 {{ focusAnnotation.confidence.toFixed(1) }}%</small>
                </div>
              </div>
            </div>

            <input
              ref="fileInput"
              type="file"
              accept=".jpg,.jpeg,.png,.dcm,application/dicom,image/*"
              hidden
              @change="onFileSelected"
            />
          </div>
        </section>

        <section class="panel summary-panel">
          <div class="panel-head summary-head">
            <div class="panel-title">AI 检测结果概览</div>
            <span class="summary-tip">结果仅供临床辅助参考</span>
          </div>

          <div class="summary-grid">
            <article class="summary-card">
              <span class="summary-label">牙体检测</span>
              <div class="summary-value">
                <template v-if="state === 'result'">
                  <strong>{{ activeResults.teethDetected }}</strong>
                  <small>/ {{ activeResults.totalTeeth ?? 28 }} 颗</small>
                </template>
                <template v-else>--</template>
              </div>
              <span class="summary-status">{{ state === 'result' ? '全部识别' : '等待分析' }}</span>
            </article>

            <article class="summary-card warning">
              <span class="summary-label">异常区域</span>
              <div class="summary-value">
                <template v-if="state === 'result'">
                  <strong>{{ activeResults.anomalies }}</strong>
                  <small>处</small>
                </template>
                <template v-else>--</template>
              </div>
              <span class="summary-status">{{ state === 'result' ? '需重点关注' : '等待分析' }}</span>
            </article>

            <article class="summary-card">
              <span class="summary-label">严重程度</span>
              <div class="summary-value">
                <strong v-if="state === 'result'" class="severity">{{ activeResults.severity }}</strong>
                <template v-else>--</template>
              </div>
              <span class="summary-status">综合评估</span>
            </article>

            <article class="summary-card">
              <span class="summary-label">置信度评估</span>
              <div class="summary-value">
                <template v-if="state === 'result'">
                  <strong>{{ activeResults.confidence }}</strong>
                  <small>%</small>
                </template>
                <template v-else>--</template>
              </div>
              <span class="summary-status">AI 置信度</span>
            </article>
          </div>

          <p class="summary-note">
            * 结果由 AI 模型辅助生成，仅供临床参考，请结合医生诊断结论。
          </p>
        </section>
      </div>

      <div class="right-column">
        <section class="panel side-panel">
          <div class="panel-head compact">
            <div class="panel-title">综合风险指数</div>
            <button class="icon-btn" type="button" aria-label="说明">
              <svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.2">
                <circle cx="8" cy="8" r="7" />
                <path d="M8 5v0M8 7v4" />
              </svg>
            </button>
          </div>

          <div class="score-ring">
            <svg viewBox="0 0 120 120" aria-hidden="true">
              <defs>
                <linearGradient id="diagScoreGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="#ff4757" />
                  <stop offset="35%" stop-color="#ff9f43" />
                  <stop offset="70%" stop-color="#ffd24a" />
                  <stop offset="100%" stop-color="#35f8ff" />
                </linearGradient>
              </defs>
              <circle cx="60" cy="60" r="52" stroke="#102857" stroke-width="8" fill="none" />
              <circle
                cx="60"
                cy="60"
                r="52"
                stroke="url(#diagScoreGrad)"
                stroke-width="8"
                fill="none"
                stroke-linecap="round"
                :stroke-dasharray="scoreCircumference"
                :stroke-dashoffset="scoreOffset"
                transform="rotate(-90 60 60)"
              />
            </svg>
            <div class="score-center">
              <strong>{{ state === 'result' ? activeResults.overallScore : '--' }}</strong>
              <span>{{ state === 'result' ? '/100' : '待评估' }}</span>
            </div>
          </div>

          <div class="risk-pill" :class="riskClass">{{ state === 'result' ? riskLabel : '待评估' }}</div>
          <p class="side-copy">
            {{
              state === 'result'
                ? '存在多个需要处理的口腔问题，建议尽快结合临床情况制定治疗方案。'
                : '上传影像后将生成 AI 风险评级与综合风险指数。'
            }}
          </p>
        </section>

        <section class="panel side-panel">
          <div class="panel-head compact">
            <div class="panel-title">风险等级分布</div>
          </div>

          <div class="donut-wrap">
            <svg viewBox="0 0 120 120" class="donut-chart" aria-hidden="true">
              <circle cx="60" cy="60" r="45" stroke="#102857" stroke-width="14" fill="none" />
              <circle
                v-for="item in riskSegments"
                v-show="state === 'result' && item.length > 0"
                :key="item.key"
                cx="60"
                cy="60"
                r="45"
                :stroke="item.color"
                stroke-width="14"
                fill="none"
                :stroke-dasharray="`${item.length} ${donutCircumference}`"
                :stroke-dashoffset="item.offset"
                transform="rotate(-90 60 60)"
                stroke-linecap="round"
              />
            </svg>
            <div class="donut-center">
              <strong>{{ state === 'result' ? activeResults.anomalies : 0 }}</strong>
              <span>异常总数</span>
            </div>
          </div>

          <div class="risk-list">
            <div v-for="item in riskLegend" :key="item.key" class="risk-row">
              <div class="risk-label">
                <span class="risk-dot" :style="{ backgroundColor: item.color }"></span>
                <span>{{ item.label }}</span>
              </div>
              <strong>{{ state === 'result' ? `${item.count} (${item.percent})` : '--' }}</strong>
            </div>
          </div>
        </section>

        <section class="panel side-panel">
          <div class="panel-head compact">
            <div class="panel-title">AI 诊断结论</div>
            <span class="sparkle">✦</span>
          </div>

          <div v-if="state === 'result'" class="diagnosis-list">
            <div
              v-for="(item, index) in activeResults.diagnoses"
              :key="`${item.level}-${index}`"
              class="diagnosis-item"
            >
              <span class="diag-icon" :style="{ color: diagColor(item.level) }">
                <svg viewBox="0 0 14 14" fill="none" aria-hidden="true">
                  <circle cx="7" cy="7" r="5" :stroke="diagColor(item.level)" stroke-width="1.2" />
                  <path
                    v-if="item.level === 'high'"
                    d="M5 5l4 4M9 5l-4 4"
                    :stroke="diagColor(item.level)"
                    stroke-width="1.2"
                    stroke-linecap="round"
                  />
                  <path
                    v-else-if="item.level === 'medium'"
                    d="M7 4.8v3.2M7 10v.01"
                    :stroke="diagColor(item.level)"
                    stroke-width="1.2"
                    stroke-linecap="round"
                  />
                  <path
                    v-else
                    d="M4.8 7.1l1.4 1.5L9.4 5.7"
                    :stroke="diagColor(item.level)"
                    stroke-width="1.2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                </svg>
              </span>
              <span>{{ item.text }}</span>
            </div>
          </div>
          <div v-else class="diagnosis-empty">等待影像上传后生成 AI 诊断结论</div>

          <button class="report-btn" type="button" :disabled="!hasResult">
            生成诊疗建议报告
          </button>
        </section>
      </div>
    </div>

    <section class="panel detail-panel">
      <div class="panel-head detail-head">
        <div>
          <div class="panel-title">异常区域明细</div>
          <p class="panel-subtitle">编号与 AI 标注视图联动，便于复核异常区域、风险等级与处置建议。</p>
        </div>
        <span class="detail-count">{{ state === 'result' ? `${activeAnnotations.length} 项异常` : '等待分析' }}</span>
      </div>

      <div v-if="state === 'result'" class="detail-table-wrap">
        <table class="detail-table">
          <thead>
            <tr>
              <th>编号</th>
              <th>牙位 / 区域</th>
              <th>疑似异常</th>
              <th>风险等级</th>
              <th>置信度</th>
              <th>处理建议</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="annotation in activeAnnotations"
              :key="`detail-${annotation.id}`"
              :class="{ active: focusAnnotation?.id === annotation.id }"
              @mouseenter="hoveredAnno = annotation.id"
              @mouseleave="hoveredAnno = null"
            >
              <td>
                <span class="table-code">{{ regionCode(annotation.id) }}</span>
              </td>
              <td>
                <div class="table-main">{{ annotation.tooth }}</div>
                <div class="table-sub">{{ annotation.region }}</div>
              </td>
              <td>{{ annotation.finding }}</td>
              <td>
                <span class="risk-badge" :style="riskBadgeStyle(annotation.level)">
                  {{ riskLabelFor(annotation.level) }}
                </span>
              </td>
              <td>{{ annotation.confidence.toFixed(1) }}%</td>
              <td class="suggestion-cell">{{ annotation.recommendation }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="detail-empty">
        上传影像并完成 AI 分析后，将在此展示异常区域明细、风险等级与处理建议。
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref } from 'vue'
import {
  DEMO_CASES,
  MOCK_ANNOTATIONS,
  MOCK_RESULTS,
  RISK_COLORS,
  RISK_LABELS,
  type AnalysisResults,
  type Annotation,
  type DemoCase,
  type DiagState,
  type RiskLevel,
} from './diagnosis'

type ViewMode = 'annotation' | 'original' | 'heatmap' | 'compare'

const state = ref<DiagState>('empty')
const isDragOver = ref(false)
const imagePreview = ref('')
const scanProgress = ref(0)
const hoveredAnno = ref<number | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)
const activeView = ref<ViewMode>('annotation')
const activeDemoCase = ref<DemoCase | null>(null)

const viewModes: Array<{ key: ViewMode; label: string; description: string; icon: string }> = [
  {
    key: 'annotation',
    label: 'AI标注',
    description: '默认显示异常区域编号、风险颜色标注与图例说明，便于快速定位病灶。',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M3 13.5V4.5A1.5 1.5 0 0 1 4.5 3h9A1.5 1.5 0 0 1 15 4.5v9A1.5 1.5 0 0 1 13.5 15h-9A1.5 1.5 0 0 1 3 13.5Z" stroke="currentColor" stroke-width="1.4"/><path d="M6 11.5 8 9.5l1.5 1.5L12 8.5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/></svg>`,
  },
  {
    key: 'original',
    label: '原始影像',
    description: '保留原始口腔影像，用于人工复核和与 AI 结果进行基线对照。',
    icon: `<svg viewBox="0 0 18 18" fill="none"><rect x="2.5" y="3" width="13" height="12" rx="2" stroke="currentColor" stroke-width="1.4"/><path d="m5.5 11 2.2-2.2 1.8 1.8 2.8-3 1.2 1.4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round"/><circle cx="6.2" cy="6.4" r="1.1" fill="currentColor"/></svg>`,
  },
  {
    key: 'heatmap',
    label: '热力图',
    description: '以半透明渐变热区呈现模型关注区域，不遮挡原始影像关键结构。',
    icon: `<svg viewBox="0 0 18 18" fill="none"><path d="M9 2.5c1.8 2 3.8 4.2 3.8 7a3.8 3.8 0 1 1-7.6 0c0-1.9 1-3.4 2.4-5" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/><circle cx="9" cy="10" r="1.8" fill="currentColor" fill-opacity=".45"/></svg>`,
  },
  {
    key: 'compare',
    label: '对比视图',
    description: '并排展示原始影像与 AI 标注结果，便于赛题演示和临床沟通。',
    icon: `<svg viewBox="0 0 18 18" fill="none"><rect x="2.5" y="3" width="5.5" height="12" rx="1.6" stroke="currentColor" stroke-width="1.4"/><rect x="10" y="3" width="5.5" height="12" rx="1.6" stroke="currentColor" stroke-width="1.4"/><path d="M9 4v10" stroke="currentColor" stroke-width="1.2" stroke-dasharray="2 2"/></svg>`,
  },
]

const activeResults = computed<AnalysisResults>(() =>
  activeDemoCase.value?.results ?? MOCK_RESULTS,
)

const activeAnnotations = computed<Annotation[]>(() =>
  activeDemoCase.value?.annotations ?? MOCK_ANNOTATIONS,
)

const resultImagePreview = computed(() =>
  activeDemoCase.value && state.value === 'result' ? activeDemoCase.value.resultImageUrl : imagePreview.value,
)

const useContainedDemoCase = computed(
  () => state.value === 'result' && activeDemoCase.value?.displayMode === 'contain',
)

let previewObjectUrl: string | null = null
let scanFrameId = 0
let finishTimerId = 0

const scoreCircumference = 2 * Math.PI * 52
const donutCircumference = 2 * Math.PI * 45
const hasResult = computed(() => state.value === 'result')
const activeViewMeta = computed(
  () => viewModes.find((item) => item.key === activeView.value) ?? viewModes[0],
)

const stateLabel = computed(() => {
  switch (state.value) {
    case 'scanning':
      return '分析中'
    case 'result':
      return '已完成'
    default:
      return '等待上传'
  }
})

const scoreOffset = computed(() => {
  if (state.value !== 'result') return scoreCircumference
  return scoreCircumference - (activeResults.value.overallScore / 100) * scoreCircumference
})

const riskClass = computed(() => {
  if (state.value !== 'result') return 'pending'
  if (activeResults.value.overallScore >= 80) return 'high-risk'
  if (activeResults.value.overallScore >= 50) return 'medium-risk'
  return 'low-risk'
})

const riskLabel = computed(() => {
  if (activeResults.value.overallScore >= 80) return '高风险'
  if (activeResults.value.overallScore >= 50) return '中风险'
  return '低风险'
})

const totalRisk = computed(
  () =>
    activeResults.value.riskHigh +
    activeResults.value.riskMedium +
    activeResults.value.riskLow +
    activeResults.value.riskOther,
)

const riskLegend = computed(() => {
  const items = [
    { key: 'high', label: '高风险', count: activeResults.value.riskHigh, color: '#ff4757' },
    { key: 'medium', label: '中风险', count: activeResults.value.riskMedium, color: '#ff9f43' },
    { key: 'low', label: '低风险', count: activeResults.value.riskLow, color: '#35f8ff' },
    { key: 'other', label: '其他异常', count: activeResults.value.riskOther, color: '#a66bff' },
  ]

  return items.map((item) => ({
    ...item,
    percent: totalRisk.value === 0 ? '0%' : `${((item.count / totalRisk.value) * 100).toFixed(1)}%`,
  }))
})

const riskSegments = computed(() => {
  let consumed = 0

  return riskLegend.value.map((item) => {
    const length = totalRisk.value === 0 ? 0 : (item.count / totalRisk.value) * donutCircumference
    const segment = {
      ...item,
      length,
      offset: -consumed,
    }
    consumed += length
    return segment
  })
})

const annotationLegend = computed(() => riskLegend.value.filter((item) => item.count > 0))

const focusAnnotation = computed<Annotation | null>(() => {
  if (!activeAnnotations.value.length) return null
  return activeAnnotations.value.find((item) => item.id === hoveredAnno.value) ?? activeAnnotations.value[0]
})

function diagColor(level: RiskLevel) {
  return RISK_COLORS[level]
}

function riskLabelFor(level: RiskLevel) {
  return RISK_LABELS[level]
}

function regionCode(id: number) {
  return `#${String(id).padStart(2, '0')}`
}

function riskBadgeStyle(level: RiskLevel) {
  const color = diagColor(level)

  return {
    color,
    borderColor: `${color}33`,
    background: `${color}1f`,
  }
}

function annotationStyle(annotation: Annotation) {
  return {
    left: `${annotation.left}%`,
    top: `${annotation.top}%`,
    width: `${annotation.width}%`,
    height: `${annotation.height}%`,
    transform: `rotate(${annotation.rotate ?? 0}deg)`,
    '--accent': diagColor(annotation.level),
  }
}

function heatStyle(annotation: Annotation) {
  return {
    left: `${annotation.heatLeft}%`,
    top: `${annotation.heatTop}%`,
    width: `${annotation.heatWidth}%`,
    height: `${annotation.heatHeight}%`,
    opacity: annotation.heatOpacity,
    '--heat': diagColor(annotation.level),
  }
}

function heatAnchorStyle(annotation: Annotation) {
  return {
    left: `${annotation.heatLeft + annotation.heatWidth / 2}%`,
    top: `${annotation.heatTop + annotation.heatHeight / 2}%`,
    '--accent': diagColor(annotation.level),
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

function onFileSelected(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (file) startScan(file)
  target.value = ''
}

function onDragOver() {
  isDragOver.value = true
}

function onDragLeave() {
  isDragOver.value = false
}

function onDrop(event: DragEvent) {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) startScan(file)
}

function startScan(file: File) {
  cleanupAnimation()
  revokePreviewUrl()
  hoveredAnno.value = null
  activeView.value = 'annotation'
  activeDemoCase.value = resolveDemoCase(file)
  imagePreview.value = buildPreviewUrl(file)
  scanProgress.value = 0
  state.value = 'scanning'
  animateScan()
}

function animateScan() {
  const duration = 3200
  const startTime = performance.now()

  const tick = (now: number) => {
    const elapsed = now - startTime
    scanProgress.value = Math.min(100, Math.round((elapsed / duration) * 100))

    if (scanProgress.value < 100) {
      scanFrameId = window.requestAnimationFrame(tick)
      return
    }

    finishTimerId = window.setTimeout(() => {
      state.value = 'result'
    }, 260)
  }

  scanFrameId = window.requestAnimationFrame(tick)
}

function reset() {
  cleanupAnimation()
  revokePreviewUrl()
  state.value = 'empty'
  imagePreview.value = ''
  scanProgress.value = 0
  hoveredAnno.value = null
  activeView.value = 'annotation'
  isDragOver.value = false
  activeDemoCase.value = null

  if (fileInput.value) {
    fileInput.value.value = ''
  }
}

function cleanupAnimation() {
  if (scanFrameId) {
    window.cancelAnimationFrame(scanFrameId)
    scanFrameId = 0
  }

  if (finishTimerId) {
    window.clearTimeout(finishTimerId)
    finishTimerId = 0
  }
}

function revokePreviewUrl() {
  if (previewObjectUrl) {
    URL.revokeObjectURL(previewObjectUrl)
    previewObjectUrl = null
  }
}

function buildPreviewUrl(file: File) {
  if (file.type.startsWith('image/')) {
    previewObjectUrl = URL.createObjectURL(file)
    return previewObjectUrl
  }

  return createDicomPlaceholder(file.name)
}

function resolveDemoCase(file: File) {
  const normalizedName = file.name.trim().toLowerCase()
  const normalizedBaseName = getFileBaseName(normalizedName)
  return (
    DEMO_CASES.find((item) => item.fileNames.includes(normalizedName) || item.fileBaseNames?.includes(normalizedBaseName)) ??
    null
  )
}

function getFileBaseName(fileName: string) {
  const extensionIndex = fileName.lastIndexOf('.')
  if (extensionIndex <= 0) return fileName
  return fileName.slice(0, extensionIndex)
}

function createDicomPlaceholder(fileName: string) {
  const safeFileName = escapeForSvg(fileName.length > 24 ? `${fileName.slice(0, 21)}...` : fileName)
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="1200" height="800" viewBox="0 0 1200 800">
      <defs>
        <linearGradient id="bg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#08142b"/>
          <stop offset="60%" stop-color="#0a1a3c"/>
          <stop offset="100%" stop-color="#050d1d"/>
        </linearGradient>
      </defs>
      <rect width="1200" height="800" rx="36" fill="url(#bg)"/>
      <rect x="96" y="96" width="1008" height="608" rx="28" fill="none" stroke="#35f8ff" stroke-opacity="0.24" stroke-width="3"/>
      <circle cx="600" cy="325" r="118" fill="#35f8ff" fill-opacity="0.08" stroke="#35f8ff" stroke-opacity="0.5" stroke-width="3"/>
      <path d="M600 232c-32 0-57 23-61 55-4 31 7 60 23 92 10 20 18 39 23 55h30c5-16 13-35 23-55 16-32 27-61 23-92-4-32-29-55-61-55z" fill="#8effff" fill-opacity="0.2" stroke="#35f8ff" stroke-width="3"/>
      <path d="M548 436c-8 33-19 93 3 125M652 436c8 33 19 93-3 125M600 436v136" stroke="#35f8ff" stroke-opacity="0.55" stroke-width="10" stroke-linecap="round"/>
      <text x="600" y="182" fill="#f2f7ff" font-size="34" font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="middle">DICOM PREVIEW</text>
      <text x="600" y="618" fill="#b6c7e8" font-size="28" font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="middle">${safeFileName}</text>
      <text x="600" y="662" fill="#6f86b6" font-size="20" font-family="Segoe UI, PingFang SC, Microsoft YaHei, sans-serif" text-anchor="middle">当前页面为静态演示，占位图用于模拟 DICOM 导入效果</text>
    </svg>
  `

  return `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(svg)}`
}

function escapeForSvg(value: string) {
  const map: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }

  return value.replace(/[&<>"']/g, (char) => map[char])
}

onUnmounted(() => {
  cleanupAnimation()
  revokePreviewUrl()
})
</script>

<style scoped>
.diagnosis-page {
  max-width: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-bottom: 8px;
}

.diagnosis-hello {
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 0;
}

.diagnosis-subtitle {
  max-width: 720px;
  margin: 10px 0 0;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.7;
}

.diagnosis-hello-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.diagnosis-meta {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: rgba(15, 31, 63, 0.52);
  color: var(--text-soft);
  font-size: 12px;
}

.diag-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 110px;
  height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: transform .16s ease, filter .16s ease, background .16s ease;
}

.diag-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  filter: brightness(1.04);
}

.diag-btn:disabled {
  opacity: .46;
  cursor: not-allowed;
}

.diag-btn-ghost {
  background: rgba(0, 229, 255, 0.08);
  border-color: rgba(0, 229, 255, 0.16);
  color: var(--brand-300);
}

.diag-btn-primary {
  background: linear-gradient(135deg, #69ffff, #35f8ff);
  color: #05211c;
  box-shadow: 0 10px 26px rgba(0, 229, 255, 0.16);
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.left-column,
.right-column {
  min-width: 0;
}

.right-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel {
  border-radius: 18px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: linear-gradient(180deg, rgba(15, 31, 63, 0.76), rgba(6, 20, 24, 0.94));
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.02);
}

.image-panel,
.summary-panel,
.side-panel {
  padding: 20px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.panel-head.compact {
  align-items: center;
  margin-bottom: 14px;
}

.panel-title {
  position: relative;
  padding-left: 14px;
  color: var(--text);
  font-size: 17px;
  font-weight: 700;
}

.panel-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  bottom: 4px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #6ccfff, #35f8ff);
}

.panel-subtitle,
.summary-tip {
  margin: 6px 0 0;
  color: var(--ink-3);
  font-size: 12px;
}

.panel-head-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.state-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(112, 224, 255, 0.14);
  background: rgba(112, 224, 255, 0.05);
  color: var(--text-soft);
  font-size: 12px;
}

.state-chip.scanning {
  color: var(--brand-300);
  background: rgba(0, 229, 255, 0.12);
}

.state-chip.result {
  color: #073026;
  border-color: transparent;
  background: linear-gradient(135deg, #6ccfff, #35f8ff);
}

.icon-btn {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid rgba(112, 224, 255, 0.14);
  border-radius: 10px;
  background: rgba(0, 229, 255, 0.06);
  color: var(--brand-300);
  cursor: pointer;
}

.icon-btn svg {
  width: 16px;
  height: 16px;
}

.image-stage {
  min-height: 420px;
}

.empty-state {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 420px;
  padding: 32px 24px;
  border-radius: 16px;
  border: 1px dashed rgba(0, 229, 255, 0.16);
  background:
    radial-gradient(circle at 50% 24%, rgba(0, 229, 255, 0.08), transparent 36%),
    linear-gradient(180deg, rgba(5, 14, 17, 0.98), rgba(3, 9, 11, 0.98));
  overflow: hidden;
  cursor: pointer;
  transition: border-color .2s ease, box-shadow .2s ease;
}

.empty-state.drag-over {
  border-color: rgba(112, 224, 255, 0.4);
  box-shadow: 0 0 32px rgba(0, 229, 255, 0.12);
}

.empty-state-grid {
  position: relative;
  z-index: 1;
  width: min(100%, 980px);
  display: grid;
  grid-template-columns: minmax(300px, 380px) minmax(0, 420px);
  align-items: center;
  justify-content: center;
  gap: 42px;
}

.empty-visual {
  position: relative;
  min-height: 320px;
  padding: 28px;
  border-radius: 24px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background:
    linear-gradient(180deg, rgba(7, 20, 38, 0.08), rgba(2, 9, 20, 0.18)),
    url('/backtooth.png') center center / cover no-repeat;
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    0 20px 40px rgba(0, 0, 0, 0.22);
  overflow: hidden;
}

.empty-visual::after {
  content: '';
  position: absolute;
  inset: 18px;
  border-radius: 20px;
  border: 1px solid rgba(112, 224, 255, 0.08);
  background:
    linear-gradient(180deg, rgba(2, 9, 20, 0.06), transparent 38%, rgba(2, 9, 20, 0.12) 100%);
  pointer-events: none;
}

.empty-drop-chip {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: 1px solid rgba(112, 224, 255, 0.14);
  background: rgba(3, 16, 32, 0.78);
  backdrop-filter: blur(10px);
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 600;
  z-index: 1;
}

.empty-drop-chip-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #35f8ff;
  box-shadow: 0 0 12px rgba(53, 248, 255, 0.82);
}

.empty-copy {
  text-align: left;
}

.empty-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(112, 224, 255, 0.14);
  background: rgba(0, 229, 255, 0.08);
  color: var(--brand-300);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.empty-title {
  margin: 18px 0 10px;
  color: var(--text);
  font-size: 30px;
  font-weight: 700;
  line-height: 1.2;
}

.empty-hint {
  margin: 0;
  color: var(--ink-2);
  font-size: 14px;
  line-height: 1.8;
}

.empty-specs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.empty-spec-chip {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(112, 224, 255, 0.1);
  background: rgba(15, 31, 63, 0.52);
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 600;
}

.upload-btn {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin-top: 24px;
  min-height: 46px;
  padding: 0 24px;
  border-radius: 999px;
  border: 1px solid rgba(0, 229, 255, 0.24);
  background: linear-gradient(135deg, rgba(105, 255, 255, 0.16), rgba(53, 248, 255, 0.08));
  color: var(--brand-300);
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, filter .18s ease;
}

.upload-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(105, 255, 255, 0.42);
  filter: brightness(1.06);
}

.upload-btn-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: rgba(105, 255, 255, 0.16);
  color: #dffbff;
  font-size: 17px;
  font-weight: 700;
  line-height: 1;
}

.scan-state,
.result-state {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-toolbar {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.view-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px;
  border-radius: 14px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: rgba(8, 20, 44, 0.46);
}

.view-switch-btn {
  min-width: 118px;
  height: 40px;
  padding: 0 14px;
  border: 1px solid transparent;
  border-radius: 11px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background: transparent;
  color: var(--text-soft);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color .18s ease, border-color .18s ease, background .18s ease, transform .18s ease;
}

.view-switch-btn:hover {
  color: var(--text);
  border-color: rgba(112, 224, 255, 0.14);
  background: rgba(0, 229, 255, 0.06);
}

.view-switch-btn.active {
  color: #05211c;
  border-color: transparent;
  background: linear-gradient(135deg, #7cf7ff, #35f8ff);
  box-shadow: 0 8px 22px rgba(0, 229, 255, 0.16);
}

.view-switch-btn.active .view-switch-icon {
  color: inherit;
}

.view-switch-icon {
  width: 16px;
  height: 16px;
  display: grid;
  place-items: center;
  color: currentColor;
}

.view-switch-icon svg {
  width: 16px;
  height: 16px;
}

.view-note {
  flex: 1;
  min-width: 260px;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(112, 224, 255, 0.1);
  background: linear-gradient(135deg, rgba(15, 31, 63, 0.52), rgba(6, 20, 24, 0.7));
}

.view-note-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--brand-300);
  font-size: 12px;
  font-weight: 700;
}

.view-note p {
  margin: 6px 0 0;
  color: var(--text-soft);
  font-size: 12px;
  line-height: 1.6;
}

.scan-box,
.result-box {
  position: relative;
  min-height: 380px;
  overflow: hidden;
  border-radius: 16px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: #020814;
}

.advanced-result-box {
  min-height: 460px;
}

.advanced-result-box > .stage-image {
  min-height: 460px;
  max-height: 540px;
}

.stage-media-shell {
  min-height: 460px;
  display: grid;
  place-items: center;
  padding: 16px;
}

.stage-media-frame {
  position: relative;
  display: inline-block;
  max-width: 100%;
}

.stage-image {
  display: block;
  width: 100%;
  min-height: 380px;
  max-height: 460px;
  object-fit: cover;
}

.stage-image-contained {
  width: auto;
  min-height: 0;
  max-width: 100%;
  max-height: 508px;
  object-fit: contain;
}

.scan-overlay {
  position: absolute;
  inset: 0;
  background: rgba(1, 8, 10, 0.24);
}

.scan-line,
.scan-trail {
  position: absolute;
  left: 0;
  right: 0;
  pointer-events: none;
}

.scan-line {
  height: 3px;
  background: #35f8ff;
  box-shadow: 0 0 16px rgba(0, 229, 255, 0.85);
  animation: scanMove 2.4s ease-in-out infinite;
  z-index: 2;
}

.scan-trail {
  height: 120px;
  background: linear-gradient(to bottom, rgba(0, 229, 255, 0.22), transparent 80%);
  filter: blur(2px);
  animation: trailMove 2.4s ease-in-out infinite;
  z-index: 1;
}

.scan-corner {
  position: absolute;
  width: 22px;
  height: 22px;
  border-style: solid;
  border-color: #35f8ff;
  border-width: 0;
  z-index: 3;
}

.scan-corner.tl {
  top: 10px;
  left: 10px;
  border-top-width: 2px;
  border-left-width: 2px;
}

.scan-corner.tr {
  top: 10px;
  right: 10px;
  border-top-width: 2px;
  border-right-width: 2px;
}

.scan-corner.bl {
  bottom: 10px;
  left: 10px;
  border-bottom-width: 2px;
  border-left-width: 2px;
}

.scan-corner.br {
  right: 10px;
  bottom: 10px;
  border-right-width: 2px;
  border-bottom-width: 2px;
}

.scan-progress {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.progress-bar {
  height: 6px;
  border-radius: 999px;
  background: #102857;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #35f8ff, #3f79ff);
  box-shadow: 0 0 16px rgba(0, 229, 255, 0.28);
}

.progress-text {
  color: var(--brand-300);
  font-size: 12px;
}

.annotation-layer,
.heatmap-layer,
.compare-overlay {
  position: absolute;
  inset: 0;
}

.annotation-layer {
  pointer-events: none;
}

.annotation-region {
  position: absolute;
  display: block;
  border: 0;
  padding: 0;
  background: transparent;
  pointer-events: auto;
  cursor: pointer;
}

.annotation-region-outline {
  position: absolute;
  inset: 0;
  border-radius: 16px;
  border: 1.5px solid var(--accent);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.02), transparent 32%),
    rgba(2, 9, 20, 0.14);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.02),
    0 0 18px color-mix(in srgb, var(--accent) 32%, transparent);
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}

.annotation-region:hover .annotation-region-outline,
.annotation-region.focused .annotation-region-outline {
  transform: scale(1.03);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.04), transparent 28%),
    rgba(2, 9, 20, 0.2);
  box-shadow:
    inset 0 0 0 1px rgba(255, 255, 255, 0.03),
    0 0 24px color-mix(in srgb, var(--accent) 42%, transparent);
}

.annotation-tag {
  position: absolute;
  left: -2px;
  bottom: calc(100% + 8px);
  max-width: 210px;
  padding: 5px 10px 5px 6px;
  border-radius: 999px;
  border: 1px solid rgba(112, 224, 255, 0.16);
  display: inline-flex;
  align-items: center;
  gap: 8px;
  background: rgba(3, 16, 32, 0.92);
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.22);
  white-space: nowrap;
}

.annotation-tag-id {
  min-width: 34px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--accent);
  color: #07141c;
  font-size: 11px;
  font-weight: 800;
}

.annotation-tag-text {
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text);
  font-size: 12px;
  font-weight: 600;
}

.stage-legend {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 180px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: rgba(3, 16, 32, 0.88);
  backdrop-filter: blur(12px);
}

.stage-legend-head {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-bottom: 10px;
}

.stage-legend-head span {
  color: var(--text);
  font-size: 12px;
  font-weight: 700;
}

.stage-legend-head small {
  color: var(--ink-3);
  font-size: 11px;
}

.stage-legend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stage-legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-soft);
  font-size: 12px;
}

.stage-legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 12px currentColor;
}

.heatmap-layer {
  pointer-events: none;
  background: linear-gradient(180deg, rgba(2, 9, 20, 0.04), rgba(2, 9, 20, 0.16));
  overflow: hidden;
}

.heat-cloud {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(circle at center, var(--heat) 0%, color-mix(in srgb, var(--heat) 65%, transparent) 34%, transparent 76%);
  filter: blur(14px);
  mix-blend-mode: screen;
}

.heat-anchor {
  position: absolute;
  transform: translate(-50%, -50%);
  min-width: 34px;
  height: 28px;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--accent) 35%, transparent);
  background: rgba(3, 16, 32, 0.9);
  color: var(--accent);
  font-size: 11px;
  font-weight: 700;
  pointer-events: auto;
  box-shadow: 0 0 16px color-mix(in srgb, var(--accent) 22%, transparent);
}

.heatmap-scale {
  position: absolute;
  right: 14px;
  bottom: 14px;
  width: 154px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: rgba(3, 16, 32, 0.9);
  backdrop-filter: blur(12px);
}

.heatmap-scale > span {
  display: block;
  margin-bottom: 8px;
  color: var(--text);
  font-size: 12px;
  font-weight: 700;
}

.heatmap-scale-bar {
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(90deg, #35f8ff, #64d3ff, #a66bff, #ffad4d, #ff5568);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.06);
}

.heatmap-scale-labels {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
}

.heatmap-scale-labels small {
  color: var(--ink-3);
  font-size: 11px;
}

.compare-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 32px minmax(0, 1fr);
  min-height: 460px;
}

.compare-pane {
  position: relative;
  min-height: 460px;
  overflow: hidden;
}

.compare-pane .stage-image {
  height: 100%;
  min-height: 460px;
  max-height: none;
}

.compare-media-shell {
  min-height: 460px;
  padding: 14px;
}

.compare-pane .stage-image-contained {
  max-height: 430px;
}

.compare-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-3);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
}

.compare-divider span {
  writing-mode: vertical-rl;
}

.compare-badge {
  position: absolute;
  top: 14px;
  left: 14px;
  z-index: 4;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(112, 224, 255, 0.14);
  background: rgba(3, 16, 32, 0.86);
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 700;
}

.compare-badge-accent {
  color: #05211c;
  border-color: transparent;
  background: linear-gradient(135deg, #7cf7ff, #35f8ff);
}

.compare-legend {
  top: 14px;
  right: 14px;
}

.focus-card {
  position: absolute;
  left: 14px;
  bottom: 14px;
  z-index: 6;
  width: 228px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(112, 224, 255, 0.12);
  background: rgba(3, 16, 32, 0.92);
  backdrop-filter: blur(14px);
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.24);
}

.focus-card.compare {
  left: auto;
  right: 14px;
}

.focus-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.focus-card-code {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(0, 229, 255, 0.08);
  color: var(--brand-300);
  font-size: 11px;
  font-weight: 800;
}

.focus-card-risk,
.risk-badge {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 11px;
  font-weight: 700;
}

.focus-card strong {
  display: block;
  color: var(--text);
  font-size: 14px;
  line-height: 1.4;
}

.focus-card p {
  margin: 6px 0 0;
  color: var(--text-soft);
  font-size: 12px;
}

.focus-card small {
  display: block;
  margin-top: 6px;
  color: var(--ink-3);
  font-size: 11px;
}

.annotation-tooth-wrap {
  position: absolute;
  width: 56px;
  height: 72px;
  transform: translate(-50%, -50%);
  z-index: 4;
  cursor: pointer;
  transition: transform 0.2s ease, filter 0.2s ease;
}

.annotation-tooth-wrap:hover {
  transform: translate(-50%, -50%) scale(1.1);
  z-index: 5;
  filter: brightness(1.2);
}

.annotation-tooth {
  width: 100%;
  height: 100%;
  animation: floatTooth 4s ease-in-out infinite;
  animation-delay: calc(var(--id, 0) * 0.4s);
}

.annotation-tip {
  position: absolute;
  left: 50%;
  bottom: calc(100% + 4px);
  transform: translateX(-50%);
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid rgba(112, 224, 255, 0.14);
  background: rgba(3, 16, 32, 0.96);
  color: var(--text);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  pointer-events: none;
  z-index: 10;
}

.summary-head {
  margin-bottom: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.summary-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px;
  border-radius: 14px;
  border: 1px solid rgba(112, 224, 255, 0.08);
  background: rgba(15, 31, 63, 0.42);
}

.summary-card.warning {
  border-color: rgba(255, 99, 110, 0.16);
}

.summary-label {
  color: var(--text-soft);
  font-size: 12px;
}

.summary-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
  min-height: 34px;
  color: var(--text);
  font-size: 24px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.summary-value strong {
  font-size: 28px;
}

.summary-value small {
  color: var(--ink-3);
  font-size: 12px;
  font-weight: 400;
}

.summary-value .severity {
  color: #ffb15f;
  font-size: 22px;
}

.summary-card.warning .summary-value strong {
  color: #ff7f7f;
}

.summary-status {
  color: var(--ink-3);
  font-size: 11px;
}

.summary-note {
  margin: 14px 0 0;
  color: var(--ink-3);
  font-size: 11px;
  line-height: 1.6;
}

.detail-panel {
  padding: 20px;
}

.detail-head {
  align-items: center;
  margin-bottom: 14px;
}

.detail-count {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid rgba(112, 224, 255, 0.14);
  background: rgba(15, 31, 63, 0.42);
  color: var(--text-soft);
  font-size: 12px;
  font-weight: 600;
}

.detail-table-wrap {
  overflow-x: auto;
  border-radius: 16px;
  border: 1px solid rgba(112, 224, 255, 0.08);
  background: rgba(8, 20, 44, 0.38);
}

.detail-table {
  width: 100%;
  min-width: 960px;
  border-collapse: collapse;
}

.detail-table thead th {
  padding: 14px 16px;
  border-bottom: 1px solid rgba(112, 224, 255, 0.08);
  color: var(--ink-3);
  font-size: 12px;
  font-weight: 600;
  text-align: left;
  background: rgba(3, 16, 32, 0.72);
}

.detail-table tbody td {
  padding: 16px;
  border-bottom: 1px solid rgba(112, 224, 255, 0.05);
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.6;
  vertical-align: top;
}

.detail-table tbody tr {
  transition: background .18s ease;
}

.detail-table tbody tr:hover,
.detail-table tbody tr.active {
  background: rgba(0, 229, 255, 0.05);
}

.detail-table tbody tr:last-child td {
  border-bottom: 0;
}

.table-code {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: rgba(0, 229, 255, 0.08);
  color: var(--brand-300);
  font-size: 12px;
  font-weight: 800;
}

.table-main {
  color: var(--text);
  font-size: 13px;
  font-weight: 700;
}

.table-sub {
  margin-top: 2px;
  color: var(--ink-3);
  font-size: 12px;
}

.suggestion-cell {
  min-width: 260px;
}

.detail-empty {
  padding: 34px 18px;
  border-radius: 16px;
  border: 1px dashed rgba(112, 224, 255, 0.12);
  background: rgba(15, 31, 63, 0.22);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

.side-panel {
  display: flex;
  flex-direction: column;
}

.score-ring,
.donut-wrap {
  position: relative;
  width: 140px;
  height: 140px;
  margin: 0 auto 14px;
}

.score-ring svg,
.donut-chart {
  width: 100%;
  height: 100%;
}

.score-center,
.donut-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.score-center strong,
.donut-center strong {
  color: var(--text);
  font-size: 36px;
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.score-center span,
.donut-center span {
  margin-top: 4px;
  color: var(--ink-3);
  font-size: 12px;
}

.risk-pill {
  align-self: center;
  margin-bottom: 10px;
  padding: 5px 14px;
  border-radius: 999px;
  border: 1px solid transparent;
  font-size: 13px;
  font-weight: 600;
}

.risk-pill.pending {
  color: var(--text-soft);
  background: rgba(111, 134, 182, 0.12);
  border-color: rgba(111, 134, 182, 0.2);
}

.risk-pill.high-risk {
  color: #ff636e;
  background: rgba(255, 99, 110, 0.14);
  border-color: rgba(255, 99, 110, 0.2);
}

.risk-pill.medium-risk {
  color: #f7a23a;
  background: rgba(247, 162, 58, 0.14);
  border-color: rgba(247, 162, 58, 0.2);
}

.risk-pill.low-risk {
  color: var(--brand-300);
  background: rgba(0, 229, 255, 0.14);
  border-color: rgba(0, 229, 255, 0.2);
}

.side-copy {
  margin: 0;
  color: var(--text-soft);
  font-size: 13px;
  line-height: 1.7;
  text-align: center;
}

.risk-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.risk-row,
.risk-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.risk-label {
  justify-content: flex-start;
  color: var(--text-soft);
  flex: 1;
}

.risk-row strong {
  color: var(--ink-2);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.risk-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.sparkle {
  color: var(--brand-300);
  font-size: 14px;
}

.diagnosis-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
}

.diagnosis-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 11px 12px;
  border-radius: 12px;
  background: rgba(15, 31, 63, 0.42);
  border: 1px solid rgba(112, 224, 255, 0.06);
  color: var(--text);
  font-size: 13px;
  line-height: 1.6;
}

.diag-icon {
  display: grid;
  place-items: center;
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  margin-top: 1px;
}

.diag-icon svg {
  width: 18px;
  height: 18px;
}

.diagnosis-empty {
  margin-bottom: 16px;
  padding: 28px 14px;
  border-radius: 14px;
  border: 1px dashed rgba(112, 224, 255, 0.12);
  background: rgba(15, 31, 63, 0.22);
  color: var(--ink-3);
  font-size: 13px;
  text-align: center;
}

.report-btn {
  width: 100%;
  min-height: 42px;
  border-radius: 12px;
  border: 1px solid rgba(0, 229, 255, 0.16);
  background: rgba(0, 229, 255, 0.06);
  color: var(--ink-3);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.report-btn:not(:disabled) {
  color: var(--brand-300);
  border-color: rgba(0, 229, 255, 0.36);
  background: linear-gradient(135deg, rgba(0, 229, 255, 0.14), rgba(0, 229, 255, 0.06));
}

.report-btn:disabled {
  cursor: not-allowed;
}

@keyframes floatTooth {
  0%, 100% {
    transform: translateY(0);
  }

  50% {
    transform: translateY(-10px);
  }
}

@keyframes pulseRing {
  0%, 100% {
    transform: scale(1);
    opacity: .32;
  }

  50% {
    transform: scale(1.05);
    opacity: .62;
  }
}

@keyframes scanMove {
  0% {
    top: -3px;
  }

  50% {
    top: 100%;
  }

  50.01% {
    top: -3px;
  }

  100% {
    top: 100%;
  }
}

@keyframes trailMove {
  0% {
    top: -120px;
  }

  50% {
    top: calc(100% - 120px);
  }

  50.01% {
    top: -120px;
  }

  100% {
    top: calc(100% - 120px);
  }
}



@media (max-width: 1180px) {
  .content-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .right-column {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

   .view-note {
    min-width: 100%;
  }
}

@media (max-width: 920px) {
  .diagnosis-hello {
    flex-direction: column;
  }

  .diagnosis-hello-actions {
    justify-content: flex-start;
  }

  .summary-grid,
  .right-column {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .empty-state-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 24px;
  }

  .empty-copy {
    text-align: center;
  }

  .empty-specs {
    justify-content: center;
  }

  .upload-btn {
    margin-left: auto;
    margin-right: auto;
  }

  .view-switch {
    width: 100%;
  }

  .view-switch-btn {
    flex: 1 1 140px;
  }

  .compare-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .compare-divider {
    min-height: 34px;
  }

  .compare-divider span {
    writing-mode: horizontal-tb;
  }

  .compare-pane,
  .compare-pane .stage-image {
    min-height: 320px;
  }

  .focus-card.compare {
    left: 14px;
    right: auto;
  }
}

@media (max-width: 680px) {
  .summary-grid,
  .right-column {
    grid-template-columns: minmax(0, 1fr);
  }

  .stage-image,
  .scan-box,
  .result-box {
    min-height: 280px;
  }

  .advanced-result-box,
  .advanced-result-box > .stage-image,
  .compare-pane,
  .compare-pane .stage-image {
    min-height: 300px;
  }

  .panel-head {
    flex-direction: column;
    align-items: flex-start;
  }

  .empty-state {
    padding: 24px 18px;
  }

  .empty-visual {
    min-height: 276px;
    padding: 22px 18px;
  }

  .empty-tooth-svg {
    width: 184px;
    height: 222px;
  }

  .empty-title {
    font-size: 24px;
  }

  .stage-legend,
  .heatmap-scale,
  .focus-card {
    left: 12px;
    right: 12px;
    width: auto;
  }

  .compare-legend {
    left: 12px;
    right: 12px;
  }

  .annotation-tag {
    max-width: 180px;
  }
}
</style>

