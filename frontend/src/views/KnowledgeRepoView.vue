<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex items-center gap-3">
        <div class="flex flex-col">
          <span class="font-mono text-[11px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">
            情报核心 / <span class="text-[var(--violet)]">知识管理</span>
          </span>
          <div class="flex items-center gap-2">
            <h2 class="text-[22px] font-medium text-[var(--tp)] m-0">知识库管理</h2>
            <div
              class="inline-flex items-center gap-1.5 px-2 py-0.5 rounded-[2px] border bg-[var(--violet)]/10 border-[var(--violet)]/30"
            >
              <div class="w-1 h-1 rounded-full bg-[var(--violet)] shadow-[0_0_8px_var(--violet)] animate-pulse-opacity"></div>
              <span class="font-mono text-[10px] text-[var(--violet)] uppercase tracking-[0.1em]">RAG 治理引擎</span>
            </div>
          </div>
        </div>
      </div>

      <div class="flex gap-2">
        <NeuralButton variant="ghost" :disabled="store.rebuilding" @click="handleRebuildAll">
          {{ store.rebuilding ? '重建中...' : '重新构建索引' }}
        </NeuralButton>
        <NeuralButton variant="primary" :disabled="store.uploading" @click="openFilePicker">
          <template #icon-left>
            <svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"
              />
            </svg>
          </template>
          {{ store.uploading ? '上传中...' : '上传文档' }}
        </NeuralButton>
      </div>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      class="hidden"
      accept=".pdf,.doc,.docx,.txt,.md,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,text/plain,text/markdown"
      @change="handleFileInputChange"
    />

    <div class="grid grid-cols-4 gap-3 mb-4 shrink-0">
      <KpiCard label="总文档数" :value="statsDocs" color="violet" />
      <KpiCard label="知识分片总数" :value="statsChunks" color="cyan" />
      <KpiCard label="最后索引时间" :value="statsLastIndexed" color="emerald" />
      <KpiCard label="知识库版本" :value="statsVersion" color="amber" />
    </div>

    <div class="flex-1 glass-panel rounded-md overflow-hidden flex flex-col min-h-0">
      <div
        class="border-b border-dashed p-6 flex flex-col items-center justify-center shrink-0 cursor-pointer transition-colors"
        :class="
          isDragActive
            ? 'border-[var(--violet)] bg-[var(--violet)]/15'
            : 'border-[var(--violet)]/20 bg-[var(--violet)]/5 hover:bg-[var(--violet)]/10'
        "
        @click="openFilePicker"
        @dragenter.prevent="isDragActive = true"
        @dragover.prevent="isDragActive = true"
        @dragleave.prevent="isDragActive = false"
        @drop.prevent="handleFileDrop"
      >
        <svg class="w-6 h-6 text-[var(--violet)] opacity-60 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="1.5"
            d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
          />
        </svg>
        <span class="text-[13px] text-[var(--tp)] mb-1">
          {{ store.uploading ? '正在上传文档...' : '拖拽文档至此或点击上传' }}
        </span>
        <span class="text-[11px] text-[var(--td)]">支持格式: PDF, DOC, DOCX, TXT, MD</span>
      </div>

      <div class="overflow-auto flex-1 relative">
        <table class="w-full text-left border-collapse min-w-[900px]">
          <thead class="sticky top-0 bg-[rgba(3,8,18,0.85)] backdrop-blur z-10 border-b border-[var(--ln)]">
            <tr>
              <th class="py-2.5 pl-4 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">文档名称</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">类型</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">状态</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-right">分片数</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-right">实体数</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-center">版本</th>
              <th class="py-2.5 px-3 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal">更新时间</th>
              <th class="py-2.5 pr-4 pl-2 text-[10px] font-mono text-[var(--ts)] uppercase tracking-[0.12em] font-normal text-right">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="doc in store.documents.items"
              :key="doc.id"
              class="border-b border-[var(--ln)]/50 hover:bg-[var(--violet)]/5 transition-colors group relative"
            >
              <td class="py-3 pl-4 px-3 relative">
                <div class="absolute left-0 top-0 bottom-0 w-[2px]" :class="getAccentColor(doc.publishStatus)"></div>
                <div class="flex flex-col">
                  <span class="text-[12px] text-[var(--tp)]">{{ doc.title }}</span>
                  <span class="font-mono text-[10px] text-[var(--td)]">{{ doc.no }}</span>
                </div>
              </td>
              <td class="py-3 px-3">
                <span
                  class="font-mono text-[10px] px-1.5 py-0.5 rounded-[2px] border"
                  :class="getTypeClass(doc.type)"
                >
                  {{ typeMap[doc.type] || doc.type }}
                </span>
              </td>
              <td class="py-3 px-3">
                <div class="flex items-center gap-1.5">
                  <div class="w-1.5 h-1.5 rounded-full" :class="getDotColor(doc.publishStatus, doc.reviewStatus)"></div>
                  <span class="font-mono text-[10px]" :class="getStatusColor(doc.publishStatus, doc.reviewStatus)">
                    {{ getStatusLabel(doc.publishStatus, doc.reviewStatus) }}
                  </span>
                </div>
              </td>
              <td class="py-3 px-3 text-right font-mono text-[11px] text-[var(--tp)] tabular-nums">
                {{ formatCount(doc.chunks) }}
              </td>
              <td class="py-3 px-3 text-right font-mono text-[11px] text-[var(--tp)] tabular-nums">
                {{ formatCount(doc.entities) }}
              </td>
              <td class="py-3 px-3 text-center font-mono text-[10px] text-[var(--ts)]">
                {{ doc.version || '--' }}
              </td>
              <td class="py-3 px-3 font-mono text-[10px] text-[var(--td)]">
                {{ formatDateTime(doc.updatedAt) }}
              </td>
              <td class="py-3 pr-4 pl-2 text-right">
                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button
                    class="text-[10px] font-mono text-[var(--violet)] hover:underline transition-colors disabled:opacity-50 disabled:no-underline"
                    :disabled="store.isDocBusy(doc.id)"
                    @click="openDocumentDetail(doc)"
                  >
                    查看
                  </button>
                  <button
                    class="text-[10px] font-mono text-[var(--td)] hover:text-[var(--tp)] transition-colors disabled:opacity-50"
                    :disabled="store.isDocBusy(doc.id)"
                    @click="handleReindexDoc(doc)"
                  >
                    重引
                  </button>
                  <button
                    class="text-[10px] font-mono text-[var(--magenta)] hover:underline transition-colors disabled:opacity-50"
                    :disabled="store.isDocBusy(doc.id)"
                    @click="handleDeleteDoc(doc)"
                  >
                    删除
                  </button>
                </div>
              </td>
            </tr>

            <tr v-if="store.loading && store.documents.items.length === 0">
              <td colspan="8" class="py-16 text-center">
                <div class="flex flex-col items-center gap-3">
                  <div
                    class="w-10 h-10 rounded-full border border-[var(--violet)]/30 border-t-[var(--violet)] animate-spin shadow-[0_0_12px_rgba(139,92,246,0.2)]"
                  ></div>
                  <span class="font-mono text-[11px] text-[var(--violet)] tracking-[0.15em] animate-pulse-opacity">
                    正在加载知识库...
                  </span>
                </div>
              </td>
            </tr>

            <tr v-if="!store.loading && store.documents.items.length === 0">
              <td colspan="8" class="py-16 text-center">
                <div class="flex flex-col items-center gap-2">
                  <svg class="w-8 h-8 text-[var(--td)] opacity-50" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="1"
                      d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                    />
                  </svg>
                  <span class="text-[13px] text-[var(--td)]">知识库中暂无文档</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="h-12 border-t border-[var(--ln)] bg-[rgba(3,8,18,0.5)] flex items-center justify-between px-4 shrink-0">
        <div class="font-mono text-[11px] text-[var(--td)] uppercase tracking-[0.1em]">
          显示 {{ displayStart }}-{{ displayEnd }} 条，共 {{ store.documents.total }} 条
        </div>

        <div class="flex items-center gap-1">
          <button
            class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:text-[var(--tp)]"
            :disabled="!canPrev"
            @click="goPrev"
          >
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
          <button
            class="w-8 h-6 flex items-center justify-center rounded-xs border border-[var(--violet)] bg-[var(--violet)]/10 text-[var(--violet)] font-mono text-[10px] shadow-[0_0_8px_rgba(139,92,246,0.2)]"
          >
            {{ store.documents.page }}
          </button>
          <button
            class="w-8 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] font-mono text-[10px]"
          >
            {{ totalPages }}
          </button>
          <button
            class="w-6 h-6 flex items-center justify-center rounded-xs border border-[var(--ln)] text-[var(--td)] transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:text-[var(--tp)]"
            :disabled="!canNext"
            @click="goNext"
          >
            <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </div>

  <Teleport to="body">
    <Transition name="fade-up">
      <div v-if="detailVisible" class="fixed inset-0 z-50 flex items-center justify-center" @click.self="closeDetail">
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
        <div class="glass-panel w-[min(92vw,900px)] max-h-[86vh] rounded-md relative z-10 shadow-[0_20px_60px_rgba(0,0,0,0.6)] flex flex-col">
          <div class="h-12 border-b border-[var(--ln)] px-4 flex items-center justify-between shrink-0">
            <div class="font-mono text-[12px] text-[var(--tp)] tracking-[0.12em] uppercase">文档详情</div>
            <button class="text-[var(--td)] hover:text-[var(--tp)] transition-colors" @click="closeDetail">✕</button>
          </div>

          <div class="p-4 overflow-auto min-h-0">
            <div v-if="detailLoading" class="py-16 flex flex-col items-center gap-3">
              <div
                class="w-10 h-10 rounded-full border border-[var(--violet)]/30 border-t-[var(--violet)] animate-spin shadow-[0_0_12px_rgba(139,92,246,0.2)]"
              ></div>
              <span class="font-mono text-[11px] text-[var(--violet)] tracking-[0.15em]">正在加载文档详情...</span>
            </div>

            <template v-else-if="detailDoc">
              <div class="grid grid-cols-2 gap-3 mb-4">
                <div class="p-3 border border-[var(--ln)] rounded-[4px] bg-[rgba(3,8,18,0.55)]">
                  <div class="font-mono text-[10px] text-[var(--td)] uppercase mb-2">基础信息</div>
                  <div class="text-[12px] text-[var(--tp)] leading-6">
                    <div>文档编号: {{ detailDoc.no }}</div>
                    <div>文档标题: {{ detailDoc.title }}</div>
                    <div>类型: {{ typeMap[detailDoc.type] || detailDoc.type }}</div>
                    <div>版本: {{ detailDoc.version || '--' }}</div>
                    <div>更新时间: {{ formatDateTime(detailDoc.updatedAt) }}</div>
                  </div>
                </div>

                <div class="p-3 border border-[var(--ln)] rounded-[4px] bg-[rgba(3,8,18,0.55)]">
                  <div class="font-mono text-[10px] text-[var(--td)] uppercase mb-2">索引状态</div>
                  <div class="text-[12px] text-[var(--tp)] leading-6">
                    <div>审核状态: {{ reviewStatusMap[detailDoc.reviewStatus] || detailDoc.reviewStatus }}</div>
                    <div>发布状态: {{ publishStatusMap[detailDoc.publishStatus] || detailDoc.publishStatus }}</div>
                    <div>分片数: {{ formatCount(detailDoc.chunks) }}</div>
                    <div>实体数: {{ formatCount(detailDoc.entities) }}</div>
                    <div>关系数: {{ formatCount(detailDoc.relations) }}</div>
                  </div>
                </div>
              </div>

              <div class="mb-4 p-3 border border-[var(--ln)] rounded-[4px] bg-[rgba(3,8,18,0.55)]">
                <div class="font-mono text-[10px] text-[var(--td)] uppercase mb-2">备注</div>
                <textarea
                  v-model.trim="detailActionComment"
                  class="w-full bg-[rgba(3,8,18,0.8)] border border-[var(--ln)] rounded-[3px] p-2 text-[12px] text-[var(--tp)] focus:outline-none focus:border-[var(--violet)] resize-none h-[64px]"
                  placeholder="可选：填写审核/发布备注"
                ></textarea>
              </div>

              <div class="flex flex-wrap items-center gap-2 mb-4">
                <NeuralButton variant="ghost" :disabled="detailActionLoading" @click="handleDetailAction('submitReview')">
                  提交审核
                </NeuralButton>
                <NeuralButton variant="success" :disabled="detailActionLoading" @click="handleDetailAction('approve')">
                  审核通过
                </NeuralButton>
                <NeuralButton variant="danger" :disabled="detailActionLoading" @click="handleDetailAction('reject')">
                  驳回
                </NeuralButton>
                <NeuralButton variant="primary" :disabled="detailActionLoading" @click="handleDetailAction('publish')">
                  发布
                </NeuralButton>
                <NeuralButton variant="ghost" :disabled="detailActionLoading" @click="handleDetailAction('rollback')">
                  回滚
                </NeuralButton>
              </div>

              <div class="border border-[var(--ln)] rounded-[4px] overflow-hidden">
                <div class="h-9 px-3 flex items-center font-mono text-[10px] text-[var(--td)] uppercase border-b border-[var(--ln)]">
                  版本记录
                </div>
                <div class="max-h-[220px] overflow-auto">
                  <table class="w-full border-collapse">
                    <thead class="bg-[rgba(3,8,18,0.75)]">
                      <tr>
                        <th class="py-2 px-3 text-left font-mono text-[10px] text-[var(--ts)] uppercase tracking-[0.1em] font-normal">版本</th>
                        <th class="py-2 px-3 text-left font-mono text-[10px] text-[var(--ts)] uppercase tracking-[0.1em] font-normal">审核状态</th>
                        <th class="py-2 px-3 text-left font-mono text-[10px] text-[var(--ts)] uppercase tracking-[0.1em] font-normal">发布状态</th>
                        <th class="py-2 px-3 text-left font-mono text-[10px] text-[var(--ts)] uppercase tracking-[0.1em] font-normal">更新时间</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr
                        v-for="version in detailDoc.versions"
                        :key="version.versionNo"
                        class="border-b border-[var(--ln)]/50"
                      >
                        <td class="py-2 px-3 font-mono text-[11px] text-[var(--tp)]">{{ version.versionNo }}</td>
                        <td class="py-2 px-3 text-[11px] text-[var(--ts)]">{{ reviewStatusMap[version.reviewStatus] || version.reviewStatus }}</td>
                        <td class="py-2 px-3 text-[11px] text-[var(--ts)]">{{ publishStatusMap[version.publishStatus] || version.publishStatus }}</td>
                        <td class="py-2 px-3 font-mono text-[10px] text-[var(--td)]">
                          {{ formatDateTime(version.updatedAt || version.createdAt) }}
                        </td>
                      </tr>
                      <tr v-if="detailDoc.versions.length === 0">
                        <td colspan="4" class="py-6 text-center text-[12px] text-[var(--td)]">暂无版本记录</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import NeuralButton from '../components/shared/NeuralButton.vue';
import KpiCard from '../components/shared/KpiCard.vue';
import { useKnowledgeStore } from '../stores/knowledge';
import { useNotificationStore } from '../stores/notification';
import { ApiClientError } from '../api/request';
import type { KnowledgeDocument, KnowledgeDocumentDetail } from '../models/knowledge';

type DetailAction = 'submitReview' | 'approve' | 'reject' | 'publish' | 'rollback';

const store = useKnowledgeStore();
const notificationStore = useNotificationStore();

const fileInputRef = ref<HTMLInputElement | null>(null);
const isDragActive = ref(false);

const detailVisible = ref(false);
const detailLoading = ref(false);
const detailActionLoading = ref(false);
const detailDoc = ref<KnowledgeDocumentDetail | null>(null);
const detailActionComment = ref('');

const typeMap: Record<string, string> = {
  MANUAL: '操作手册',
  GUIDELINE: '临床指南',
  RESEARCH: '科研文献',
  UPLOAD: '上传文档',
};

const reviewStatusMap: Record<string, string> = {
  PENDING: '待处理',
  PARSED: '已解析',
  REVIEW_PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回',
};

const publishStatusMap: Record<string, string> = {
  DRAFT: '草稿',
  INDEXED: '已索引',
  PUBLISHED: '已发布',
  DISABLED: '已禁用',
};

const statsDocs = computed(() => String(store.stats?.docs ?? 0));
const statsChunks = computed(() => String(store.stats?.chunks ?? 0));
const statsLastIndexed = computed(() => formatDateOnly(store.stats?.lastIndexed));
const statsVersion = computed(() => store.stats?.version || '--');

const totalPages = computed(() => Math.max(1, Math.ceil(store.documents.total / store.documents.pageSize)));
const canPrev = computed(() => store.documents.page > 1);
const canNext = computed(() => store.documents.page < totalPages.value);
const displayStart = computed(() => (store.documents.total === 0 ? 0 : (store.documents.page - 1) * store.documents.pageSize + 1));
const displayEnd = computed(() => Math.min(store.documents.page * store.documents.pageSize, store.documents.total));

const normalizedError = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message;
  if (error instanceof Error) return error.message;
  return '操作失败';
};

const reload = async (pageNum = store.documents.page) => {
  await Promise.all([
    store.fetchOverview(),
    store.fetchDocuments({
      pageNum,
      pageSize: store.documents.pageSize,
    }),
  ]);
};

const openFilePicker = () => {
  if (store.uploading) return;
  fileInputRef.value?.click();
};

const allowedFile = (file: File) => {
  const fileName = file.name.toLowerCase();
  return ['.pdf', '.doc', '.docx', '.txt', '.md'].some((ext) => fileName.endsWith(ext));
};

const uploadFile = async (file?: File | null) => {
  if (!file) return;
  if (!allowedFile(file)) {
    notificationStore.warning('文件类型不支持', '请上传 PDF / DOC / DOCX / TXT / MD 文件');
    return;
  }

  try {
    await store.uploadDocument(file);
    notificationStore.success('上传成功', `文档 ${file.name} 已入库，正在后台处理`);
    await reload(1);
  } catch (error) {
    notificationStore.error('上传失败', normalizedError(error));
  }
};

const handleFileInputChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const [file] = input.files ?? [];
  void uploadFile(file);
  if (input) input.value = '';
};

const handleFileDrop = (event: DragEvent) => {
  isDragActive.value = false;
  const [file] = Array.from(event.dataTransfer?.files ?? []);
  void uploadFile(file);
};

const handleRebuildAll = async () => {
  const ok = window.confirm('确认重新构建知识库索引吗？该过程可能需要一些时间。');
  if (!ok) return;

  try {
    await store.rebuildIndex();
    notificationStore.success('重建任务已提交', '索引重建已在后台执行');
    await reload();
  } catch (error) {
    notificationStore.error('重建失败', normalizedError(error));
  }
};

const goPrev = () => {
  if (!canPrev.value) return;
  void reload(store.documents.page - 1);
};

const goNext = () => {
  if (!canNext.value) return;
  void reload(store.documents.page + 1);
};

const openDocumentDetail = async (doc: KnowledgeDocument) => {
  detailVisible.value = true;
  detailActionComment.value = '';
  detailLoading.value = true;
  try {
    detailDoc.value = await store.fetchDocumentDetail(doc.id);
  } catch (error) {
    notificationStore.error('加载详情失败', normalizedError(error));
    detailDoc.value = null;
  } finally {
    detailLoading.value = false;
  }
};

const closeDetail = () => {
  if (detailActionLoading.value) return;
  detailVisible.value = false;
  detailDoc.value = null;
};

const pickVersionNo = (doc: KnowledgeDocumentDetail | KnowledgeDocument): string => {
  if ('versions' in doc && doc.versions.length > 0) {
    return doc.currentVersionNo ?? doc.publishedVersionNo ?? doc.version ?? doc.versions[0].versionNo;
  }
  return doc.publishedVersionNo ?? doc.currentVersionNo ?? doc.version ?? '';
};

const handleReindexDoc = async (doc: KnowledgeDocument) => {
  const ok = window.confirm(`确认对文档 ${doc.no} 执行重引吗？`);
  if (!ok) return;

  try {
    await store.reindexDocument(doc);
    notificationStore.success('重引成功', `${doc.no} 已触发重引`);
    await reload();
  } catch (error) {
    notificationStore.error('重引失败', normalizedError(error));
  }
};

const handleDeleteDoc = async (doc: KnowledgeDocument) => {
  const ok = window.confirm(`确认删除文档 ${doc.no} 吗？该操作不可撤销。`);
  if (!ok) return;

  try {
    await store.deleteDocument(doc.id);
    notificationStore.success('删除成功', `${doc.no} 已删除`);
    const targetPage = store.documents.items.length === 1 && store.documents.page > 1
      ? store.documents.page - 1
      : store.documents.page;
    await reload(targetPage);
  } catch (error) {
    notificationStore.error('删除失败', normalizedError(error));
  }
};

const handleDetailAction = async (action: DetailAction) => {
  if (!detailDoc.value) return;
  const versionNo = pickVersionNo(detailDoc.value);
  if (!versionNo) {
    notificationStore.warning('无法执行操作', '当前文档缺少可用版本号');
    return;
  }

  detailActionLoading.value = true;
  try {
    const payload = {
      versionNo,
      comment: detailActionComment.value || undefined,
    };

    if (action === 'submitReview') {
      await store.submitReview(detailDoc.value.id, payload);
      notificationStore.success('提交成功', '已提交审核');
    } else if (action === 'approve') {
      await store.approve(detailDoc.value.id, payload);
      notificationStore.success('审核通过', '文档已审核通过');
    } else if (action === 'reject') {
      await store.reject(detailDoc.value.id, payload);
      notificationStore.success('已驳回', '文档已驳回');
    } else if (action === 'publish') {
      await store.publish(detailDoc.value.id, payload);
      notificationStore.success('发布成功', '文档已发布');
    } else if (action === 'rollback') {
      await store.rollback(detailDoc.value.id, payload);
      notificationStore.success('回滚成功', '文档已回滚');
    }

    detailDoc.value = await store.fetchDocumentDetail(detailDoc.value.id);
    await reload();
  } catch (error) {
    notificationStore.error('操作失败', normalizedError(error));
  } finally {
    detailActionLoading.value = false;
  }
};

const formatDateOnly = (iso?: string) => {
  if (!iso) return '--';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '--';
  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')}`;
};

const formatDateTime = (iso?: string) => {
  if (!iso) return '--';
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return '--';
  return `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, '0')}-${d.getDate().toString().padStart(2, '0')} ${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
};

const formatCount = (value?: number) => (typeof value === 'number' && Number.isFinite(value) ? String(value) : '--');

const getTypeClass = (type: string) => {
  if (type === 'MANUAL') return 'bg-[var(--violet)]/20 border-[var(--violet)]/40 text-[var(--violet)]';
  if (type === 'GUIDELINE') return 'bg-[var(--violet)]/15 border-[var(--violet)]/30 text-[var(--violet)]';
  if (type === 'RESEARCH') return 'bg-[var(--cyan)]/10 border-[var(--cyan)]/30 text-[var(--cyan)]';
  return 'bg-[var(--td)]/10 border-[var(--td)]/30 text-[var(--td)]';
};

const getStatusLabel = (publishStatus: string, reviewStatus: string) =>
  publishStatusMap[publishStatus] || reviewStatusMap[reviewStatus] || publishStatus || reviewStatus || '未知';

const getStatusColor = (publishStatus: string, reviewStatus: string) => {
  if (publishStatus === 'PUBLISHED' || publishStatus === 'INDEXED') return 'text-[var(--emerald)]';
  if (reviewStatus === 'REVIEW_PENDING' || reviewStatus === 'PARSED') return 'text-[var(--cyan)]';
  if (reviewStatus === 'APPROVED') return 'text-[var(--emerald)]';
  if (reviewStatus === 'REJECTED') return 'text-[var(--magenta)]';
  return 'text-[var(--amber)]';
};

const getDotColor = (publishStatus: string, reviewStatus: string) => {
  if (publishStatus === 'PUBLISHED' || publishStatus === 'INDEXED') {
    return 'bg-[var(--emerald)] shadow-[0_0_4px_var(--emerald)]';
  }
  if (reviewStatus === 'REVIEW_PENDING' || reviewStatus === 'PARSED') {
    return 'bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)] animate-pulse-opacity';
  }
  if (reviewStatus === 'REJECTED') {
    return 'bg-[var(--magenta)] shadow-[0_0_4px_var(--magenta)]';
  }
  return 'bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]';
};

const getAccentColor = (publishStatus: string) => {
  if (publishStatus === 'PUBLISHED' || publishStatus === 'INDEXED') return 'bg-[var(--emerald)]';
  if (publishStatus === 'DRAFT') return 'bg-[var(--cyan)]';
  if (publishStatus === 'DISABLED') return 'bg-[var(--magenta)]';
  return 'bg-[var(--violet)]/50';
};

onMounted(() => {
  void reload(1);
});
</script>

<style scoped>
.fade-up-enter-active,
.fade-up-leave-active {
  transition: all 0.2s ease-out;
}

.fade-up-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.fade-up-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
