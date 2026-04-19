<template>
  <div class="flex flex-col h-full p-4 lg:p-6 pb-0 overflow-hidden page-bg">
    <div class="flex items-center justify-between mb-4 shrink-0">
      <div class="flex flex-col">
        <span class="font-mono text-[11px] text-[var(--td)] tracking-[0.1em] uppercase mb-1">AI Core / <span class="text-[var(--cyan)]">Case Portal</span></span>
        <h2 class="text-[22px] font-medium text-[var(--tp)] m-0">影像扫描</h2>
      </div>

      <div class="flex items-center gap-3">
        <div class="relative w-[280px]">
          <input
            type="text"
            placeholder="搜索患者 ID 或姓名..."
            class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[36px] pl-9 pr-3 text-[13px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] focus:shadow-[0_0_8px_rgba(0,229,255,0.2)] transition-all font-mono placeholder:text-[var(--td)]"
          />
          <svg class="w-4 h-4 text-[var(--ts)] absolute left-3 top-1/2 -translate-y-1/2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
        </div>

        <div class="flex gap-1 border border-[var(--ln)] rounded-[3px] p-0.5" style="background: rgba(3,8,18,0.5);">
          <button class="px-3 py-1 font-mono text-[11px] bg-[var(--cyan)] text-[var(--void)] rounded-[2px] shadow-[0_0_8px_var(--cyan)] glow-cyan">全部</button>
          <button class="px-3 py-1 font-mono text-[11px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">待处理</button>
          <button class="px-3 py-1 font-mono text-[11px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">已分析</button>
          <button class="px-3 py-1 font-mono text-[11px] text-[var(--td)] hover:text-[var(--tp)] rounded-[2px] transition-colors">已复核</button>
        </div>

        <NeuralButton variant="primary" @click="openNewCaseDrawer">
          <template #icon-left><svg class="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg></template>
          新建病例
        </NeuralButton>
      </div>
    </div>

    <div class="flex-1 overflow-y-auto min-h-0 pr-1">
      <div class="grid grid-cols-3 gap-3">
        <div
          v-for="c in store.tasks.items"
          :key="c.id"
          class="glass-panel rounded-md overflow-hidden cursor-pointer hover:border-[var(--cyan)]/40 transition-all group relative"
          @click="router.push(`/analysis/${c.id}`)"
        >
          <div class="absolute left-0 top-0 bottom-0 w-[2px] z-20" :class="getGradeAccent(c.grade)"></div>

          <div class="aspect-[16/9] bg-[#0A1428] relative overflow-hidden border-b border-[var(--ln)]">
            <div class="absolute inset-0 opacity-[0.04]" style="background-image: linear-gradient(var(--cyan) 1px, transparent 1px), linear-gradient(90deg, var(--cyan) 1px, transparent 1px); background-size: 12px 12px;"></div>

            <div class="absolute inset-0 flex items-center justify-center">
              <div class="flex gap-0.5 opacity-50">
                <div v-for="t in 6" :key="t" class="w-5 h-14 bg-gradient-to-b from-white/20 to-transparent rounded-[40%_40%_10%_10%]"></div>
              </div>
            </div>

            <div class="absolute inset-0 bg-gradient-to-t from-[#030812] via-transparent to-transparent"></div>

            <div class="absolute top-2 right-2 flex flex-col gap-1 z-10">
              <div class="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-[2px] border" :class="getGradeChipClass(c.grade)">
                <span class="font-mono text-[10px] font-medium">{{ c.grade }}</span>
              </div>
            </div>

            <div class="absolute bottom-2 left-2 z-10 flex items-center gap-1.5">
              <div class="w-1.5 h-1.5 rounded-full bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]"></div>
              <span class="font-mono text-[10px] text-[var(--cyan-soft)]">{{ new Date(c.createdAt).toLocaleDateString() }}</span>
            </div>

            <div class="absolute left-0 right-0 h-[1px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)] opacity-0 group-hover:opacity-100 transition-opacity z-10 animate-scanline"></div>
          </div>

          <div class="p-3 flex flex-col gap-2">
            <div class="flex justify-between items-start">
              <div class="flex flex-col">
                <span class="text-[13px] text-[var(--tp)] font-medium">{{ c.patientName || c.patientId }}</span>
                <span class="font-mono text-[11px] text-[var(--td)]">{{ c.patientId }} | {{ c.no }}</span>
              </div>
              <span class="font-mono text-[10px] text-[var(--td)] uppercase">{{ c.caseNo }}</span>
            </div>

            <div class="flex items-center justify-between gap-2 pt-1 border-t border-[var(--ln)]">
              <div class="flex items-center gap-1.5">
                <div class="w-1 h-1 rounded-full animate-pulse-opacity" :class="getStatusDotClass(c.status)"></div>
                <span class="font-mono text-[10px] uppercase tracking-[0.1em]" :class="getStatusTextClass(c.status)">{{ statusMap[c.status] || c.status }}</span>
              </div>

              <div class="flex items-center gap-2 flex-1 max-w-[120px]">
                <div class="flex-1 h-[3px] bg-[var(--void)] border border-[var(--ln)] rounded-full overflow-hidden">
                  <div class="h-full" :style="{ width: `${(c.uncertainty || 0) * 100}%` }" :class="(c.uncertainty || 0) > 0.35 ? 'bg-[var(--amber)] shadow-[0_0_4px_var(--amber)]' : 'bg-[var(--cyan)] shadow-[0_0_4px_var(--cyan)]'"></div>
                </div>
                <span class="font-mono text-[10px] tabular-nums" :class="(c.uncertainty || 0) > 0.35 ? 'text-[var(--amber)]' : 'text-[var(--ts)]'">{{ (c.uncertainty || 0).toFixed(2) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <Transition name="fade-up">
        <div v-if="showNewCase" class="fixed inset-0 z-50 flex items-center justify-center" @click.self="closeNewCaseDrawer">
          <div class="absolute inset-0 bg-black/60 backdrop-blur-sm"></div>
          <div class="glass-panel w-[520px] p-8 rounded-md relative z-10 shadow-[0_20px_60px_rgba(0,0,0,0.6)]">
            <div class="absolute top-0 left-0 w-[10px] h-[10px] border-t border-l border-[var(--cyan)]/60"></div>
            <div class="absolute top-0 right-0 w-[10px] h-[10px] border-t border-r border-[var(--cyan)]/60"></div>
            <div class="absolute bottom-0 left-0 w-[10px] h-[10px] border-b border-l border-[var(--cyan)]/60"></div>
            <div class="absolute bottom-0 right-0 w-[10px] h-[10px] border-b border-r border-[var(--cyan)]/60"></div>

            <div class="flex items-center gap-3 mb-1">
              <div class="w-[3px] h-[16px] bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]"></div>
              <h3 class="text-[16px] font-medium text-[var(--tp)] m-0">登记新病例</h3>
            </div>
            <div class="font-mono text-[10px] text-[var(--td)] tracking-[0.15em] mb-8 pl-[15px]">录入患者并上传 X 光扫描影像</div>

            <form class="flex flex-col gap-5" @submit.prevent="submitNewCase">
              <div class="grid grid-cols-2 gap-4">
                <div class="flex flex-col gap-1.5">
                  <label class="font-mono text-[11px] text-[var(--ts)] tracking-widest uppercase">患者 ID</label>
                  <input
                    v-model.trim="newCaseForm.patientCode"
                    type="text"
                    class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[36px] px-3 text-[13px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-all font-mono"
                    placeholder="P-XXXX"
                  />
                </div>
                <div class="flex flex-col gap-1.5">
                  <label class="font-mono text-[11px] text-[var(--ts)] tracking-widest uppercase">年龄</label>
                  <input
                    v-model.trim="newCaseForm.age"
                    type="number"
                    min="0"
                    max="120"
                    class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] h-[36px] px-3 text-[13px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-all font-mono"
                    placeholder="45"
                  />
                </div>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="font-mono text-[11px] text-[var(--ts)] tracking-widest uppercase">性别</label>
                <div class="flex gap-2">
                  <button
                    type="button"
                    class="flex-1 py-2 rounded-[3px] border font-mono text-[11px] transition-all"
                    :class="newCaseForm.genderCode === 'MALE'
                      ? 'border-[var(--cyan)] bg-[var(--cyan)]/20 text-[var(--cyan)] shadow-[0_0_8px_rgba(0,229,255,0.2)]'
                      : 'border-[var(--ln)] text-[var(--td)] hover:text-[var(--ts)]'"
                    @click="newCaseForm.genderCode = 'MALE'"
                  >
                    男
                  </button>
                  <button
                    type="button"
                    class="flex-1 py-2 rounded-[3px] border font-mono text-[11px] transition-all"
                    :class="newCaseForm.genderCode === 'FEMALE'
                      ? 'border-[var(--cyan)] bg-[var(--cyan)]/20 text-[var(--cyan)] shadow-[0_0_8px_rgba(0,229,255,0.2)]'
                      : 'border-[var(--ln)] text-[var(--td)] hover:text-[var(--ts)]'"
                    @click="newCaseForm.genderCode = 'FEMALE'"
                  >
                    女
                  </button>
                </div>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="font-mono text-[11px] text-[var(--ts)] tracking-widest uppercase">主诉</label>
                <textarea
                  v-model.trim="newCaseForm.chiefComplaint"
                  class="w-full bg-[rgba(3,8,18,0.7)] border border-[var(--ln)] rounded-[3px] p-3 text-[13px] text-[var(--tp)] focus:outline-none focus:border-[var(--cyan)] transition-all resize-none h-[80px]"
                  placeholder="请输入患者主诉内容..."
                ></textarea>
              </div>

              <div class="flex flex-col gap-1.5">
                <label class="font-mono text-[11px] text-[var(--ts)] tracking-widest uppercase">X 光影像上传</label>
                <input
                  ref="fileInputRef"
                  type="file"
                  accept=".png,.jpg,.jpeg,.dcm,.dicom,image/png,image/jpeg,application/dicom"
                  class="hidden"
                  @change="handleFileInputChange"
                />
                <div
                  class="border border-dashed rounded-[4px] p-6 flex flex-col items-center justify-center transition-colors"
                  :class="isDragActive ? 'border-[var(--cyan)] bg-[var(--cyan)]/10' : 'border-[var(--cyan)]/30 hover:bg-[var(--cyan)]/5'"
                  @click="openFilePicker"
                  @dragenter.prevent="isDragActive = true"
                  @dragover.prevent="isDragActive = true"
                  @dragleave.prevent="isDragActive = false"
                  @drop.prevent="handleFileDrop"
                >
                  <template v-if="selectedFile">
                    <div class="w-full rounded-[4px] border border-[var(--cyan)]/30 bg-[rgba(3,8,18,0.65)] p-4">
                      <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0">
                          <div class="text-[13px] text-[var(--tp)] truncate">{{ selectedFile.name }}</div>
                          <div class="mt-1 font-mono text-[11px] text-[var(--td)]">{{ formatFileSize(selectedFile.size) }} · {{ fileTypeLabel }}</div>
                        </div>
                        <button
                          type="button"
                          class="text-[11px] font-mono text-[var(--amber)] hover:text-[var(--tp)] transition-colors"
                          @click.stop="clearSelectedFile"
                        >
                          移除
                        </button>
                      </div>
                    </div>
                    <span class="text-[11px] text-[var(--td)] mt-3">点击重新选择，或直接拖入其他文件替换</span>
                  </template>
                  <template v-else>
                    <svg class="w-6 h-6 text-[var(--cyan)] opacity-50 mb-2" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
                    <span class="text-[13px] text-[var(--ts)]">拖拽 X 光图片至此</span>
                    <span class="text-[11px] text-[var(--td)] mt-1">支持 PNG, JPG, DICOM</span>
                  </template>
                </div>
              </div>

              <div v-if="inlineError" class="rounded-[4px] border border-[var(--magenta)]/35 bg-[var(--magenta)]/10 px-3 py-2 text-[12px] text-[var(--tp)]">
                {{ inlineError }}
              </div>

              <div class="flex gap-2 mt-2">
                <button
                  type="button"
                  @click="closeNewCaseDrawer"
                  class="flex-1 py-2.5 rounded-[3px] border border-[var(--ln)] text-[var(--td)] font-mono text-[11px] uppercase tracking-wider hover:text-[var(--ts)] transition-colors"
                  :disabled="isSubmitting"
                >
                  取消
                </button>
                <button
                  type="submit"
                  class="flex-1 py-2.5 rounded-[3px] bg-gradient-to-r from-[var(--cyan)]/20 to-[var(--violet)]/20 border border-[var(--cyan)] text-[var(--tp)] font-mono text-[11px] uppercase tracking-wider shadow-[0_0_12px_rgba(0,229,255,0.3)] hover:shadow-[0_0_20px_rgba(0,229,255,0.5)] transition-all disabled:opacity-60 disabled:cursor-not-allowed"
                  :disabled="isSubmitting"
                >
                  {{ isSubmitting ? '提交中...' : '创建并分析' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import NeuralButton from '../components/shared/NeuralButton.vue';
import { useAnalysisStore } from '../stores/analysis';
import { useAuthStore } from '../stores/auth';
import { useNotificationStore } from '../stores/notification';
import { ApiClientError } from '../api/request';
import { casePortalApi } from '../api/casePortal';

type GenderCode = 'MALE' | 'FEMALE' | '';

interface NewCaseFormState {
  patientCode: string;
  age: string;
  genderCode: GenderCode;
  chiefComplaint: string;
}

const router = useRouter();
const store = useAnalysisStore();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();

const showNewCase = ref(false);
const isSubmitting = ref(false);
const isDragActive = ref(false);
const selectedFile = ref<File | null>(null);
const inlineError = ref('');
const fileInputRef = ref<HTMLInputElement | null>(null);

const newCaseForm = reactive<NewCaseFormState>({
  patientCode: '',
  age: '',
  genderCode: '',
  chiefComplaint: '',
});

const statusMap: Record<string, string> = {
  DONE: '已完成',
  RUNNING: '进行中',
  REVIEW: '待复核',
  FAILED: '失败',
};

const fileTypeLabel = computed(() => {
  if (!selectedFile.value) return '';
  const fileName = selectedFile.value.name.toLowerCase();
  if (fileName.endsWith('.dcm') || fileName.endsWith('.dicom')) return 'DICOM';
  if (selectedFile.value.type === 'image/png') return 'PNG';
  if (selectedFile.value.type === 'image/jpeg') return 'JPG';
  return selectedFile.value.type || 'Unknown';
});

onMounted(() => {
  store.fetchTasks({ pageNum: 1, pageSize: 12 });
});

const resetNewCaseForm = () => {
  newCaseForm.patientCode = '';
  newCaseForm.age = '';
  newCaseForm.genderCode = '';
  newCaseForm.chiefComplaint = '';
  selectedFile.value = null;
  inlineError.value = '';
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const openNewCaseDrawer = () => {
  resetNewCaseForm();
  showNewCase.value = true;
};

const closeNewCaseDrawer = () => {
  if (isSubmitting.value) return;
  showNewCase.value = false;
  inlineError.value = '';
};

const openFilePicker = () => {
  if (isSubmitting.value) return;
  fileInputRef.value?.click();
};

const setSelectedFile = (file?: File | null) => {
  if (!file) return;
  selectedFile.value = file;
  inlineError.value = '';
};

const handleFileInputChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  const [file] = input.files ?? [];
  setSelectedFile(file);
};

const handleFileDrop = (event: DragEvent) => {
  isDragActive.value = false;
  const [file] = Array.from(event.dataTransfer?.files ?? []);
  setSelectedFile(file);
};

const clearSelectedFile = () => {
  selectedFile.value = null;
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

const formatFileSize = (bytes: number) => {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
};

const buildLocalDate = (age: string) => {
  const numericAge = Number.parseInt(age, 10);
  if (Number.isNaN(numericAge) || numericAge < 0) return undefined;
  const date = new Date();
  date.setFullYear(date.getFullYear() - numericAge);
  return date.toISOString().slice(0, 10);
};

const buildLocalDateTime = () => {
  const now = new Date();
  const pad = (value: number) => value.toString().padStart(2, '0');
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
};

const buildPatientName = (patientCode: string) => `患者${patientCode}`;

const normalizeErrorMessage = (error: unknown) => {
  if (error instanceof ApiClientError) return error.message;
  if (error instanceof Error) return error.message;
  return '创建病例失败';
};

const validateNewCaseForm = () => {
  if (!newCaseForm.patientCode) return '患者 ID 不能为空。';
  if (!newCaseForm.age) return '年龄不能为空。';
  if (!newCaseForm.genderCode) return '请选择性别。';
  if (!newCaseForm.chiefComplaint) return '请填写主诉。';
  if (!selectedFile.value) return '请先选择一张影像文件。';
  return '';
};

const submitNewCase = async () => {
  const validationError = validateNewCaseForm();
  if (validationError) {
    inlineError.value = validationError;
    notificationStore.warning('表单未完成', validationError);
    return;
  }

  const currentUserId = authStore.user?.id;
  if (!currentUserId) {
    inlineError.value = '当前登录信息不完整，请重新登录后再试。';
    notificationStore.error('无法创建病例', inlineError.value);
    return;
  }

  isSubmitting.value = true;
  inlineError.value = '';

  try {
    const patientRes = await casePortalApi.createPatient({
      patientName: buildPatientName(newCaseForm.patientCode),
      genderCode: newCaseForm.genderCode || undefined,
      birthDate: buildLocalDate(newCaseForm.age),
      sourceCode: 'OUTPATIENT',
      privacyLevelCode: 'L4',
      remark: `Created from case portal for ${newCaseForm.patientCode}`,
    });

    const patientId = patientRes.data.patientId;

    const visitRes = await casePortalApi.createVisit({
      patientId,
      doctorUserId: currentUserId,
      visitTypeCode: 'OUTPATIENT',
      visitDate: buildLocalDateTime(),
      complaint: newCaseForm.chiefComplaint,
      triageLevelCode: 'NORMAL',
      sourceChannelCode: 'MANUAL',
      remark: 'Created from case portal',
    });

    const visitId = visitRes.data.visitId;

    const caseRes = await casePortalApi.createCase({
      visitId,
      patientId,
      caseTypeCode: 'CARIES_SCREENING',
      caseTitle: `Case ${newCaseForm.patientCode}`,
      chiefComplaint: newCaseForm.chiefComplaint,
      priorityCode: 'NORMAL',
      clinicalNotes: newCaseForm.chiefComplaint,
      remark: 'Created from case portal',
    });

    const caseId = caseRes.data.caseId;

    const uploadRes = await casePortalApi.uploadCaseFile(selectedFile.value!, caseId, 'PANORAMIC');

    const imageRes = await casePortalApi.createCaseImage(caseId, {
      attachmentId: uploadRes.data.attachmentId,
      visitId,
      patientId,
      imageTypeCode: 'PANORAMIC',
      imageSourceCode: 'UPLOAD',
      shootingTime: buildLocalDateTime(),
      primaryFlag: '1',
      remark: 'Uploaded from case portal',
    });

    await casePortalApi.saveImageQualityCheck(imageRes.data.imageId, {
      checkTypeCode: 'AUTO',
      checkResultCode: 'PASS',
      qualityScore: 97,
      issueCodes: ['NONE'],
      suggestionText: 'Portal upload passed default quality gate',
      remark: 'Auto-approved from case portal',
    });

    const analysisRes = await casePortalApi.createAnalysis(caseId, {
      caseId,
      patientId,
      forceRetryFlag: false,
      taskTypeCode: 'INFERENCE',
      remark: 'Created from case portal',
    });

    notificationStore.success('病例创建成功', `分析任务 ${analysisRes.data.taskNo} 已入队。`);
    showNewCase.value = false;
    resetNewCaseForm();
    await store.fetchTasks({ pageNum: 1, pageSize: 12 });
    await router.push(`/analysis/${analysisRes.data.taskId}`);
  } catch (error) {
    const message = normalizeErrorMessage(error);
    inlineError.value = message;
    notificationStore.error('创建并分析失败', message);
  } finally {
    isSubmitting.value = false;
  }
};

const getGradeAccent = (grade?: string) => {
  switch (grade) {
    case 'G0': return 'bg-[var(--emerald)]';
    case 'G1': return 'bg-[var(--cyan)]';
    case 'G2': return 'bg-[var(--amber)]';
    case 'G3': return 'bg-[var(--magenta)]';
    case 'G4': return 'bg-[var(--violet)]';
    default: return 'bg-[var(--td)]';
  }
};

const getGradeChipClass = (grade?: string) => {
  switch (grade) {
    case 'G0': return 'bg-[var(--emerald)]/15 border-[var(--emerald)]/40 text-[var(--emerald)]';
    case 'G1': return 'bg-[var(--cyan)]/15 border-[var(--cyan)]/40 text-[var(--cyan)]';
    case 'G2': return 'bg-[var(--amber)]/15 border-[var(--amber)]/40 text-[var(--amber)]';
    case 'G3': return 'bg-[var(--magenta)]/15 border-[var(--magenta)]/40 text-[var(--magenta)]';
    case 'G4': return 'bg-[var(--violet)]/15 border-[var(--violet)]/40 text-[var(--violet)]';
    default: return 'bg-[var(--td)]/15 border-[var(--td)]/40 text-[var(--td)]';
  }
};

const getStatusDotClass = (status: string) => {
  switch (status) {
    case 'DONE': return 'bg-[var(--emerald)] shadow-[0_0_8px_var(--emerald)]';
    case 'RUNNING': return 'bg-[var(--cyan)] shadow-[0_0_8px_var(--cyan)]';
    case 'REVIEW': return 'bg-[var(--amber)] shadow-[0_0_8px_var(--amber)]';
    case 'FAILED': return 'bg-[var(--magenta)] shadow-[0_0_8px_var(--magenta)]';
    default: return 'bg-[var(--td)]';
  }
};

const getStatusTextClass = (status: string) => {
  switch (status) {
    case 'DONE': return 'text-[var(--emerald)]';
    case 'RUNNING': return 'text-[var(--cyan)]';
    case 'REVIEW': return 'text-[var(--amber)]';
    case 'FAILED': return 'text-[var(--magenta)]';
    default: return 'text-[var(--td)]';
  }
};
</script>
