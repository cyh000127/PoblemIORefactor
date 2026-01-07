<template>
  <div class="quiz-editor-page">
    <!-- 수정 모드 + 로딩 중 -->
    <div v-if="mode === 'edit' && loading" class="loading-wrapper">
      <i class="pi pi-spin pi-spinner text-4xl"></i>
    </div>

    <!-- 실제 에디터 화면 -->
    <div v-else>
      <!-- 상단 퀴즈 메타 + 저장 버튼 -->
      <div class="top-bar">
        <Button label="퀴즈 기본 정보" icon="pi pi-sliders-h" class="mr-2" @click="metaDialogVisible = true" />
        <div class="flex-1" />
        <Button label="취소" severity="secondary" outlined class="mr-2" @click="handleCancel" />
        <Button
          :label="mode === 'create' ? '저장' : '수정 완료'"
          icon="pi pi-check"
          :loading="submitting"
          :disabled="!isFormValid || thumbnailUploading || questionUploadingIndex !== null || isGenerating || isConfirming"
          @click="handleSubmit"
        />
      </div>

      <!-- 문제 카드 리스트 -->
      <div class="question-list-wrapper">
        <div class="question-list">
          <!-- 문제 카드들 -->
          <div
            v-for="(question, index) in quizForm.questions"
            :key="question.id || index"
            class="question-card"
            :class="{ 'drag-over': questionCardDragOverIndex === index }"
            @click="handleQuestionCardClick(index)"
            @dragover.prevent.stop="handleQuestionDragOver(index)"
            @dragleave.stop="handleQuestionDragLeave"
            @drop.prevent.stop="handleQuestionDrop(index, $event)"
          >
            <div class="card-image-wrapper">
              <img v-if="question.imageUrl" :src="getImagePreview(question.imageUrl)" alt="question image" />
              <div v-else class="no-image">이미지를 설정해주세요</div>

              <!-- 설명 + 정답이 모두 세팅 안 된 경우 느낌표 표시 -->
              <span v-if="!isQuestionConfigured(question)" class="badge-warning">!</span>
            </div>
            <div class="card-footer">
              <span class="card-title">
                {{ question.description || `Question ${question.questionOrder}` }}
              </span>
              <Button icon="pi pi-trash" severity="danger" text rounded @click.stop="removeQuestion(index)" />
            </div>
          </div>

          <!-- + 문제 추가 카드 -->
          <div class="question-card add-card" @click="handleAddQuestionClick">
            <div class="add-card-inner">
              <span class="plus-icon">+</span>
            </div>
          </div>
        </div>

        <!-- 문제 하나도 없을 때 안내 문구 -->
        <div v-if="quizForm.questions.length === 0" class="empty-helper text-color-secondary">
          아직 등록된 문제가 없습니다. 오른쪽
          <strong>+</strong>
          카드를 눌러 첫 문제를 추가해보세요.
        </div>
      </div>

      <!-- 숨겨진 파일 인풋: 문제 이미지 업로드용 -->
      <input ref="questionImageInputRef" type="file" accept="image/*" class="hidden-file-input" @change="handleQuestionImageChange" />

      <!-- 숨겨진 파일 인풋: 썸네일 업로드용 -->
      <input ref="thumbnailInputRef" type="file" accept="image/*" class="hidden-file-input" @change="handleThumbnailChange" />

      <!-- 퀴즈 메타 정보 모달 -->
      <Dialog v-model:visible="metaDialogVisible" :header="mode === 'create' ? '퀴즈 만들기' : '퀴즈 수정하기'" :modal="true" :style="{ width: '480px' }">
        <div class="meta-form">
          <div class="field">
            <label class="field-label">제목 *</label>
            <InputText v-model="quizForm.title" placeholder="퀴즈 제목을 입력하세요" class="w-full" />
          </div>

          <div class="field">
            <label class="field-label">설명 (ai 이미지 생성은  10글자 이상)</label>
            <Textarea v-model="quizForm.description" rows="3" placeholder="퀴즈에 대한 설명을 입력하세요" class="w-full" />
          </div>

          <div class="field">
            <label class="field-label">썸네일 (선택)</label>
            <div
              class="thumbnail-box"
              :class="{ 'drag-over': thumbnailDragOver }"
              @click="triggerThumbnailSelect"
              @dragover.prevent="thumbnailDragOver = true"
              @dragleave.prevent="thumbnailDragOver = false"
              @drop.prevent="handleThumbnailDrop"
            >
              <img v-if="thumbnailPreview" :src="thumbnailPreview" alt="thumbnail" />
              <div v-else class="thumbnail-placeholder">
                <span class="plus-icon">+</span>
                <span>썸네일을 추가해보세요. (설정하지 않으면 첫 문제 이미지)</span>
              </div>
            </div>

            <div class="ai-thumbnail-actions">
              <Button
                label="AI 추천 나만의 썸네일 생성"
                icon="pi pi-sparkles"
                severity="secondary"
                outlined
                class="ai-generate-button"
                :loading="isGenerating"
                :disabled="!canGenerateAiThumbnail || isGenerating || isConfirming"
                @click="handleGenerateAiCandidates"
              />
              <small v-if="!canGenerateAiThumbnail" class="text-color-secondary ai-helper">제목/설명을 조금 더 입력하면 정확한 썸네일을 추천할 수 있어요.</small>
              <small v-else-if="thumbnailPreview" class="text-color-secondary ai-helper">AI 적용 시 현재 썸네일이 교체됩니다.</small>
            </div>

            <div v-if="aiCandidates.length > 0" class="ai-candidate-panel">
              <div class="ai-candidate-grid">
                <button
                  v-for="candidate in aiCandidates"
                  :key="candidate.candidateId"
                  type="button"
                  class="ai-candidate-card"
                  :class="{ selected: selectedCandidateId === candidate.candidateId }"
                  @click="selectAiCandidate(candidate.candidateId)"
                >
                  <img :src="candidate.previewDataUrl" alt="AI thumbnail" />
                  <span v-if="selectedCandidateId === candidate.candidateId" class="ai-selected-badge">선택됨</span>
                </button>
              </div>
              <div class="ai-candidate-footer">
                <Button label="다시 생성" icon="pi pi-refresh" severity="secondary" outlined :loading="isGenerating" :disabled="isConfirming" @click="handleGenerateAiCandidates" />
                <Button label="이 썸네일 적용" icon="pi pi-check" :loading="isConfirming" :disabled="!selectedCandidateId || isGenerating" @click="applyAiCandidate" />
              </div>
            </div>
          </div>

          <div class="flex justify-end gap-2 mt-4">
            <Button label="닫기" text @click="metaDialogVisible = false" />
            <Button label="확인" @click="metaDialogVisible = false" />
          </div>
        </div>
      </Dialog>

      <!-- 문제 편집 모달 -->
      <Dialog v-model:visible="questionDialogVisible" header="문제 편집" :modal="true" :style="{ width: '720px' }">
        <div v-if="editingQuestionIndex !== null" class="question-edit">
          <div
            class="question-image-preview"
            :class="{ 'drag-over': questionDialogDragOver }"
            @dragover.prevent="handleQuestionDialogDragOver"
            @dragleave.prevent="handleQuestionDialogDragLeave"
            @drop.prevent="handleQuestionDialogDrop"
          >
            <img v-if="currentQuestion.imageUrl" :src="getImagePreview(currentQuestion.imageUrl)" alt="question" />
            <div v-else class="no-image large">
              아직 이미지가 없습니다.
              <Button label="이미지 선택" size="small" class="mt-2" @click="triggerQuestionImageSelect" />
            </div>
          </div>

          <div class="field mt-3">
            <label class="field-label">질문 설명</label>
            <Textarea v-model="questionForm.description" rows="2" placeholder="이 이미지에 대한 설명(힌트)을 입력하세요." class="w-full" />
          </div>

          <div class="field">
            <label class="field-label">정답 *</label>
            <small class="text-color-secondary mb-2 block">여러 개의 정답(동의어, 변형)을 입력할 수 있습니다. 엔터를 눌러 추가하세요.</small>
            <Chips v-ime-fix v-model="questionForm.answers" placeholder="정답을 입력하고 Enter" class="w-full answer-chips" :allow-duplicate="false" @add="handleAnswerAdd" />
          </div>

          <div class="flex justify-end gap-2 mt-4">
            <Button label="취소" text @click="closeQuestionDialog" />
            <Button label="확인" @click="confirmQuestionDialog" />
          </div>
        </div>
      </Dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, watch, onMounted } from "vue";
import { useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
import { useConfirm } from "primevue/useconfirm";
import { useQuizStore } from "@/stores/quiz";
import { createQuiz, getQuiz, updateQuiz } from "@/api/quiz";
import { createCandidates, confirmCandidate } from "@/api/aiThumbnail";
import { uploadFile } from "@/api/file";
import { resolveImageUrl } from "@/lib/image";

// 한글 IME 중복 입력 방지 디렉티브
const vImeFix = {
  mounted(el: HTMLElement) {
    const input = el.querySelector("input");
    if (input) {
      let isComposing = false;

      // IME 상태 수동 추적
      input.addEventListener("compositionstart", () => {
        isComposing = true;
      });

      input.addEventListener("compositionend", () => {
        isComposing = false;
      });

      input.addEventListener(
        "keydown",
        (e: KeyboardEvent) => {
          if ((e.isComposing || isComposing) && (e.key === "Enter" || e.code === "Enter")) {
            e.preventDefault();
            e.stopImmediatePropagation();
          }
        },
        true // Capture phase에서 가로채기
      );
    }
  },
};

const props = defineProps<{
  mode: "create" | "edit";
  quizId?: number;
}>();

const router = useRouter();
const toast = useToast();
const quizStore = useQuizStore();

const submitting = ref(false);
const thumbnailUploading = ref(false);
const questionUploadingIndex = ref<number | null>(null);
const loading = ref(false);
const thumbnailDragOver = ref(false);
const questionCardDragOverIndex = ref<number | null>(null);
const questionDialogDragOver = ref(false);
const aiCandidates = ref<{ candidateId: string; previewDataUrl: string }[]>([]);
const selectedCandidateId = ref<string | null>(null);
const isGenerating = ref(false);
const isConfirming = ref(false);
const aiTitleMin = 5;
const aiDescriptionMin = 10;

// 퀴즈 메타/문제 편집 모달
const metaDialogVisible = ref(props.mode === "create");
const questionDialogVisible = ref(false);
const editingQuestionIndex = ref<number | null>(null);

// 파일 입력 참조
const questionImageInputRef = ref<HTMLInputElement | null>(null);
const thumbnailInputRef = ref<HTMLInputElement | null>(null);

const quizForm = computed(() => quizStore.quizForm);
const buildImageUrl = (path?: string) => resolveImageUrl(path);
const thumbnailPreview = computed(() => buildImageUrl(quizForm.value.thumbnailUrl));
const canGenerateAiThumbnail = computed(() => {
  const titleLength = quizForm.value.title?.trim().length || 0;
  const descriptionLength = quizForm.value.description?.trim().length || 0;
  return titleLength >= aiTitleMin && descriptionLength >= aiDescriptionMin;
});

// 모달 내에서 쓰는 questionForm
const questionForm = reactive({
  description: "",
  answers: [] as string[],
});

// ==== 정답 칩 정규화 로직 ====
const normalizeAnswers = (arr: string[]): string[] => {
  let normalized = arr.map((v) => v.trim()).filter((v) => v.length > 0);

  normalized = normalized.filter((v, idx, self) => self.indexOf(v) === idx);
  return normalized;
};

const applyAnswerNormalization = () => {
  const current = questionForm.answers;
  const normalized = normalizeAnswers(current);

  if (normalized.length !== current.length || normalized.some((v, i) => v !== current[i])) {
    questionForm.answers = [...normalized];
  }
};

const handleAnswerAdd = () => {
  applyAnswerNormalization();
};

watch(
  () => questionForm.answers,
  () => {
    applyAnswerNormalization();
  },
  { deep: true }
);

// 현재 편집 중인 문제 객체
const currentQuestion = computed(() => {
  if (editingQuestionIndex.value === null) return {} as any;
  return quizForm.value.questions[editingQuestionIndex.value];
});

// 폼 유효성 검사
const isFormValid = computed(() => {
  if (!quizForm.value.title?.trim()) return false;
  if (quizForm.value.questions.length === 0) return false;

  return quizForm.value.questions.every((q) => {
    return q.answers && q.answers.length > 0;
  });
});

// 문제가 설정 완료됐는지 여부 (설명 + 정답)
const isQuestionConfigured = (q: any) => {
  const hasDesc = !!q.description?.trim();
  const hasAnswers = q.answers && q.answers.length > 0;
  return hasDesc && hasAnswers;
};

// ==== 퀴즈 로딩 (edit 모드 전용) ====
const loadQuiz = async () => {
  if (!props.quizId) return;
  loading.value = true;
  try {
    const quiz = await getQuiz(props.quizId);
    quizForm.value.title = quiz.title;
    quizForm.value.description = quiz.description;
    quizForm.value.thumbnailUrl = quiz.thumbnailUrl;
    quizForm.value.questions = quiz.questions.map((q: any) => ({
      id: q.id,
      questionOrder: q.order,
      description: q.description,
      imageUrl: q.imageUrl,
      answers: q.answers?.map((a: any) => a.text) || [],
    }));
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "Error",
      detail: "퀴즈 정보를 불러오지 못했습니다.",
      life: 3000,
    });
    router.push("/");
  } finally {
    loading.value = false;
  }
};

// ==== 퀴즈 메타 / 썸네일 ====
const triggerThumbnailSelect = () => {
  thumbnailInputRef.value?.click();
};

const uploadThumbnailFile = async (file: File) => {
  if (!isImageFile(file)) {
    toast.add({
      severity: "warn",
      summary: "Invalid file",
      detail: "이미지 파일만 업로드할 수 있습니다.",
      life: 2500,
    });
    return;
  }

  thumbnailUploading.value = true;
  try {
    const { url } = await uploadFile(file, "thumbnail");
    quizForm.value.thumbnailUrl = url;
    toast.add({
      severity: "success",
      summary: "업로드 성공",
      detail: "썸네일 이미지가 설정되었습니다.",
      life: 2000,
    });
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "업로드 실패",
      detail: error.response?.data?.message || "이미지 업로드에 실패했습니다.",
      life: 3000,
    });
  } finally {
    thumbnailUploading.value = false;
  }
};

const handleThumbnailChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;

  try {
    await uploadThumbnailFile(file);
  } finally {
    target.value = "";
  }
};

const handleThumbnailDrop = async (event: DragEvent) => {
  thumbnailDragOver.value = false;
  const file = event.dataTransfer?.files?.[0];
  if (!file) return;
  await uploadThumbnailFile(file);
};

// ==== AI 썸네일 후보 생성 ====
const handleGenerateAiCandidates = async () => {
  if (!canGenerateAiThumbnail.value || isGenerating.value) return;

  isGenerating.value = true;
  try {
    const data = await createCandidates(quizForm.value.title?.trim() || "", quizForm.value.description?.trim() || "");
    aiCandidates.value = data?.candidates || [];
    selectedCandidateId.value = null;
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "오류",
      detail: "생성에 실패했습니다. 잠시 후 다시 시도해 주세요.",
      life: 3000,
    });
  } finally {
    isGenerating.value = false;
  }
};

const selectAiCandidate = (candidateId: string) => {
  selectedCandidateId.value = candidateId;
};

const applyAiCandidate = async () => {
  if (!selectedCandidateId.value || isConfirming.value) return;

  isConfirming.value = true;
  try {
    const data = await confirmCandidate(selectedCandidateId.value);
    quizForm.value.thumbnailUrl = data?.thumbnailUrl || "";
    toast.add({
      severity: "success",
      summary: "성공",
      detail: "AI 썸네일이 적용되었습니다.",
      life: 2000,
    });
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "오류",
      detail: "적용에 실패했습니다. 다시 시도해 주세요.",
      life: 3000,
    });
  } finally {
    isConfirming.value = false;
  }
};

const resetAiState = () => {
  aiCandidates.value = [];
  selectedCandidateId.value = null;
  isGenerating.value = false;
  isConfirming.value = false;
};

// ==== 문제 카드/이미지/모달 ====
const handleAddQuestionClick = () => {
  quizStore.addQuestion();
};

const triggerQuestionImageSelect = () => {
  questionImageInputRef.value?.click();
};

const handleQuestionImageChange = async (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) return;
  if (editingQuestionIndex.value === null) return;

  const index = editingQuestionIndex.value;
  try {
    await uploadQuestionImageFile(file, index);
  } finally {
    target.value = "";
  }
};

const uploadQuestionImageFile = async (file: File, index: number) => {
  if (!isImageFile(file)) {
    toast.add({
      severity: "warn",
      summary: "파일 형식 오류",
      detail: "이미지 파일만 업로드할 수 있습니다.",
      life: 2500,
    });
    return;
  }

  questionUploadingIndex.value = index;

  try {
    const { url } = await uploadFile(file, "question");
    quizForm.value.questions[index].imageUrl = url;
    toast.add({
      severity: "success",
      summary: "업로드 성공",
      detail: `Question ${quizForm.value.questions[index].questionOrder} 이미지가 설정되었습니다.`,
      life: 2000,
    });

    openQuestionDialog(index);
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "업로드 실패",
      detail: error.response?.data?.message || "이미지 업로드에 실패했습니다.",
      life: 3000,
    });
  } finally {
    questionUploadingIndex.value = null;
  }
};

const handleQuestionDragOver = (index: number) => {
  questionCardDragOverIndex.value = index;
};

const handleQuestionDragLeave = () => {
  questionCardDragOverIndex.value = null;
};

const handleQuestionDrop = async (index: number, event: DragEvent) => {
  questionCardDragOverIndex.value = null;
  const file = event.dataTransfer?.files?.[0];
  if (!file) return;
  editingQuestionIndex.value = index;
  await uploadQuestionImageFile(file, index);
};

const handleQuestionCardClick = (index: number) => {
  editingQuestionIndex.value = index;
  const q = quizForm.value.questions[index];

  if (!q.imageUrl) {
    triggerQuestionImageSelect();
  } else {
    openQuestionDialog(index);
  }
};

const openQuestionDialog = (index: number) => {
  editingQuestionIndex.value = index;
  const q = quizForm.value.questions[index];

  questionForm.description = q.description || "";
  questionForm.answers = q.answers ? [...q.answers] : [];

  questionDialogVisible.value = true;
};

const handleQuestionDialogDragOver = () => {
  questionDialogDragOver.value = true;
};

const handleQuestionDialogDragLeave = () => {
  questionDialogDragOver.value = false;
};

const handleQuestionDialogDrop = async (event: DragEvent) => {
  questionDialogDragOver.value = false;
  if (editingQuestionIndex.value === null) return;
  const file = event.dataTransfer?.files?.[0];
  if (!file) return;
  await uploadQuestionImageFile(file, editingQuestionIndex.value);
};

const closeQuestionDialog = () => {
  questionDialogVisible.value = false;
  editingQuestionIndex.value = null;
};

const confirmQuestionDialog = () => {
  if (editingQuestionIndex.value === null) return;
  const idx = editingQuestionIndex.value;
  const q = quizForm.value.questions[idx];

  q.description = questionForm.description;
  q.answers = [...questionForm.answers];

  questionDialogVisible.value = false;
};

const removeQuestion = (index: number) => {
  quizStore.removeQuestion(index);
};

const getImagePreview = (path?: string) => buildImageUrl(path);
const isImageFile = (file: File) => file.type.startsWith("image/");

// ==== 취소/저장 ====
const confirmService = useConfirm();

const handleCancel = () => {
  if (props.mode === "create") {
    confirmService.require({
      message: '작성 중인 내용이 모두 사라집니다. 취소하시겠습니까?',
      header: '취소 확인',
      icon: 'pi pi-exclamation-triangle',
      rejectProps: {
        label: '계속 작성',
        severity: 'secondary',
        outlined: true
      },
      acceptProps: {
        label: '나가기',
        severity: 'danger'
      },
      accept: () => {
        quizStore.resetQuizForm();
        router.push("/");
      },
      reject: () => {
        // stay
      }
    });
  } else {
    router.push(`/quiz/${props.quizId}`);
  }
};

const handleSubmit = async () => {
  if (thumbnailUploading.value || questionUploadingIndex.value !== null) {
    toast.add({
      severity: "warn",
      summary: "Uploading images",
      detail: "이미지 업로드가 완료될 때까지 기다려주세요.",
      life: 2500,
    });
    return;
  }

  if (isGenerating.value || isConfirming.value) {
    toast.add({
      severity: "warn",
      summary: "Generating thumbnails",
      detail: "AI 썸네일 작업이 완료될 때까지 기다려주세요.",
      life: 2500,
    });
    return;
  }

  if (!isFormValid.value) {
    toast.add({
      severity: "warn",
      summary: "Warning",
      detail: "필수 항목을 모두 입력해주세요.",
      life: 3000,
    });
    return;
  }

  submitting.value = true;
  try {
    if (props.mode === "create") {
      const result = await createQuiz(quizForm.value);
      quizStore.resetQuizForm();
      toast.add({
        severity: "success",
        summary: "성공",
        detail: "퀴즈가 생성되었습니다.",
        life: 3000,
      });
      router.push(`/quiz/${result.id}`);
    } else {
      if (!props.quizId) return;
      await updateQuiz(props.quizId, quizForm.value);
      toast.add({
        severity: "success",
        summary: "성공",
        detail: "퀴즈가 수정되었습니다.",
        life: 3000,
      });
      router.push(`/quiz/${props.quizId}`);
    }
  } catch (error: any) {
    toast.add({
      severity: "error",
      summary: "오류",
      detail: error.response?.data?.message || (props.mode === "create" ? "퀴즈 생성에 실패했습니다." : "퀴즈 수정에 실패했습니다."),
      life: 3000,
    });
  } finally {
    submitting.value = false;
  }
};

// ==== 초기화 ====
onMounted(() => {
  if (props.mode === "create") {
    quizStore.resetQuizForm();
    resetAiState();
  } else if (props.mode === "edit") {
    loadQuiz();
  }
});
</script>

<style scoped>
.quiz-editor-page {
  min-height: calc(100vh - 120px);
  padding: 1.5rem 2rem;
  color: var(--color-heading);
}

:global([data-theme="dark"] .quiz-editor-page) {
  /* background: #000; */
  background: transparent;
}

.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 200px);
}

/* 상단 바 */
.top-bar {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}

/* 문제 카드 리스트 */
.question-list-wrapper {
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: 24px;
  padding: 1.5rem;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
}

:global([data-theme="dark"] .question-list-wrapper) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
}

/* 문제 카드 그리드 */
.question-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 1rem;
}

@media (max-width: 768px) {
  .question-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .question-list {
    grid-template-columns: 1fr;
  }
}

.question-card {
  background: var(--color-background-soft);
  border-radius: 18px;
  border: 1px solid var(--color-border);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  cursor: pointer;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  color: var(--color-heading);
  transition: transform 0.15s ease, box-shadow 0.15s ease, border-color 0.15s ease, background-color 0.15s ease;
  padding: 0.6rem 0.6rem 0.3rem;
}

:global([data-theme="dark"] .question-card) {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: none;
}

.question-card:hover {
  transform: translateY(-3px);
  border-color: var(--color-border-hover);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.18);
  background: var(--color-background-mute);
}

:global([data-theme="dark"] .question-card:hover) {
  background: var(--bg-surface-hover);
  box-shadow: var(--shadow-lg);
}

.card-image-wrapper {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  border-radius: 14px;
  background: var(--color-background-mute);
  border: 1px dashed var(--color-border);
}

:global([data-theme="dark"] .card-image-wrapper) {
  background: var(--bg-main);
  border-color: var(--border);
}

.card-image-wrapper img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-image {
  width: 100%;
  height: 100%;
  font-size: 0.85rem;
  color: var(--color-text);
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-background-mute);
}

:global([data-theme="dark"] .no-image) {
  color: #d1d5db;
}

.no-image.large {
  height: 220px;
  flex-direction: column;
}

.badge-warning {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 20px;
  height: 20px;
  background: #ff4d4f;
  color: #fff;
  border-radius: 50%;
  font-size: 0.8rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.card-footer {
  padding: 0.5rem 0.75rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 0.85rem;
  font-weight: 600;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-heading);
}

.add-card {
  background: var(--color-background-soft);
  border: 2px dashed var(--color-border);
  color: var(--color-text);
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.add-card-inner {
  width: 100%;
  aspect-ratio: 1 / 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.plus-icon {
  font-size: 3rem;
  font-weight: 300;
}

.empty-helper {
  margin-top: 1rem;
  font-size: 0.9rem;
}

.hidden-file-input {
  display: none;
}

/* 메타 모달 */
.meta-form .field {
  margin-bottom: 1rem;
}

.field-label {
  display: block;
  margin-bottom: 0.25rem;
  font-weight: 600;
}

.thumbnail-box {
  border: 2px dashed var(--primary);
  border-radius: 16px;
  padding: 0.75rem;
  cursor: pointer;
  min-height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(39, 181, 207, 0.05); /* var(--primary) with opacity */
}

.thumbnail-box img {
  max-width: 100%;
  max-height: 160px;
  border-radius: 12px;
}

.thumbnail-box.drag-over {
  background: rgba(39, 181, 207, 0.12);
  border-color: var(--primary-hover);
}

.thumbnail-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--primary);
}

.ai-thumbnail-actions {
  margin-top: 0.75rem;
  display: flex;
  flex-direction: column;
  gap: 0.4rem;
}

.ai-helper {
  font-size: 0.8rem;
}

.ai-candidate-panel {
  margin-top: 0.9rem;
  padding: 0.75rem;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  background: var(--color-background-mute);
}

.ai-candidate-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0.6rem;
}

.ai-candidate-card {
  position: relative;
  border: 2px solid transparent;
  border-radius: 12px;
  overflow: hidden;
  background: #111;
  cursor: pointer;
  padding: 0;
  transition: border-color 0.15s ease, transform 0.15s ease;
}

.ai-candidate-card img {
  width: 100%;
  height: 120px;
  object-fit: cover;
  display: block;
}

.ai-candidate-card:hover {
  transform: translateY(-2px);
  border-color: var(--color-border-hover);
}

.ai-candidate-card.selected {
  border-color: var(--primary);
}

.ai-selected-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: var(--primary);
  color: #fff;
  font-size: 0.7rem;
  padding: 2px 6px;
  border-radius: 999px;
}

.ai-candidate-footer {
  margin-top: 0.75rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

:deep(.ai-generate-button.p-button) {
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

:global([data-theme="dark"] :deep(.ai-generate-button.p-button:hover)) {
  background: rgba(39, 181, 207, 0.18) !important;
  border-color: rgba(39, 181, 207, 0.5) !important;
  color: #aeddeb !important;
}

:global([data-theme="dark"] :deep(.ai-generate-button.p-button:enabled:focus-visible)) {
  box-shadow: 0 0 0 2px rgba(39, 181, 207, 0.35) !important;
}

/* 문제 편집 모달 */
.question-image-preview {
  border: 2px dashed transparent;
  border-radius: 12px;
  padding: 0.5rem;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.question-image-preview.drag-over {
  border-color: var(--color-border-hover);
  background: var(--color-background-mute);
}

.question-image-preview img {
  width: 100%;
  max-height: 260px;
  object-fit: contain;
}

.question-card.drag-over {
  border-color: var(--color-border-hover);
  background: var(--color-background-mute);
  box-shadow: 0 12px 26px rgba(0, 0, 0, 0.15);
}

/* 정답 입력칩 - 다크 배경 가독성 */
:deep(.answer-chips) {
  background: var(--color-background-mute);
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 0.35rem 0.5rem;
}

:deep(.answer-chips.p-focus) {
  border-color: var(--color-border-hover);
  box-shadow: 0 0 0 1px var(--color-border-hover);
}

:deep(.answer-chips .p-chip-list),
:deep(.answer-chips .p-inputchips-input) {
  background: transparent !important;
  border: 0;
  padding: 0;
}

:deep(.answer-chips .p-chip) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid var(--color-border);
  color: var(--color-heading);
}

:deep(.answer-chips .p-chip-label) {
  color: inherit;
}

:deep(.answer-chips .p-chip-input) {
  color: var(--color-heading);
}

:deep(.answer-chips .p-chip-input::placeholder) {
  color: var(--color-text);
  opacity: 0.8;
}

/* 스크린샷 기반의 새 구조에 대한 명시적 폴백 */
:global([data-theme="dark"] .p-inputchips-input) {
  background-color: transparent !important;
  color: var(--color-heading) !important;
}

/* PrimeVue 다이얼로그 다크 모드 수정 */
:global([data-theme="dark"] .p-dialog) {
  background: var(--bg-surface) !important;
  color: var(--text-main) !important;
  border: 1px solid var(--border) !important;
}

:global([data-theme="dark"] .p-dialog-header),
:global([data-theme="dark"] .p-dialog-content),
:global([data-theme="dark"] .p-dialog-footer) {
  background: transparent !important;
  color: var(--text-main) !important;
}

:global([data-theme="dark"] .p-dialog .p-dialog-header-icon) {
  color: var(--text-sub) !important;
}

:global([data-theme="dark"] .p-dialog .p-dialog-header-icon:hover) {
  background: var(--bg-surface-hover) !important;
  color: var(--text-main) !important;
}
</style>
